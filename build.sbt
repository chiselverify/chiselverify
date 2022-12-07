scalaVersion := "2.12.15"

scalacOptions ++= Seq(
  "-language:reflectiveCalls",
  "-language:implicitConversions", // silences all warnings about implicit conversions
  "-deprecation",
  "-feature",
  "-Xcheckinit",
)

// Chisel 3.5
libraryDependencies += "edu.berkeley.cs" %% "chiseltest" % "0.5.1"
addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % "3.5.1" cross CrossVersion.full)

libraryDependencies += "org.jacop" % "jacop" % "4.7.0"
libraryDependencies += "org.jliszka" %% "probability-monad" % "1.0.3"

// library name
name := "chiselverify"

// library version
version := "0.3.0"

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
