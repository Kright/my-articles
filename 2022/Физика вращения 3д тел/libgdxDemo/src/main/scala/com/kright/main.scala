package com.kright

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration

@main
def runLibgdxDemo(): Unit =
  val config = new Lwjgl3ApplicationConfiguration
  config.setForegroundFPS(60)
  config.setTitle("RotationDemo")
  new Lwjgl3Application(new RotationDemo, config)
