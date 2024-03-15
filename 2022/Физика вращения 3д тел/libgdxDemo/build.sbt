ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.4.0"

resolvers += "jitpack" at "https://jitpack.io"

lazy val root = (project in file("."))
  .settings(
    name := "rotationDemo",
    libraryDependencies += "com.badlogicgames.gdx" % "gdx" % "1.12.0",
    libraryDependencies += "com.badlogicgames.gdx" % "gdx-backend-lwjgl3" % "1.12.0",
    libraryDependencies += "com.badlogicgames.gdx" % "gdx-platform" % "1.12.0" classifier "natives-desktop",
    libraryDependencies += "com.github.Kright.ScalaGameMath" %% "physics3d" % "0.4.0",
    libraryDependencies += "org.jfree" % "jfreechart" % "1.5.4",
  )
