lazy val commonSettings = Seq(
    scalaVersion := "2.12.12",
    scalacOptions := Seq("-deprecation", "-Xsource:2.11"),

    resolvers ++= Seq(
        Resolver.sonatypeRepo("snapshots"),
        Resolver.sonatypeRepo("releases")
    ),

    // Chisel 3.4
    libraryDependencies += "edu.berkeley.cs" %% "chisel-iotesters" % "1.5.1",
    libraryDependencies += "edu.berkeley.cs" %% "chiseltest" % "0.3.1",
    libraryDependencies += "org.jacop" % "jacop" % "4.7.0",
)
commonSettings

//JNI declarations
lazy val chiselverify = (project in file("."))
    .withId("chiselverify")

lazy val native = project
    // sbt-jni related settings
    .enablePlugins(JniNative) //Required to use nativeInit and nativeCompile
    .settings (
        commonSettings,
        target in javah := baseDirectory.value / "src/native/c/include", //where to generate header files
        target in nativeCompile := baseDirectory.value / "lib", //where to place output of nativeCompile
        sourceDirectory in nativeCompile := baseDirectory.value / "src/native/c" //Where to look for .c files when compiling
    )
    .dependsOn(chiselverify)

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

