package com.kright

import com.badlogic.gdx.{Gdx, Input, InputProcessor}
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.Vector3
import com.github.kright.math.MathUtils.clamp

import scala.language.{existentials, implicitConversions}
import scala.util.chaining.*

class MyCamera:
  val sensivityYaw = 0.03
  val sensivityPitch = 0.03

  var cameraDistance: Double = 5
  var yaw: Double = 0.0
  var pitch: Double = 0.0

  val camera: PerspectiveCamera =
    new PerspectiveCamera(67, Gdx.graphics.getWidth().toFloat, Gdx.graphics.getHeight().toFloat).tap { c =>
      c.near = 0.1f
      c.far = 100f
      c.lookAt(0, 0, 0)
    }

  updateCameraMatrix()

  def resize(width: Int, height: Int): Unit =
    camera.viewportWidth = width.toFloat
    camera.viewportHeight = height.toFloat
    camera.update(true)

  private def updateCameraMatrix(): Unit = {
    camera.position.set(getCameraPos)
    camera.lookAt(0, 0, 0)
    camera.up.set(0, 1, 0)
    camera.update()
  }

  def rotateFromMouse(): Unit =
    var cameraUpdated = false
    if (Gdx.input.isKeyPressed(Input.Keys.EQUALS)) {
      cameraDistance /= 1.01
      cameraUpdated = true
    }
    if (Gdx.input.isKeyPressed(Input.Keys.MINUS)) {
      cameraDistance *= 1.01
      cameraUpdated = true
    }
    if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
      val dx = Gdx.input.getDeltaX
      val dy = Gdx.input.getDeltaY
      yaw += dx * sensivityYaw
      pitch += dy * sensivityPitch
      normalizeAngles()
      cameraUpdated = true
    }

    if (cameraUpdated) {
      updateCameraMatrix()
    }

  private def normalizeAngles(): Unit =
    val eps = 0.01
    pitch = pitch.clamp(-Math.PI / 2 + eps, Math.PI / 2 - eps)
    if (yaw > Math.PI) yaw -= 2 * Math.PI
    if (yaw < -Math.PI) yaw += 2 * Math.PI

  private def getCameraPos: Vector3 =
    val x = Math.cos(yaw) * Math.cos(pitch) * cameraDistance
    val z = Math.sin(yaw) * Math.cos(pitch) * cameraDistance
    val y = Math.sin(pitch) * cameraDistance
    new Vector3(x.toFloat, y.toFloat, z.toFloat)
