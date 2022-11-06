package com.kright

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration

@main
def runLibgdxDemo(): Unit =
  val config = new Lwjgl3ApplicationConfiguration
  config.setForegroundFPS(60)
  config.setTitle("RotationDemo")
  config.setWindowedMode(780, 440)
  new Lwjgl3Application(new RotationDemo, config)
