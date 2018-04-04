name := "GithubContIntegration"
 
version := "1.0"

lazy val `githubcontintegration` = (project in file(".")).enablePlugins(PlayScala)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
      
resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
      
scalaVersion := "2.12.4"

libraryDependencies ++= Seq( jdbc , ehcache , ws , specs2 % Test , guice, "mysql" % "mysql-connector-java" % "5.1.29" )
libraryDependencies += javaJdbc
libraryDependencies += "com.google.cloud" % "google-cloud-pubsub" % "0.32.0-beta"

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )  
