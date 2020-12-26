def scalacOptionsVersion(scalaVersion: String): Seq[String] = {
	Seq() ++ {
		CrossVersion.partialVersion(scalaVersion) match {
			case Some((2, scalaMajor: Long)) if scalaMajor < 12 => Seq()
			case _ => Seq("-Xsource:2.11")
		}
	}
}

def javacOptionsVersion(scalaVersion: String): Seq[String] = {
	Seq() ++ {
		CrossVersion.partialVersion(scalaVersion) match {
			case Some((2, scalaMajor: Long)) if scalaMajor < 12 =>
				Seq("-source", "1.7", "-target", "1.7")
			case _ =>
				Seq("-source", "1.8", "-target", "1.8")
		}
	}
}

lazy val commonSettings = Seq(
	scalaVersion := "2.12.10",
	crossScalaVersions := Seq("2.12.10", "2.11.12"),
	resolvers ++= Seq(
		Resolver.sonatypeRepo("snapshots"),
		Resolver.sonatypeRepo("releases")
	),
	libraryDependencies += "edu.berkeley.cs" %% "chisel3" % "3.2.0",
	libraryDependencies += "edu.berkeley.cs" %% "chisel-iotesters" % "1.4.2",
	libraryDependencies += "edu.berkeley.cs" %% "chiseltest" % "0.2.2",
	scalacOptions ++= Seq("-deprecation", "-feature"),
	scalacOptions ++= scalacOptionsVersion(scalaVersion.value),
	javacOptions ++= javacOptionsVersion(scalaVersion.value)
)
commonSettings
