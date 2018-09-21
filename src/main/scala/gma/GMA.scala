package gma

import java.io._
import java.nio.file.Files

import com.roundeights.hasher.Implicits._
import util.FileUtil

import scala.util.Try

object GMA {

	/**
		*
		* @param title - The title to give the addon on the workshop
		* @param description - Description data about the addon
		* @param files - The files to include in this addon
		* @return Option[File] - If successful returns a reference to the completed GMA file.
		*/
	def create(title: String, description: Description, files: Seq[File]): Option[File] = {

		// Check for files that don't match the whitelisted patterns
		val invalidFileOpt= files.collectFirst { case file if !pathIsWhiteListed(file.getPath) => file }

		if(invalidFileOpt.isDefined){
			println(s"[Error] File: ${invalidFileOpt.head.getPath} is not allowed on the workshop")
			return None
		} else if(files.isEmpty){
			println(s"[Error] file list is empty. Unable to create GMA.")
			return None
		}

		val newGMA = new File(s"${title.replace(' ', '-')}.gma")

		implicit val gmaBufferedWriter: BufferedOutputStream = new BufferedOutputStream(new FileOutputStream(newGMA))

		try {
			writeHeader(title, description)
			writeFileList(files)
			writeFileContents(files)
		} finally
			gmaBufferedWriter.close()

		// Sign the end of the file with a crc32 of the completed gma
		FileUtil.CRC32SignFile(newGMA)
		Some(newGMA)
	}


	// Writes all the headers and other metadata to the start of the GMA file
	private def writeHeader(title: String, description: Description)(implicit outputBuffer: BufferedOutputStream) = {
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
	private def writeFileList(files: Seq[File])(implicit outputBuffer: BufferedOutputStream) = {
		// Write our file list
		var fileNum = 0
		files.foreach { file =>
			fileNum += 1
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
	private def writeFileContents(files: Seq[File])(implicit outputBuffer: BufferedOutputStream) = {
		files.foreach { file =>
			Files.copy(file.toPath, outputBuffer)
		}
	}

}
