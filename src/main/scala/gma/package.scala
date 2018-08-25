import scala.util.matching.Regex

package object gma {

	val ADDON_WHITELIST: List[Regex] = List(
		raw"lua/[a-zA-Z\\s]+.lua".r,
		raw"scenes/[a-zA-Z\\s]+.vcd".r,
		raw"particles/[a-zA-Z\\s]+.pcf".r,
		raw"resource/fonts/[a-zA-Z\\s]+.ttf".r,
		raw"scripts/vehicles/[a-zA-Z\\s]+.txt".r,
		raw"resource/localization/[a-zA-Z\\s]+/[a-zA-Z\\s]+.properties".r,
		raw"maps/[a-zA-Z\\s]+.bsp".r,
		raw"maps/[a-zA-Z\\s]+.nav".r,
		raw"maps/[a-zA-Z\\s]+.ain".r,
		raw"maps/thumb/[a-zA-Z\\s]+.png".r,
		raw"sound/[a-zA-Z\\s]+.wav".r,
		raw"sound/[a-zA-Z\\s]+.mp3".r,
		raw"sound/[a-zA-Z\\s]+.ogg".r,
		raw"materials/[a-zA-Z\\s]+.vmt".r,
		raw"materials/[a-zA-Z\\s]+.vtf".r,
		raw"materials/[a-zA-Z\\s]+.png".r,
		raw"materials/[a-zA-Z\\s]+.jpg".r,
		raw"materials/[a-zA-Z\\s]+.jpeg".r,
		raw"models/[a-zA-Z\\s]+.mdl".r,
		raw"models/[a-zA-Z\\s]+.vtx".r,
		raw"models/[a-zA-Z\\s]+.phy".r,
		raw"models/[a-zA-Z\\s]+.ani".r,
		raw"models/[a-zA-Z\\s]+.vvd".r,
		raw"gamemodes/[a-zA-Z\\s]+/[a-zA-Z\\s]+.txt".r,
		raw"gamemodes/[a-zA-Z\\s]+/[a-zA-Z\\s]+.fgd".r,
		raw"gamemodes/[a-zA-Z\\s]+/logo.png".r,
		raw"gamemodes/[a-zA-Z\\s]+/icon24.png".r,
		raw"gamemodes/[a-zA-Z\\s]+/gamemode/[a-zA-Z\\s]+.lua".r,
		raw"gamemodes/[a-zA-Z\\s]+/entities/effects/[a-zA-Z\\s]+.lua".r,
		raw"gamemodes/[a-zA-Z\\s]+/entities/weapons/[a-zA-Z\\s]+.lua".r,
		raw"gamemodes/[a-zA-Z\\s]+/entities/entities/[a-zA-Z\\s]+.lua".r,
		raw"gamemodes/[a-zA-Z\\s]+/backgrounds/[a-zA-Z\\s]+.png".r,
		raw"gamemodes/[a-zA-Z\\s]+/backgrounds/[a-zA-Z\\s]+.jpg".r,
		raw"gamemodes/[a-zA-Z\\s]+/backgrounds/[a-zA-Z\\s]+.jpeg".r,
		raw"gamemodes/[a-zA-Z\\s]+/content/models/[a-zA-Z\\s]+.mdl".r,
		raw"gamemodes/[a-zA-Z\\s]+/content/models/[a-zA-Z\\s]+.vtx".r,
		raw"gamemodes/[a-zA-Z\\s]+/content/models/[a-zA-Z\\s]+.phy".r,
		raw"gamemodes/[a-zA-Z\\s]+/content/models/[a-zA-Z\\s]+.ani".r,
		raw"gamemodes/[a-zA-Z\\s]+/content/models/[a-zA-Z\\s]+.vvd".r,
		raw"gamemodes/[a-zA-Z\\s]+/content/materials/[a-zA-Z\\s]+.vmt".r,
		raw"gamemodes/[a-zA-Z\\s]+/content/materials/[a-zA-Z\\s]+.vtf".r,
		raw"gamemodes/[a-zA-Z\\s]+/content/materials/[a-zA-Z\\s]+.png".r,
		raw"gamemodes/[a-zA-Z\\s]+/content/materials/[a-zA-Z\\s]+.jpg".r,
		raw"gamemodes/[a-zA-Z\\s]+/content/materials/[a-zA-Z\\s]+.jpeg".r,
		raw"gamemodes/[a-zA-Z\\s]+/content/scenes/[a-zA-Z\\s]+.vcd".r,
		raw"gamemodes/[a-zA-Z\\s]+/content/particles/[a-zA-Z\\s]+.pcf".r,
		raw"gamemodes/[a-zA-Z\\s]+/content/resource/fonts/[a-zA-Z\\s]+.ttf".r,
		raw"gamemodes/[a-zA-Z\\s]+/content/scripts/vehicles/[a-zA-Z\\s]+.txt".r,
		raw"gamemodes/[a-zA-Z\\s]+/content/resource/localization/[a-zA-Z\\s]+/[a-zA-Z\\s]+.properties".r,
		raw"gamemodes/[a-zA-Z\\s]+/content/maps/[a-zA-Z\\s]+.bsp".r,
		raw"gamemodes/[a-zA-Z\\s]+/content/maps/[a-zA-Z\\s]+.nav".r,
		raw"gamemodes/[a-zA-Z\\s]+/content/maps/[a-zA-Z\\s]+.ain".r,
		raw"gamemodes/[a-zA-Z\\s]+/content/maps/thumb/[a-zA-Z\\s]+.png".r,
		raw"gamemodes/[a-zA-Z\\s]+/content/sound/[a-zA-Z\\s]+.wav".r,
		raw"gamemodes/[a-zA-Z\\s]+/content/sound/[a-zA-Z\\s]+.mp3".r,
		raw"gamemodes/[a-zA-Z\\s]+/content/sound/[a-zA-Z\\s]+.ogg".r
	)

	def pathIsWhiteListed(path: String): Boolean = {
		ADDON_WHITELIST.collectFirst{ case pattern =>
			path match {
				case pattern(_) => true
			}
		}.getOrElse(false)
	}
}
