package gma

import java.io._
import java.nio.file.Files

import cats.effect.IO
import com.roundeights.hasher.Implicits._
import com.typesafe.config.ConfigFactory
import util.{FileUtil, IOUtil}
import util.IOUtil.runWithClosable

object GMA {

	/**
		*
		* @param title - The title to give the addon on the workshop
		* @param description - Description data about the addon
		* @param files - The files to include in this addon
		* @return Option[File] - If successful returns a reference to the completed GMA file.
		*/
	def create(title: String, description: Description, files: Seq[File]): IO[File] = {
		// Check for files that don't match the whitelisted patterns
    val filteredFiles = files.filter { file => !isFileIgnored(FileUtil.relativizeToAssetPath(file).toLowerCase)}
		val invalidFileOpt= filteredFiles.collectFirst {
			case file if !pathIsWhiteListed(FileUtil.relativizeToAssetPath(file).toLowerCase) => file
		}

		if(invalidFileOpt.isDefined) {
			IO.raiseError(new Exception(s"[Error] File: ${invalidFileOpt.head.getPath} is not allowed on the workshop"))
		} else if(filteredFiles.isEmpty){
			IO.raiseError(new Exception(s"[Error] file list is empty. Unable to create GMA."))
		} else {
			val newGMA = new File(s"${title.replace(' ', '-')}.gma")
      val ioResult = runWithClosable(new BufferedOutputStream(new FileOutputStream(newGMA))) { implicit bos =>
        for (_ <- writeHeader(title, description);
             _ <- writeFileList(filteredFiles);
             _ <- writeFileContents(filteredFiles)) yield {
          ()
        }
      }
      ioResult.flatMap(_ => FileUtil.CRC32SignFile(newGMA))
        .map(_ => newGMA)
		}
	}


	// Writes all the headers and other metadata to the start of the GMA file
	private def writeHeader(title: String, description: Description)
												 (implicit outputBuffer: BufferedOutputStream): IO[Unit] = IO {
		outputBuffer.write(ADDON_IDENT.getBytes)
		outputBuffer.write(ADDON_VERSION.toChar)
		// SteamId (unused)
		outputBuffer.write(FileUtil.paddedEndianInt(0, 8))
		// Timestamp
		outputBuffer.write(FileUtil.paddedEndianInt(System.currentTimeMillis / 1000, 8))
		// Required content (unused)
		outputBuffer.write(0.toChar)
		// Addon Title
		outputBuffer.write(FileUtil.strToNullTerminatedByteArray(title))
		// Addon description as json
		outputBuffer.write(FileUtil.strToNullTerminatedByteArray(description.toString))
		// Addon Author [unused]
		outputBuffer.write(FileUtil.strToNullTerminatedByteArray("Author Name"))
		// Addon Version [unused]
		outputBuffer.write(FileUtil.paddedEndianInt(1, 4))
	}

	// Writes our file list and other associated metadata to the gma file
	private def writeFileList(files: Seq[File])(implicit outputBuffer: BufferedOutputStream): IO[Unit] = IO {
		// Write our file list
		val stream = IOUtil.intStream(1)
		files.zip(stream).foreach { case (file, fileNum) =>
			val crc = new FileInputStream(file).crc32
			val fileSize = file.length()
			outputBuffer.write(FileUtil.paddedEndianInt(fileNum, 4))
			outputBuffer.write(FileUtil.strToNullTerminatedByteArray(FileUtil.relativizeToAssetPath(file).toLowerCase))
			outputBuffer.write(FileUtil.paddedEndianInt(fileSize, 8))
			outputBuffer.write(crc.bytes.reverse.padTo(4, 0.toByte))
		}
		// Zero to signify end of file list
		outputBuffer.write(FileUtil.paddedEndianInt(0, 4))
	}

	// Writes the actual content of each file to the gma file
	private def writeFileContents(files: Seq[File])(implicit outputBuffer: BufferedOutputStream): IO[Unit] = IO {
		files.foreach { file =>
			Files.copy(file.toPath, outputBuffer)
		}
	}

}
