scalaVersion := "2.12.12"

scalacOptions := Seq("-deprecation", "-Xsource:2.11")


resolvers ++= Seq(
	Resolver.sonatypeRepo("snapshots"),
	Resolver.sonatypeRepo("releases")
)

// Chisel 3.4
libraryDependencies += "edu.berkeley.cs" %% "chisel-iotesters" % "1.5.1"
libraryDependencies += "edu.berkeley.cs" %% "chiseltest" % "0.3.1"
libraryDependencies += "org.jacop" % "jacop" % "4.7.0"

// library name
name := "chiselverify"

// library version
version := "0.1"

// groupId, SCM, license information
organization := "io.github.chiselverify"
homepage := Some(url("https://github.com/chiselverify/chiselverify"))
scmInfo := Some(ScmInfo(url("https://github.com/chiselverify/chiselverify"), "git@github.com: chiselverify/chiselverify.git"))
developers := List(Developer("schoeberl", "schoeberl", "martin@jopdesign.com", url("https://github.com/schoeberl")))
licenses += ("BSD-2-Clause", url("https://opensource.org/licenses/BSD-2-Clause"))
publishMavenStyle := true

// disable publishw ith scala version, otherwise artifact name will include scala version 
// e.g cassper_2.11
crossPaths := false

// add sonatype repository settings
// snapshot versions publish to sonatype snapshot repository
// other versions publish to sonatype staging repository
publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)

