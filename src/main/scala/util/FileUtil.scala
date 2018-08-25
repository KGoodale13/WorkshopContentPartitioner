package util

import java.io.{File, FileOutputStream}
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
		val crc = scala.io.Source.fromFile(file).crc32.bytes
		val fileOutputStream = new FileOutputStream(file)
		try
			fileOutputStream.write(crc)
		finally
			fileOutputStream.close()
	}

}
