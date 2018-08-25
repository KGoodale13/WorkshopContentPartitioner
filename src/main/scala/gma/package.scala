import scala.util.matching.Regex

package object gma {

	// Hardcoded Addon format variables from here: https://github.com/garrynewman/gmad/blob/master/include/AddonFormat.h

	val ADDON_IDENT = "GMAD"
	val ADDON_VERSION = 3
	val ADDON_APPID = 4000

	val ADDON_WHITELIST: List[Regex] = List(
		raw"lua/[a-zA-Z_\-\s\/\d\.]+\.lua$$".r,
		raw"scenes/[a-zA-Z_\-\s\/\d\.]+\.vcd$$".r,
		raw"particles/[a-zA-Z_\-\s\/\d\.]+\.pcf$$".r,
		raw"resource/fonts/[a-zA-Z_\-\s\/\d\.]+\.ttf$$".r,
		raw"scripts/vehicles/[a-zA-Z_\-\s\/\d\.]+\.txt$$".r,
		raw"resource/localization/[a-zA-Z_\-\s\/\d\.]+/[a-zA-Z_\-\s\/\d\.]+\.properties$$".r,
		raw"maps/[a-zA-Z_\-\s\/\d\.]+\.bsp$$".r,
		raw"maps/[a-zA-Z_\-\s\/\d\.]+\.nav$$".r,
		raw"maps/[a-zA-Z_\-\s\/\d\.]+\.ain$$".r,
		raw"maps/thumb/[a-zA-Z_\-\s\/\d\.]+\.png$$".r,
		raw"sound/[a-zA-Z_\-\s\/\d\.]+\.wav$$".r,
		raw"sound/[a-zA-Z_\-\s\/\d\.]+\.mp3$$".r,
		raw"sound/[a-zA-Z_\-\s\/\d\.]+\.ogg$$".r,
		raw"materials/[a-zA-Z_\-\s\/\d\.]+\.vmt$$".r,
		raw"materials/[a-zA-Z_\-\s\/\d\.]+\.vtf$$".r,
		raw"materials/[a-zA-Z_\-\s\/\d\.]+\.png$$".r,
		raw"materials/[a-zA-Z_\-\s\/\d\.]+\.jpg$$".r,
		raw"materials/[a-zA-Z_\-\s\/\d\.]+\.jpeg$$".r,
		raw"models/[a-zA-Z_\-\s\/\d\.]+\.mdl$$".r,
		raw"models/[a-zA-Z_\-\s\/\d\.]+\.vtx$$".r,
		raw"models/[a-zA-Z_\-\s\/\d\.]+\.phy$$".r,
		raw"models/[a-zA-Z_\-\s\/\d\.]+\.ani$$".r,
		raw"models/[a-zA-Z_\-\s\/\d\.]+\.vvd$$".r,
		raw"gamemodes/[a-zA-Z_\-\s\/\d\.]+/[a-zA-Z_\-\s\/\d\.]+\.txt$$".r,
		raw"gamemodes/[a-zA-Z_\-\s\/\d\.]+/[a-zA-Z_\-\s\/\d\.]+\.fgd$$".r,
		raw"gamemodes/[a-zA-Z_\-\s\/\d\.]+/logo.png$$".r,
		raw"gamemodes/[a-zA-Z_\-\s\/\d\.]+/icon24.png$$".r,
		raw"gamemodes/[a-zA-Z_\-\s\/\d\.]+/gamemode/[a-zA-Z_\-\s\/\d\.]+\.lua$$".r,
		raw"gamemodes/[a-zA-Z_\-\s\/\d\.]+/entities/effects/[a-zA-Z_\-\s\/\d\.]+\.lua$$".r,
		raw"gamemodes/[a-zA-Z_\-\s\/\d\.]+/entities/weapons/[a-zA-Z_\-\s\/\d\.]+\.lua$$".r,
		raw"gamemodes/[a-zA-Z_\-\s\/\d\.]+/entities/entities/[a-zA-Z_\-\s\/\d\.]+\.lua$$".r,
		raw"gamemodes/[a-zA-Z_\-\s\/\d\.]+/backgrounds/[a-zA-Z_\-\s\/\d\.]+\.png$$".r,
		raw"gamemodes/[a-zA-Z_\-\s\/\d\.]+/backgrounds/[a-zA-Z_\-\s\/\d\.]+\.jpg$$".r,
		raw"gamemodes/[a-zA-Z_\-\s\/\d\.]+/backgrounds/[a-zA-Z_\-\s\/\d\.]+\.jpeg$$".r,
		raw"gamemodes/[a-zA-Z_\-\s\/\d\.]+/content/models/[a-zA-Z_\-\s\/\d\.]+\.mdl$$".r,
		raw"gamemodes/[a-zA-Z_\-\s\/\d\.]+/content/models/[a-zA-Z_\-\s\/\d\.]+\.vtx$$".r,
		raw"gamemodes/[a-zA-Z_\-\s\/\d\.]+/content/models/[a-zA-Z_\-\s\/\d\.]+\.phy$$".r,
		raw"gamemodes/[a-zA-Z_\-\s\/\d\.]+/content/models/[a-zA-Z_\-\s\/\d\.]+\.ani$$".r,
		raw"gamemodes/[a-zA-Z_\-\s\/\d\.]+/content/models/[a-zA-Z_\-\s\/\d\.]+\.vvd$$".r,
		raw"gamemodes/[a-zA-Z_\-\s\/\d\.]+/content/materials/[a-zA-Z_\-\s\/\d\.]+\.vmt$$".r,
		raw"gamemodes/[a-zA-Z_\-\s\/\d\.]+/content/materials/[a-zA-Z_\-\s\/\d\.]+\.vtf$$".r,
		raw"gamemodes/[a-zA-Z_\-\s\/\d\.]+/content/materials/[a-zA-Z_\-\s\/\d\.]+\.png$$".r,
		raw"gamemodes/[a-zA-Z_\-\s\/\d\.]+/content/materials/[a-zA-Z_\-\s\/\d\.]+\.jpg$$".r,
		raw"gamemodes/[a-zA-Z_\-\s\/\d\.]+/content/materials/[a-zA-Z_\-\s\/\d\.]+\.jpeg$$".r,
		raw"gamemodes/[a-zA-Z_\-\s\/\d\.]+/content/scenes/[a-zA-Z_\-\s\/\d\.]+\.vcd$$".r,
		raw"gamemodes/[a-zA-Z_\-\s\/\d\.]+/content/particles/[a-zA-Z_\-\s\/\d\.]+\.pcf$$".r,
		raw"gamemodes/[a-zA-Z_\-\s\/\d\.]+/content/resource/fonts/[a-zA-Z_\-\s\/\d\.]+\.ttf$$".r,
		raw"gamemodes/[a-zA-Z_\-\s\/\d\.]+/content/scripts/vehicles/[a-zA-Z_\-\s\/\d\.]+\.txt$$".r,
		raw"gamemodes/[a-zA-Z_\-\s\/\d\.]+/content/resource/localization/[a-zA-Z_\-\s\/\d\.]+/[a-zA-Z_\-\s\/\d\.]+\.properties$$".r,
		raw"gamemodes/[a-zA-Z_\-\s\/\d\.]+/content/maps/[a-zA-Z_\-\s\/\d\.]+\.bsp$$".r,
		raw"gamemodes/[a-zA-Z_\-\s\/\d\.]+/content/maps/[a-zA-Z_\-\s\/\d\.]+\.nav$$".r,
		raw"gamemodes/[a-zA-Z_\-\s\/\d\.]+/content/maps/[a-zA-Z_\-\s\/\d\.]+\.ain$$".r,
		raw"gamemodes/[a-zA-Z_\-\s\/\d\.]+/content/maps/thumb/[a-zA-Z_\-\s\/\d\.]+\.png$$".r,
		raw"gamemodes/[a-zA-Z_\-\s\/\d\.]+/content/sound/[a-zA-Z_\-\s\/\d\.]+\.wav$$".r,
		raw"gamemodes/[a-zA-Z_\-\s\/\d\.]+/content/sound/[a-zA-Z_\-\s\/\d\.]+\.mp3$$".r,
		raw"gamemodes/[a-zA-Z_\-\s\/\d\.]+/content/sound/[a-zA-Z_\-\s\/\d\.]+\.ogg$$".r
	)

	def pathIsWhiteListed(path: String): Boolean = {
		ADDON_WHITELIST.collectFirst{ case p if p.findFirstIn(path).isDefined => true }.getOrElse(false)
	}
}
