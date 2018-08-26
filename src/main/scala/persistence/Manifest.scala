package persistence

import java.io.File
import java.io._

import play.api.libs.json._

case class ManifestEntry(files: Seq[String], workshopId: Option[Int] = None)

object Manifest {
	implicit val manifestEntryFormat = Json.format[ManifestEntry]

	/**
		* Attempts to parse the passed string as a file manifest
		* @param manifest - The JSON string that contains our manifest data
		* @return The a map representing the manifest or None
		*/
	def parseManifestFile(manifest: String): Option[List[ManifestEntry]] =
		Json.parse(manifest).validate[List[ManifestEntry]].asOpt

	/**
		* Saves the passed manifest data to the file specified
		* @param file - A reference to the file where we should save the manifest
		* @param manifest - The manifest data to save to the file
		* @return true if we were able to write to the file. False if we were unable to
		*/
	def saveManifestToFile(file: File, manifest: List[ManifestEntry]): Boolean = {
		val printWriter = new PrintWriter(file)
		val manifestJSON = Json.toJson[List[ManifestEntry]](manifest)
		printWriter.write(manifestJSON.toString)
		printWriter.close()
		printWriter.checkError()
	}

}
