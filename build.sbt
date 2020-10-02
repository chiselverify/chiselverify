scalaVersion := "2.12.6"

scalacOptions := Seq("-Xsource:2.11")

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  Resolver.sonatypeRepo("releases")
)

// libraryDependencies += "edu.berkeley.cs" %% "chisel3" % "3.2.0"
libraryDependencies += "edu.berkeley.cs" %% "chisel-iotesters" % "1.4.2"
libraryDependencies += "edu.berkeley.cs" %% "chiseltest" % "0.2.2"

// sbt-jni related settings
enablePlugins(JniNative)
target in javah := file("src/native/include")
target in nativeCompile := file("lib")