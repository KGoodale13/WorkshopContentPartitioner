package util

import java.io.{File, FileInputStream, FileOutputStream}

import com.roundeights.hasher.Implicits._

object FileUtil {

	// The path of assets we will be partitioning into workshop addons
	val ASSETS_PATH = sys.env("ASSETS_PATH")
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

}
