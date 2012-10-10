sbtPlugin := true

organization := "org.technbolts"

name := "sbt-vaadin-plugin"

scalaVersion := "2.9.2"

version := "0.0.2-SNAPSHOT"

retrieveManaged := true

// moved to a global sbt file: credentials in ThisBuild += Credentials("Artifactory Realm", "sbpdevrepo", "artifactory", "password")

publishTo in ThisBuild <<= (version) { version: String =>
   val repo = "http://sbpdevrepo:8081/artifactory/"
   val (name, url) = if (version.contains("-SNAPSHOT"))
                       ("ext-snapshot-local", repo + "ext-snapshot-local")
                     else
                       ("ext-release-local", repo + "ext-release-local")
   Some(Resolver.url(name, new URL(url))(Resolver.ivyStylePatterns))
}


