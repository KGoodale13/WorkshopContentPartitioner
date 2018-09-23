/**
  * Applications main entry point
  */

import java.io.File

import gma.{Description, GMA}
import gmpublish.GMPublish
import org.apache.commons.io.FileUtils
import persistence.messages.{ManifestAddonEntry, ManifestFileEntry, WorkshopManifest}
import util.FileUtil
import com.typesafe.config.ConfigFactory

import scala.annotation.tailrec

object WorkshopContentPartitioner extends App {

  val addonTextDescription = ConfigFactory.load().getString("workshop.description")
  val addonTitlePrefix = ConfigFactory.load().getString("workshop.title_prefix")

  val manifestFile = new File(s"${FileUtil.ASSET_FOLDER.getAbsolutePath}/.manifest")

  val originalManifest = {
    if (manifestFile.exists())
      FileUtil.parseManifest(manifestFile)
    else
      WorkshopManifest(Nil)
  }

  val originalFiles: Map[String, ManifestFileEntry] =
    originalManifest.addons.flatMap(_.files.map(entry => entry.path -> entry)).toMap

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
  private def hasFileChanged(file: File): Boolean = {
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

  private def generateFileEntry(file: File): ManifestFileEntry = {
    val path = FileUtil.relativizeToAssetPath(file)
    val crc = FileUtils.checksumCRC32(file)
    ManifestFileEntry(path, file.lastModified(), file.length().toInt, crc.toInt)
  }

  private def createGMA(partitionNumber: Int, files: Seq[File]) = {
    val title = s"$addonTitlePrefix $partitionNumber"
    val addonDescription = Description(addonTextDescription)

    GMA.create(title, addonDescription, files)
  }

  // loop over the original manifest, removing any files that were deleted and adding any new files that can fit

  def fillExistingAddons(newFiles: Stream[File]): (Seq[ManifestAddonEntry], Stream[File]) = {
    var homelessFiles = newFiles
    val modifiedManifests = originalManifest.addons.flatMap { addon =>
      val partitionNumber = addon.partitionNumber
      val files =
        // Remove any deleted files from the manifest
        addon.files.filter(f => FileUtil.resolveRelativePath(f.path).exists)
        // Update the file entries of any changed files
          .map(f =>
            if(hasFileChanged(FileUtil.resolveRelativePath(f.path)))
              generateFileEntry(FileUtil.resolveRelativePath(f.path))
            else f
          )

      // Attempt to add any new files to this addon if there's room
      val currentAddonSize = files.map(_.length).sum
      val (remainingFiles, filesAdded) = takeFilesUntilSizeReached(homelessFiles, currentSize = currentAddonSize)
      homelessFiles = remainingFiles

      val newManifestFileEntries = files ++ filesAdded.map(generateFileEntry)
      val filesInAddon = newManifestFileEntries.map(f => FileUtil.resolveRelativePath(f.path))
      for {
        gmaFile <- createGMA(partitionNumber, filesInAddon)
      } yield {
        if(GMPublish.updateExistingAddon(addon.workshopId, gmaFile)) {
          println(s"Updated content pack $partitionNumber")
          addon.copy(files = newManifestFileEntries)
        } else {
          println(s"Failed to update content pack $partitionNumber")
          addon
        }
      }
    }
    (modifiedManifests, homelessFiles)
  }

  def createNewContentPacksForFiles(homelessFiles: Stream[File]): Seq[ManifestAddonEntry] = {
    var partitionNumber = originalManifest.addons.length + 1
    partitionFiles(homelessFiles).flatMap { partition =>
      for {
        newGMA <- createGMA(partitionNumber, partition)
        addonId <- GMPublish.createNewAddon(newGMA)
      } yield {
        partitionNumber += 1
        new ManifestAddonEntry(
          files = partition.map(generateFileEntry),
          workshopId = addonId
        )
      }
    }
  }


  val newFiles = fileTreeStream.filter(f => !originalFiles.contains(FileUtil.relativizeToAssetPath(f)))

  val manifest = {
    val (updatedManifest: Seq[ManifestAddonEntry], homelessFiles: Stream[File]) = fillExistingAddons(newFiles)
    val newAddonManifests = createNewContentPacksForFiles(homelessFiles)
    WorkshopManifest(updatedManifest ++ newAddonManifests)
  }

  FileUtil.writeManifest(manifestFile, manifest)
}
