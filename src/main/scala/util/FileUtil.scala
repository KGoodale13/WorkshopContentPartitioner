package util

import java.io.{File, FileInputStream, FileOutputStream}
import java.util.zip.{ZipEntry, ZipInputStream, ZipOutputStream}

import cats.effect.IO
import com.roundeights.hasher.Implicits._
import com.typesafe.config.ConfigFactory
import persistence.messages.WorkshopManifest
import util.IOUtil.runWithClosable

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

  def CRC32File(file: File): IO[Array[Byte]] = {
    runWithClosable(new FileInputStream(file)) { stream =>
      IO(stream.crc32.bytes)
    }
  }

	def CRC32SignFile(file: File): IO[Unit] = {
    CRC32File(file).flatMap { crc =>
      runWithClosable(new FileOutputStream(file, true)) { stream =>
        IO(stream.write(crc.reverse.padTo(4, 0.toByte)))
      }
    }
	}

	def strToNullTerminatedByteArray(string: String) = (string + "\0").getBytes

	def paddedEndianInt(number: Long, padSize: Int) = BigInt(number).toByteArray.reverse.padTo(padSize, 0.toByte)

  def parseManifest(file: File): IO[WorkshopManifest] = {
    runWithClosable(new ZipInputStream(new FileInputStream(file))) { stream =>
      IO {
        stream.getNextEntry
        WorkshopManifest.parseFrom(stream)
      }
    }
  }

  def writeManifest(file: File, manifest: WorkshopManifest): IO[Unit] = {
    runWithClosable(new ZipOutputStream(new FileOutputStream(file))) { stream =>
      IO {
        stream.putNextEntry(new ZipEntry("manifest"))
        manifest.writeTo(stream)
      }
    }
  }
}
