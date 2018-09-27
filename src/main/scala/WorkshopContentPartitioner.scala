/**
  * Applications main entry point
  */

import java.io.File

import cats.effect.IO
import gma.{Description, GMA, isFileIgnored, pathIsWhiteListed}
import gmpublish.GMPublish
import org.apache.commons.io.FileUtils
import persistence.messages.{ManifestAddonEntry, ManifestFileEntry, WorkshopManifest}
import util.{FileUtil, IOUtil}
import com.typesafe.config.ConfigFactory
import cats.implicits._

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

object WorkshopContentPartitioner extends App {

  val addonTextDescription = ConfigFactory.load().getString("workshop.description")
  val addonTitlePrefix = ConfigFactory.load().getString("workshop.title_prefix")

  val manifestFile = new File(s"${FileUtil.ASSET_FOLDER.getAbsolutePath}/.manifest")

  val originalManifestIO = {
    if (manifestFile.exists())
      FileUtil.parseManifest(manifestFile)
    else
      IO.pure(WorkshopManifest(Nil))
  }

  private def isValidFolder(f: File): Boolean =
    f.isDirectory &&
      !f.isHidden &&
      !f.getName.startsWith(".") &&
      (f.getParentFile != FileUtil.ASSET_FOLDER || FileUtil.FOLDER_WHITELIST.contains(f.getName))

  // Source: https://stackoverflow.com/a/7264833/5404965
  def getFileTreeStream(f: File): Stream[File] =
    f #:: (if (isValidFolder(f)) f.listFiles().toStream.flatMap(getFileTreeStream) else Stream.empty)

  val fileTreeStream = getFileTreeStream(FileUtil.ASSET_FOLDER).filterNot(file => file.isHidden || file.isDirectory || file.getName.startsWith(".") || file.getParentFile() == FileUtil.ASSET_FOLDER)

  /**
    * @return true iff the file has already been tracked in the manifest but the content changed
    */
  private def hasFileChanged(file: File, originalFiles: Map[String, ManifestFileEntry]): Boolean = {
    val relativePath = FileUtil.relativizeToAssetPath(file)
    originalFiles.get(relativePath).exists(entry =>
      file.length() != entry.length ||
        (entry.lastModified != file.lastModified && entry.crc != FileUtils.checksumCRC32(file))
    )
  }

  @tailrec
  def takeFilesUntilSizeReached(
     inputList: List[File],
     currentFiles: List[File] = Nil,
     currentSize: Long = 0
   ): (List[File], List[File]) = inputList match {
    case Nil => (inputList, currentFiles)
    case file :: xss if currentSize + file.length() <= FileUtil.PARTITION_SIZE =>
      takeFilesUntilSizeReached(xss, file :: currentFiles, currentSize + file.length())
    case _ => (inputList, currentFiles)
  }

  def partitionFiles(inputStream: List[File]): List[Seq[File]] = inputStream match {
    case Nil => List.empty[Seq[File]]
    case xss =>
      val (newList, filesInPartition) = takeFilesUntilSizeReached(xss)
      filesInPartition :: partitionFiles(newList)
  }

  private def generateFileEntry(file: File): IO[ManifestFileEntry] = IO {
    val path = FileUtil.relativizeToAssetPath(file)
    val crc = FileUtils.checksumCRC32(file)
    ManifestFileEntry(path, file.lastModified(), file.length().toInt, crc.toInt)
  }

  private def createGMA(partitionNumber: Int, files: Seq[File]): IO[File] = {
    val title = s"$addonTitlePrefix $partitionNumber"
    val addonDescription = Description(addonTextDescription)

    GMA.create(title, addonDescription, files)
  }

  // loop over the original manifest, removing any files that were deleted and adding any new files that can fit

  def fillExistingAddons(
    newFiles: List[File],
    originalManifest: WorkshopManifest,
    originalFiles: Map[String, ManifestFileEntry]
  ): IO[(List[ManifestAddonEntry], List[File])] = {
    originalManifest.addons.toList.foldLeft(IO(List[ManifestAddonEntry](), newFiles)) {
      case (current, addon) =>
        current.flatMap { case (addonEntries, remainingFiles) =>
          // Remove any deleted files from the manifest and update any changed files
          val cleanedManifest: IO[List[ManifestFileEntry]] =
            addon.files.toList.filter(f => FileUtil.resolveRelativePath(f.path).exists).map{ f =>
              if (hasFileChanged(FileUtil.resolveRelativePath(f.path), originalFiles))
                generateFileEntry(FileUtil.resolveRelativePath(f.path))
              else
                IO.pure(f)
            }.sequence

          val newManifestFileEntries = cleanedManifest.flatMap { fileEntries: Seq[ManifestFileEntry] =>
            val files = fileEntries.map(f => FileUtil.resolveRelativePath(f.path))
            val currentAddonSize = files.map(_.length).sum
            // Attempt to add any new files to this addon if there's room
            val (newRemainingFiles, filesAdded) = takeFilesUntilSizeReached(remainingFiles, currentSize = currentAddonSize)
            val addedEntries: IO[List[ManifestFileEntry]] = filesAdded.map(generateFileEntry).sequence
            addedEntries.map(entries => newRemainingFiles -> (entries ++ fileEntries))
          }

          for {
            manifestResult <- newManifestFileEntries
            (newRemainingFiles, newManifestFileEntries) = manifestResult
          } yield {
            (addon.copy(files = newManifestFileEntries) :: addonEntries) -> newRemainingFiles
          }
        }
    }
  }

