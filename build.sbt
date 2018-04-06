name := "GithubContIntegration"
 
version := "1.0"

lazy val `githubcontintegration` = (project in file(".")).enablePlugins(PlayScala)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
      
resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
      
scalaVersion := "2.12.4"

libraryDependencies ++= Seq( evolutions, jdbc , ehcache , ws , specs2 % Test , guice, "mysql" % "mysql-connector-java" % "5.1.29" )
libraryDependencies ++= Seq( javaJdbc, "com.google.cloud" % "google-cloud-pubsub" % "0.43.0-beta", "com.google.cloud" % "google-cloud-storage" % "1.25.0" )
libraryDependencies += "org.playframework.anorm" %% "anorm" % "2.6.1"

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )  
