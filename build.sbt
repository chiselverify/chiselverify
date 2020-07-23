scalaVersion := "2.12.6"

scalacOptions := Seq("-Xsource:2.11")

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  Resolver.sonatypeRepo("releases")
)

 libraryDependencies += "edu.berkeley.cs" %% "chisel3" % "3.2.0"
 libraryDependencies += "edu.berkeley.cs" %% "chisel-iotesters" % "1.3.0"

// libraryDependencies += "edu.berkeley.cs" %% "chisel3" % "latest.release"
// libraryDependencies += "edu.berkeley.cs" %% "chisel-iotesters" % "latest.release"