  // Partition the rest of our files into new packs
  def createNewContentPacksForFiles(homelessFiles: List[File], originalManifest: WorkshopManifest): IO[List[ManifestAddonEntry]] = {
    val numberStream = IOUtil.intStream(originalManifest.addons.length + 1)
    partitionFiles(homelessFiles).zip(numberStream).map { case (partition, partitionNumber) =>
      for {
        fileEntries <- partition.toList.map(generateFileEntry).sequence[IO, ManifestFileEntry]
      } yield {
        ManifestAddonEntry(
          files = fileEntries,
          partitionNumber = partitionNumber
        )
      }
    }.sequence
  }

  // Publishes or updates the addon depending on if it has a workshop id set already or not. This will set the workshop id in the manifest in the case that its a create operation
  def updateOrCreateWorkshopAddon(gmaFile: File, manifestAddonEntry: ManifestAddonEntry): IO[ManifestAddonEntry] = {
    if(manifestAddonEntry.workshopId != 0)
      GMPublish.updateExistingAddon(manifestAddonEntry.workshopId, gmaFile).map(_ => manifestAddonEntry)
    else
      GMPublish.createNewAddon(gmaFile).map(newId => manifestAddonEntry.copy(workshopId = newId))
  }

  // Create all the gma's and upload them
  def publishWorkshopAddons(originalManifest: WorkshopManifest, updatedManifest: List[ManifestAddonEntry]): IO[Unit] = {
    val originalManifestMap = originalManifest.addons.groupBy(_.partitionNumber).mapValues(_.head)
    updatedManifest.foldLeft(IO(originalManifestMap)) { (committedManifestChanges, updatedManifestEntry) =>

      val originalManifestEntry = originalManifestMap.get(updatedManifestEntry.partitionNumber)

      if(originalManifestEntry.contains(updatedManifestEntry)){ // No changes to this entry so we can continue on
        committedManifestChanges
      } else {
        for (
          gmaFile <- createGMA(updatedManifestEntry.partitionNumber, updatedManifestEntry.files.map(f => FileUtil.resolveRelativePath(f.path)));
          resultingManifestEntry <- updateOrCreateWorkshopAddon(gmaFile, updatedManifestEntry);
          newManifest <- committedManifestChanges.map(_ + (resultingManifestEntry.workshopId -> resultingManifestEntry));
          _ <- FileUtil.writeManifest(manifestFile, WorkshopManifest(newManifest.values.toList))
        ) yield newManifest
      }
    }.map(_ => ())
  }

  // Checks the new files to make sure they match the whitelisted patterns and filters out any ignored files
  def filterNewFiles(newFiles: Stream[File]): List[File] = {
    newFiles
    .filterNot(file => isFileIgnored(FileUtil.relativizeToAssetPath(file).toLowerCase))
     .foldLeft(List.empty[File]) { (validFiles, file) =>
       val relativeFilePath = FileUtil.relativizeToAssetPath(file).toLowerCase
       // Filter out blacklisted files or throw a fatal error if they shouldn't be ignored
       if(!pathIsWhiteListed(relativeFilePath))
         println(s"Warning file: $relativeFilePath is not allowed. Ignoring. This file will not be included in the workshop addons.")
       file :: validFiles
     }
  }

  val ioAction =
    for (originalManifest <- originalManifestIO;
         originalFiles = originalManifest.addons.flatMap(_.files.map(entry => entry.path -> entry)).toMap;
         newFiles = filterNewFiles(fileTreeStream.filter(f => !originalFiles.contains(FileUtil.relativizeToAssetPath(f))));
         addonResult <- fillExistingAddons(newFiles, originalManifest, originalFiles);
         (updatedManifest, homelessFiles) = addonResult;
         newAddonManifests <- createNewContentPacksForFiles(homelessFiles, originalManifest);
         _ <- publishWorkshopAddons(originalManifest, updatedManifest ++ newAddonManifests)
    ) yield {()}

  Try {
    ioAction.unsafeRunSync()
  } match {
    case Success(_) => println("Partitioned successfully")
    case Failure(ex) => println(ex.getMessage)
  }
}
