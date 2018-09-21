package util

import java.io.{File, FileInputStream, FileOutputStream}
import java.nio.file.Files
import java.util.zip.{ZipEntry, ZipInputStream, ZipOutputStream}

import com.roundeights.hasher.Implicits._
import com.typesafe.config.ConfigFactory
import persistence.messages.WorkshopManifest

import scala.util.Try


object FileUtil {

	// The path of assets we will be partitioning into workshop addons
	val ASSETS_PATH = ConfigFactory.load().getString("assets_path")
	// The max uncompressed size of each addon
	val PARTITION_SIZE = 210000000L
	// Whitelist of folders to create addons for
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

	val ASSET_FOLDER = new File(ASSETS_PATH)

	def resolveRelativePath(path: String): File =
		new File(s"${FileUtil.ASSETS_PATH}/$path")

	def relativizeToAssetPath(otherFile: File): String = ASSET_FOLDER.getAbsoluteFile.toURI.relativize(otherFile.getAbsoluteFile.toURI).getPath

	def CRC32SignFile(file: File) = {
		val crc = new FileInputStream(file).crc32.bytes
		val fileOutputStream = new FileOutputStream(file, true)
		try
			fileOutputStream.write(crc.reverse.padTo(4, 0.toByte))
		finally
			fileOutputStream.close()
	}

	def strToNullTerminatedByteArray(string: String) = (string + "\0").getBytes

	def paddedEndianInt(number: Long, padSize: Int) = BigInt(number).toByteArray.reverse.padTo(padSize, 0.toByte)

  def parseManifest(file: File): WorkshopManifest = {
    Try {
      val zipStream = new ZipInputStream(new FileInputStream(file))
      zipStream.getNextEntry
      val result = WorkshopManifest.parseFrom(zipStream)
      zipStream.close()
      result
    }.getOrElse(WorkshopManifest())
  }

  def writeManifest(file: File, manifest: WorkshopManifest): Unit = {
    Try {
      val zipStream = new ZipOutputStream(new FileOutputStream(file))
      zipStream.putNextEntry(new ZipEntry("manifest"))
      manifest.writeTo(zipStream)
      zipStream.close()
    }
  }
}
