package gmpublish

import java.io.File

import scala.language.postfixOps
import sys.process._
import scala.util.matching.Regex

object GMPublish {

	val ICON_IMAGE = System.getenv("ICON_PATH")

	def getWorkshopAddonLink(id: Int) = s"https://steamcommunity.com/sharedfiles/filedetails/?id=$id"

	val addonListIdPattern = raw"\s\d{9,10}\s".r

	// TODO: Handle error codes - List of error codes here: https://gmod.facepunch.com/f/gmoddev/nyqm/GMPublish-publish-addons-to-workshop/1/

	private def getAddonIdList(): List[Int] = {
		val result = Seq(GM_PUBLISH_LOCATION, "list") !!
		val foundIds = addonListIdPattern.findAllIn(result).toList
		foundIds.map(_.toInt)
	}

	def createNewAddon(addonGMA: File): Int = {
		println(addonGMA.getAbsolutePath)
		val result: String = Seq(GM_PUBLISH_LOCATION, "create", "-icon", ICON_IMAGE, "-addon", addonGMA.getAbsolutePath) !!

		println(s"Addon Create finished with result: $result")

		// Get the id of the created addon. This should be the largest id
		val addonId = getAddonIdList().reduceLeft((a, b) => if(a > b) a else b)
		println(s"Addon Created with ID: $addonId. You need to set the visibility to public on this page: ${getWorkshopAddonLink(addonId)}")
		addonId
	}

	//def updateExistingAddon(addonId: Int, addonGMA: File)

}
