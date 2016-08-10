//

lazy val root = (project in file(".")).aggregate(
  common,
  `slack-api`,
  `eval-bot`
)

lazy val akkaVersion       = "2.4.8"
lazy val catsVersion       = "0.6.1"
lazy val circeVersion      = "0.5.0-M2"
lazy val scalacheckVersion = "1.13.2"

lazy val common = (project in file("common"))
  .settings(name := "common")
  .settings(resolvers += Resolver.bintrayRepo("oncue", "releases"))
  .settings(libraryDependencies ++=
    // eventually this won't be a catch-all
    Seq(
      "org.typelevel"     %% "cats-core"              % catsVersion,
      "org.typelevel"     %% "cats-free"              % catsVersion,
      "io.circe"          %% "circe-core"             % circeVersion,
      "io.circe"          %% "circe-generic"          % circeVersion,
      "io.circe"          %% "circe-parser"           % circeVersion,
      "oncue.knobs"       %% "core"                   % "3.8.107",
      "com.chuusai"       %% "shapeless"              % "2.3.1",
      "com.typesafe.akka" %% "akka-http-core"         % akkaVersion,
      "com.typesafe.akka" %% "akka-http-experimental" % akkaVersion,
      "co.fs2"            %% "fs2-core"               % "0.9.0-M6",
      "co.fs2"            %% "fs2-cats"               % "0.1.0-M6"
    ) ++ Seq(
      "org.scalacheck"    %% "scalacheck"             % scalacheckVersion
    ).map(_ % "test") ++ Seq(
      "org.slf4j"          % "slf4j-simple"           % "1.7.21"
    ).map(_ % "runtime")
  )

lazy val `slack-api` = (project in file("slack-api"))
  .settings(name := "slack-api")
  .dependsOn(common)

lazy val `eval-bot` = (project in file("eval-bot"))
  .settings(name := "eval-bot")
  .dependsOn(common)
  .dependsOn(`slack-api`)
