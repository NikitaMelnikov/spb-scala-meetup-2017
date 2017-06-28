name := "SpbScalaMeetup2017.1"

organization := "ru.tinkoff"

version := "1.0"

scalaVersion := "2.12.1"

mainClass := Some("ru.tinkoff.Main")

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-remote" % "2.5.3"
)

test in assembly := {}
assemblyJarName := "application.jar"

enablePlugins(DockerPlugin)

dockerfile in docker := {
  val artifact: File = assembly.value
  val artifactTargetPath = s"/app/${artifact.name}"

  new Dockerfile {
    from("java")
    add(artifact, artifactTargetPath)
    entryPoint("java", "-jar", artifactTargetPath)
  }
}