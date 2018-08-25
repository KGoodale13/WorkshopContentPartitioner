package gma

import play.api.libs.json.Json

object Description {
	implicit val descriptionFormat = Json.format[Description]
}

case class Description(
	description: String,
	`type`: String = "ServerContent",
	tags: List[String] = List("roleplay")
){
	override def toString = Json.toJson[Description](this).toString()
}