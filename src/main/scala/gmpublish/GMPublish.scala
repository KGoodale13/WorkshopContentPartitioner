package gmpublish

import java.io.File

import scala.language.postfixOps
import sys.process._
import scala.util.matching.Regex

object GMPublish {

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

	def updateExistingAddon(addonId: Int, addonGMA: File): Boolean = {
		val result: String = Seq(GM_PUBLISH_LOCATION, "update", "-addon", addonGMA.getAbsolutePath, "-id", addonId.toString) !!

		// Probably not the best way to determine success or failure but works.
		val success = result.contains("Success!")
		if(!success)
			println(s"Error updating. Got result: $result")
		success
	}

}
