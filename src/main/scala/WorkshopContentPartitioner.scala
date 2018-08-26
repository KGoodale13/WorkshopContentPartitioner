/**
  * Applications main entry point
  */

import java.io.File

import gma.{Description, GMA}
import gmpublish.GMPublish
import persistence.ManifestEntry
import persistence.Manifest
import util.FileUtil

import scala.annotation.tailrec

object WorkshopContentPartitioner extends App {

  val addonTextDescription = System.getenv("ADDON_DESCRIPTION")
  val addonTitlePrefix = System.getenv("ADDON_TITLE_PREFIX").trim()

  val manifestFile = new File(s"${FileUtil.ASSET_FOLDER.getAbsolutePath}/.manifest")

  val originalManifest = {
    if (manifestFile.exists())
      Manifest.parseManifestFile(scala.io.Source.fromFile(manifestFile).mkString).getOrElse(List[ManifestEntry]())
    else
      List[ManifestEntry]()
  }

  val originalFileList = originalManifest.flatMap(_.files).toSet

  // Source: https://stackoverflow.com/a/7264833/5404965
  def getFileTreeStream(f: File): Stream[File] =
    f #::
      (if (f.isDirectory && !f.isHidden && (f.getParentFile != FileUtil.ASSET_FOLDER || FileUtil.FOLDER_WHITELIST.contains(f.getName)))
        f.listFiles().toStream.flatMap(getFileTreeStream)
      else Stream.empty).filterNot(file => file.isHidden || file.isDirectory)

  val fileTreeStream = getFileTreeStream(FileUtil.ASSET_FOLDER)

  val newFiles = fileTreeStream.filter(f => !originalFileList.contains(FileUtil.relativizeToAssetPath(f)))
  val removedFiles = originalFileList.filter(f => !new File(s"${FileUtil.ASSETS_PATH}/$f").exists)


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

  // Create manifest if one doesn't exist. TODO: Handle cases where the manifest does exist. Both here and in the partitioner
  val manifest = partitionFiles(fileTreeStream).map { partition =>
    ManifestEntry(partition.map(FileUtil.relativizeToAssetPath))
  }.toList


  val addonDescription = Description(addonTextDescription)

  var partitionNumber = 0
  val updatedManifest = manifest.map { manifestEntry =>
    partitionNumber += 1
    val title = s"$addonTitlePrefix $partitionNumber"
    println(s"Creating addon '$title'")

    val createdGMA = GMA.create(title, addonDescription, manifestEntry.files.map(f => new File(s"${FileUtil.ASSETS_PATH}/$f")))

    if(createdGMA.isDefined) {
      println(s"Publishing Addon '$title'")
      val addonId = GMPublish.createNewAddon(createdGMA.head)
      manifestEntry.copy(workshopId = Some(addonId))
    }
    else {
      println(s"Error: Failed to create GMA for Addon '$title'")
      manifestEntry // Leave the manifest entry unchanged
    }
  }

  Manifest.saveManifestToFile(manifestFile, updatedManifest)

}
