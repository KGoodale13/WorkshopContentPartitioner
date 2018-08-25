/**
  * Applications main entry point
  */

import java.io.File

import persistence.ManifestEntry
import persistence.Manifest

import scala.annotation.tailrec
import scala.collection.JavaConverters._

object Main extends App {

  // The path of assets we will be partitioning into workshop addons
  val ASSETS_PATH = sys.env("ASSETS_PATH")
  val assetFolder = new File(ASSETS_PATH)

  def relativizeToAssetPath(otherFile: File) = assetFolder.getAbsoluteFile.toURI.relativize(otherFile.getAbsoluteFile.toURI)

  val manifestFile = new File(s"${assetFolder.getAbsolutePath}/.manifest")

  val PARTITION_SIZE = 200000000L

  val FOLDER_WHITELIST = Set(
    "maps",
    "backgrounds",
    "gamemodes",
    "materials",
    "lua",
    "scenes",
    "models",
    "scripts",
    "particles",
    "sound",
    "resource"
  )

  val originalManifest = {
    if (manifestFile.exists())
      Manifest.parseManifestFile(scala.io.Source.fromFile(manifestFile).mkString).getOrElse(List[ManifestEntry]())
    else
      List[ManifestEntry]()
  }

  val originalFileList = originalManifest.flatMap(_.files).toSet

  // Source: https://stackoverflow.com/a/7264833/5404965
  def getFileTreeStream(f: File): Stream[File] =
    f #:: (if (f.isDirectory && (f.getParentFile != assetFolder || FOLDER_WHITELIST.contains(f.getName))) f.listFiles().toStream.flatMap(getFileTreeStream) else Stream.empty)

  val fileTreeStream = getFileTreeStream(assetFolder)

  val newFiles = fileTreeStream.filter(f => !originalFileList.contains(relativizeToAssetPath(f).getPath))
  val removedFiles = originalFileList.filter(f => !new File(s"$ASSETS_PATH/$f").exists)

  println(removedFiles)


  @tailrec
  def takeFilesUntilSizeReached(
		inputStream: Stream[File],
		currentFiles: List[File] = Nil,
		currentSize: Long = 0
	): (Stream[File], List[File]) = inputStream match {
    case Stream.Empty => (inputStream, currentFiles)
    case file #:: xss if currentSize + file.length() <= PARTITION_SIZE =>
      takeFilesUntilSizeReached(xss, file :: currentFiles, currentSize + file.length())
    case _ => (inputStream, currentFiles)
  }


  def partitionFiles(inputStream: Stream[File]): Stream[Seq[File]] = inputStream match {
    case Stream.Empty => Stream.empty
    case xss =>
      val (newStream, filesInPartition) = takeFilesUntilSizeReached(xss)
      filesInPartition #:: partitionFiles(newStream)
  }

  val manifest = partitionFiles(fileTreeStream).map { partition =>
    ManifestEntry(partition.map(relativizeToAssetPath(_).getPath))
  }.toList

  Manifest.saveManifestToFile(manifestFile, manifest)

}
