/**
  * Applications main entry point
  */

import java.io.File

import cats.effect.IO
import gma.{Description, GMA}
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

  val fileTreeStream = getFileTreeStream(FileUtil.ASSET_FOLDER).filterNot(file => file.isHidden || file.isDirectory || file.getName.startsWith("."))

  /**
    * @return true iff the file has already been tracked in the manifest but the content changed
    */
  private def hasFileChanged(file: File, originalFiles: Map[String, ManifestFileEntry]): IO[Boolean] = IO {
    val relativePath = FileUtil.relativizeToAssetPath(file)
    originalFiles.get(relativePath).exists(entry =>
      file.length() != entry.length ||
        (entry.lastModified != file.lastModified && entry.crc != FileUtils.checksumCRC32(file))
    )
  }

  @tailrec
  def takeFilesUntilSizeReached(
     inputStream: Stream[File],
     currentFiles: List[File] = Nil,
     currentSize: Long = 0
   ): (Stream[File], List[File]) = inputStream match {
    case Stream.Empty => (inputStream, currentFiles)
    case file #:: xss if currentSize + file.length() <= FileUtil.PARTITION_SIZE =>
      takeFilesUntilSizeReached(xss, file :: currentFiles, currentSize + file.length())
    case _ => (inputStream, currentFiles)
  }

  def partitionFiles(inputStream: Stream[File]): Stream[Seq[File]] = inputStream match {
    case Stream.Empty => Stream.empty
    case xss =>
      val (newStream, filesInPartition) = takeFilesUntilSizeReached(xss)
      filesInPartition #:: partitionFiles(newStream)
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

  def fillExistingAddons(newFiles: Stream[File],
                         originalManifest: WorkshopManifest,
                         originalFiles: Map[String, ManifestFileEntry]): IO[(List[ManifestAddonEntry], Stream[File])] = {
    originalManifest.addons.toList.foldLeft(IO(List.empty[ManifestAddonEntry], newFiles)) {
      case (currentIO, addon) =>
        currentIO.flatMap { case (addonEntries, remainingFiles) =>
          val partitionNumber = addon.partitionNumber
          val filesIO: IO[List[ManifestFileEntry]] =
          // Remove any deleted files from the manifest
            addon.files.toList.filter(f => FileUtil.resolveRelativePath(f.path).exists)
              // Update the file entries of any changed files
              .map(f =>
              hasFileChanged(FileUtil.resolveRelativePath(f.path), originalFiles).flatMap { changed =>
                if (changed) {
                  generateFileEntry(FileUtil.resolveRelativePath(f.path))
                } else {
                  IO.pure(f)
                }
              }
            ).sequence
          val newManifestFileEntriesIO = filesIO.flatMap { fileEntries =>
            val files = fileEntries.map(e => FileUtil.resolveRelativePath(e.path))
            val currentAddonSize = files.map(_.length).sum
            // Attempt to add any new files to this addon if there's room
            val (newRemainingFiles, filesAdded) = takeFilesUntilSizeReached(remainingFiles, currentSize = currentAddonSize)
            val addedEntries: IO[List[ManifestFileEntry]] = filesAdded.map(generateFileEntry).sequence
            addedEntries.map(entries => newRemainingFiles -> (entries ++ fileEntries))
          }
          for {
            manifestResult <- newManifestFileEntriesIO
            (newRemainingFiles, newManifestFileEntries) = manifestResult
            filesInAddon = newManifestFileEntries.map { f => FileUtil.resolveRelativePath(f.path) }
            gmaFile <- createGMA(partitionNumber, filesInAddon)
            _ <- GMPublish.updateExistingAddon(addon.workshopId, gmaFile)
          } yield {
            (addon.copy(files = newManifestFileEntries) :: addonEntries) -> newRemainingFiles
          }
        }
    }
  }

  def createNewContentPacksForFiles(homelessFiles: Stream[File], originalManifest: WorkshopManifest): IO[List[ManifestAddonEntry]] = {
    val numberStream = IOUtil.intStream(originalManifest.addons.length + 1)
    partitionFiles(homelessFiles).zip(numberStream).map { case (partition, partitionNumber) =>
      val filesIO: IO[List[ManifestFileEntry]] = partition.toList.map(generateFileEntry).sequence
      for {
        newGMA <- createGMA(partitionNumber, partition)
        addonId <- GMPublish.createNewAddon(newGMA)
        files <- filesIO
      } yield {
        ManifestAddonEntry(
          files = files,
          workshopId = addonId
        )
      }
    }.toList.sequence
  }

  val ioAction =
    for (originalManifest <- originalManifestIO;
         originalFiles = originalManifest.addons.flatMap(_.files.map(entry => entry.path -> entry)).toMap;
         newFiles = fileTreeStream.filter(f => !originalFiles.contains(FileUtil.relativizeToAssetPath(f)));
         addonResult <- fillExistingAddons(newFiles, originalManifest, originalFiles);
         (updatedManifest, homelessFiles) = addonResult;
         newAddonManifests <- createNewContentPacksForFiles(homelessFiles, originalManifest);
         newWorkshopManifest = WorkshopManifest(updatedManifest ++ newAddonManifests);
         _ <- FileUtil.writeManifest(manifestFile, newWorkshopManifest)) yield {
      ()
    }
  Try {
    ioAction.unsafeRunSync()
  } match {
    case Success(_) => println("Partitioned successfully")
    case Failure(ex) => println(ex.getMessage)
  }
}
