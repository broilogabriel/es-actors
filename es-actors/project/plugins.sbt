logLevel := Level.Warn

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.6.1")

addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.6.0")

// Coverage plugins:
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.0")

addSbtPlugin("org.scoverage" %% "sbt-coveralls" % "1.1.1-SNAPSHOT")

lazy val root = (project in file(".")).dependsOn(coverallsPlugin)

lazy val coverallsPlugin = uri("https://github.com/NewsWhip/sbt-coveralls.git")

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.8.0")



