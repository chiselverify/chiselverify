lazy val commonSettings = Seq(
	scalaVersion := "2.12.6",
	scalacOptions := Seq("-Xsource:2.11"),
	resolvers ++= Seq(
		Resolver.sonatypeRepo("snapshots"),
		Resolver.sonatypeRepo("releases")
	),
	libraryDependencies += "edu.berkeley.cs" %% "chisel3" % "3.2.0",
	libraryDependencies += "edu.berkeley.cs" %% "chisel-iotesters" % "1.4.2",
	libraryDependencies += "edu.berkeley.cs" %% "chiseltest" % "0.2.2"
)
commonSettings

lazy val chiseluvm = (project in file("."))
	.withId("chisel-uvm")

lazy val native = project
// sbt-jni related settings
	.enablePlugins(JniNative) //Required to use nativeInit and nativeCompile
	.settings (
		commonSettings,
		target in javah := baseDirectory.value / "src/c/include", //where to generate header files
		target in nativeCompile := baseDirectory.value / "lib", //where to place output of nativeCompile
		sourceDirectory in nativeCompile := baseDirectory.value / "src/c" //Where to look for .c files when compiling
	)
	.dependsOn(chiseluvm)
