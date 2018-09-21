name := "WorkshopContentPartitioner"

version := "0.1"

scalaVersion := "2.12.6"

libraryDependencies += "com.typesafe" % "config" % "1.3.2"
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.7"
libraryDependencies += "com.outr" %% "hasher" % "1.2.1"
libraryDependencies += "commons-io" % "commons-io" % "2.6"

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)

libraryDependencies += "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"