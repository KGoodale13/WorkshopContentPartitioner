import com.typesafe.config.ConfigFactory

package object gmpublish {
	val ICON_IMAGE = ConfigFactory.load().getString("workshop.icon_path")
	val GM_PUBLISH_LOCATION = ConfigFactory.load().getString("gmpublish_location")
}
