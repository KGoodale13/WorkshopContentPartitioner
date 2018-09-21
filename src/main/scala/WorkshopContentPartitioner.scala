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
    f.isDirectory && !f.isHidden && !f.getName.startsWith(".") && (f.getParentFile != FileUtil.ASSET_FOLDER || FileUtil.FOLDER_WHITELIST.contains(f.getName))

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


  val newFiles = fileTreeStream.filter(f => !originalFiles.contains(FileUtil.relativizeToAssetPath(f)))
  val removedFiles = originalFiles.keys.filter(f => !new File(s"${FileUtil.ASSETS_PATH}/$f").exists)
  val updatedFiles = fileTreeStream.collect {
    case f if hasFileChanged(f) => f
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


  // Create manifest if one doesn't exist. TODO: Handle cases where the manifest does exist. Both here and in the partitioner
  val manifest = partitionFiles(fileTreeStream).map { partition =>
    val fileEntries = partition.map(generateFileEntry)
    ManifestAddonEntry(fileEntries)
  }.toList


  val addonDescription = Description(addonTextDescription)

  var partitionNumber = 0
  val updatedManifest = WorkshopManifest(manifest.map { manifestEntry =>
    partitionNumber += 1
    val title = s"$addonTitlePrefix $partitionNumber"
    println(s"Creating addon '$title'")

    val createdGMA = GMA.create(title, addonDescription, manifestEntry.files.map(f => FileUtil.resolveRelativePath(f.path)))

    if (createdGMA.isDefined) {
      println(s"Publishing Addon '$title'")
      val addonId = GMPublish.createNewAddon(createdGMA.head)
      manifestEntry.copy(workshopId = addonId)
    }
    else {
      println(s"Error: Failed to create GMA for Addon '$title'")
      manifestEntry // Leave the manifest entry unchanged
    }
  })

  FileUtil.writeManifest(manifestFile, updatedManifest)
}
