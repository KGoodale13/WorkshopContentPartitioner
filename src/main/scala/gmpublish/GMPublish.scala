package gmpublish

import java.io.File

import cats.effect.IO
import util.IOUtil.putStrLn

import scala.language.postfixOps
import sys.process._

object GMPublish {

	def getWorkshopAddonLink(id: Int) = s"https://steamcommunity.com/sharedfiles/filedetails/?id=$id"

	val addonListIdPattern = raw"\s\d{9,10}\s".r

	// TODO: Handle error codes - List of error codes here: https://gmod.facepunch.com/f/gmoddev/nyqm/GMPublish-publish-addons-to-workshop/1/

	private def getLatestAddonId: IO[Int] = {
		val addonList = IO { Seq(GM_PUBLISH_LOCATION, "list") !! }
    addonList.flatMap { result =>
      val foundIds = addonListIdPattern.findAllIn(result).toList.map(_.trim.toInt)
      if (foundIds.nonEmpty) {
        val addonId = foundIds.reduceLeft((a, b) => if(a > b) a else b)
        IO.pure(addonId)
      } else {
        IO.raiseError(new Exception(""))
      }
    }
	}

	def createNewAddon(addonGMA: File): IO[Int] = {
		println(addonGMA.getAbsolutePath)
		val processResult = IO {
			Seq(GM_PUBLISH_LOCATION, "create", "-icon", ICON_IMAGE, "-addon", addonGMA.getAbsolutePath) !!
		}
		for (result <- processResult;
				 _ <- putStrLn(s"Addon Create finished with result: $result");
         addonId <- getLatestAddonId;
         _ <- putStrLn(s"Addon Created with ID: $addonId. You need to set the visibility to public on this page: ${getWorkshopAddonLink(addonId)}")) yield  {
      addonId
    }
	}

	def updateExistingAddon(addonId: Int, addonGMA: File): IO[Unit] = {
		IO {
      Seq(GM_PUBLISH_LOCATION, "update", "-addon", addonGMA.getAbsolutePath, "-id", addonId.toString) !!
    }.flatMap { result =>
      if (result.contains("Success!")) {
        IO.pure(())
      } else {
        IO.raiseError(new Exception(s"Error updating. Got result: $result"))
      }
    }
	}

}
