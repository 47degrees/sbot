//

lazy val root = (project in file(".")).aggregate(
  common,
  `slack-api`,
  `eval-bot`
)

lazy val common = (project in file("common"))
  .settings(name := "common")
  .settings(resolvers += Resolver.bintrayRepo("oncue", "releases"))
  .settings(libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-http"              % "10.0.6",
    "com.typesafe.akka" %% "akka-http-core"         % "10.0.6",
    "io.circe"          %% "circe-core"             % "0.8.0",
    "io.circe"          %% "circe-parser"           % "0.8.0",
    "co.fs2"            %% "fs2-core"               % "0.9.5"))

lazy val `slack-api` = (project in file("slack-api"))
  .settings(name := "slack-api")
  .dependsOn(common)
  .settings(libraryDependencies ++= Seq(
    "org.typelevel"     %% "cats-core"              % "0.9.0",
    "org.typelevel"     %% "cats-free"              % "0.9.0",
    "co.fs2"            %% "fs2-core"               % "0.9.5",
    "co.fs2"            %% "fs2-cats"               % "0.3.0",
    "io.circe"          %% "circe-core"             % "0.8.0",
    "io.circe"          %% "circe-generic"          % "0.8.0",
    "io.circe"          %% "circe-parser"           % "0.8.0"))

lazy val `eval-bot` = (project in file("eval-bot"))
  .settings(name := "eval-bot")
  .dependsOn(common)
  .dependsOn(`slack-api`)
  .enablePlugins(JavaAppPackaging)
  .settings(libraryDependencies ++=
    Seq(
      "com.47deg"         %% "classy-core"            % "0.4.0",
      "com.47deg"         %% "classy-config-typesafe" % "0.4.0",
      "com.47deg"         %% "classy-generic"         % "0.4.0"
    ) ++ Seq(
      "org.slf4j"          % "slf4j-simple"           % "1.7.21"
    ).map(_ % "runtime"))
