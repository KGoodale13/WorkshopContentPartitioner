package gma

import java.io._

import com.roundeights.hasher.Implicits._
import util.FileUtil

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
			// The Header
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
		outputBuffer.write(0)
		// Timestamp
		outputBuffer.write(BigInt(System.currentTimeMillis / 1000).toByteArray)
		// Required content (unused)
		outputBuffer.write(0.toChar)
		// Addon Title
		outputBuffer.write(title.getBytes)
		// Addon description as json
		outputBuffer.write(description.toString.getBytes)
		// Addon Author [unused]
		outputBuffer.write("Author Name".getBytes)
		// Addon Version [unused]
		outputBuffer.write(1)
	}

	// Writes our file list and other associated metadata to the gma file
	private def writeFileList(files: Seq[File])(implicit outputBuffer: BufferedOutputStream) = {
		// Write our file list
		var fileNum = 0
		files.foreach { file =>
			fileNum += 1
			val crc = new FileInputStream(file).crc32
			val fileSize = file.length()
			outputBuffer.write(fileNum)
			outputBuffer.write(FileUtil.relativizeToAssetPath(file).toLowerCase.getBytes)
			outputBuffer.write(BigInt(fileSize).toByteArray)
			outputBuffer.write(crc.bytes)
		}
		// Zero to signify end of file list
		outputBuffer.write(0)
	}

	// Writes the actual content of each file to the gma file
	private def writeFileContents(files: Seq[File])(implicit outputBuffer: BufferedOutputStream) = {
		files.foreach { file =>
			val fileInputStream = new BufferedInputStream(new FileInputStream(file))
			try
				Stream.continually(fileInputStream.read).takeWhile(_ != -1).foreach(outputBuffer.write)
			finally
				fileInputStream.close()
		}
	}

}
