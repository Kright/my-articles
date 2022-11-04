package com.kright

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader
import com.badlogic.gdx.graphics.g3d.{Environment, Model, ModelBatch, ModelInstance}
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.graphics.{GL20, PerspectiveCamera, Texture}
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.{ApplicationAdapter, Gdx}
import com.kright.math.{IVector3d, Vector3d}

import scala.language.implicitConversions
import scala.util.chaining.*

class RotationDemo extends ApplicationAdapter:
  private val loader = new ObjLoader()
  private val jm = new WingNut()

  private var model: Model = null
  private var instance: ModelInstance = null
  private var modelBatch: ModelBatch = null
  private var camera: PerspectiveCamera = null
  private var shapeRenderer: ShapeRenderer = null

  private val environment = new Environment().tap { e =>
    e.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
    e.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f))
  }

  override def create(): Unit =
    model = loader.loadModel(Gdx.files.internal("assets/wingNut.obj"))
    instance = new ModelInstance(model)
    modelBatch = new ModelBatch()

    camera = new PerspectiveCamera(67, Gdx.graphics.getWidth().toFloat, Gdx.graphics.getHeight().toFloat).tap{ c =>
      c.position.set(2, 2, 2)
      c.lookAt(0, 0, 0)
      c.near = 0.1f
      c.far = 100f
      c.update()
    }

    shapeRenderer = ShapeRenderer()
    Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth, Gdx.graphics.getHeight)

  private def update(): Unit =
    val dt = Gdx.graphics.getDeltaTime.toDouble
    jm.simulate(dt)
    jm.setToMatrix(instance.transform)
    instance.calculateTransforms()

  override def resize(width: Int, height: Int): Unit =
    Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth, Gdx.graphics.getHeight)
    camera.viewportWidth = width.toFloat
    camera.viewportHeight = height.toFloat
    camera.update(true)

  private def renderModel(): Unit =
    modelBatch.begin(camera)
    modelBatch.render(instance, environment)
    modelBatch.end()

  private def renderArrows(): Unit =
    shapeRenderer.setProjectionMatrix(camera.combined)

    shapeRenderer.begin(ShapeType.Line)
    shapeRenderer.setColor(1, 0, 0, 1)
    shapeRenderer.line(Vector3d(0, 0, 0), jm.state.transform.rotation * Vector3d(1, 0, 0))
    shapeRenderer.setColor(0, 1, 0, 1)
    shapeRenderer.line(Vector3d(0, 0, 0), jm.state.transform.rotation * Vector3d(0, 1, 0))
    shapeRenderer.setColor(0, 0, 1, 1)
    shapeRenderer.line(Vector3d(0, 0, 0), jm.state.transform.rotation * Vector3d(0, 0, 1))
    shapeRenderer.setColor(0.5, 0.5, 0.5, 1)
    shapeRenderer.line(Vector3d(0, 0, 0), jm.state.velocity.angular.normalized())
    shapeRenderer.end()

  override def render(): Unit =
    update()

    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT)

    renderModel()
    renderArrows()

  override def dispose(): Unit =
    modelBatch.dispose()
    model.dispose()
    shapeRenderer.dispose()
