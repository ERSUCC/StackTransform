import java.nio.file.{ Files, Paths }

import sbt.{ InputKey, IO }
import sbt.complete.DefaultParsers.{ Space, StringBasic }

name := "Stack Transform"
version := "0.2.0"
organization := "org.ersucc"

scalaVersion := "3.7.0"

scalacOptions ++= Seq("-Werror", "-Wunused:linted")

libraryDependencies += "net.imagej" % "ij" % "1.54p"

val packageTo = InputKey[Unit]("packageTo", "Package and copy the compiled jar to the specified directory")

packageTo := {
  val path = Paths.get((Space ~> StringBasic).parsed)

  if (!Files.exists(path))
    throw new Exception("The specified path does not exist.")

  if (!Files.isDirectory(path))
    throw new Exception("The specified path is not a directory.")

  val jar = (Compile / packageBin).value

  IO.copyFile(jar, path.resolve(jar.getName).toFile)
}
