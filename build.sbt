name := "scaladex-keyword-badge"
organization := "lt.2m"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka"           %% "akka-http-experimental" % "2.4.9",
  "drewhk.com"                  %% "akka-xml-stream"        % "0.0.1-1-ga03571d",
  "net.sourceforge.htmlcleaner" % "htmlcleaner"             % "2.16",
  "com.nrinaudo"                %% "kantan.regex"           % "0.1.3",
  "org.scalatest"               %% "scalatest"              % "3.0.0" % Test,
  "com.typesafe.akka"           %% "akka-http-testkit"      % "2.4.9" % Test
)

resolvers += Resolver.bintrayRepo("2m", "maven")

scalafmtConfig in ThisBuild := Some(file(".scalafmt"))
reformatOnCompileSettings

enablePlugins(GitVersioning)
git.useGitDescribe := true

licenses += ("Apache-2.0", url("https://opensource.org/licenses/Apache-2.0"))
