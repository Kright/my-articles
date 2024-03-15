package com.kright

import com.badlogic.gdx.backends.lwjgl3.{Lwjgl3Application, Lwjgl3ApplicationConfiguration}
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader
import com.badlogic.gdx.graphics.g3d.{Environment, ModelBatch, ModelInstance}
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.{ApplicationAdapter, Gdx, Input}
import com.github.kright.math.*
import com.github.kright.physics3d.*

import scala.language.{existentials, implicitConversions}
import scala.util.chaining.*

/**
 * space: pause
 * keys 0-1-2.. 5 is different models
 * "+" "-" for scale
 * mouse for rotation
 */

@main
def runPhysicsDemo(): Unit =
  val config = new Lwjgl3ApplicationConfiguration()
  config.setForegroundFPS(60)
  config.setTitle("RotationDemo")
  config.setWindowedMode(800, 600)
  new Lwjgl3Application(new ModelDemo(TestModel.modelFactories), config)


class ModelDemo(private val factories: Seq[() => BodySystem[Joint3d]]) extends ApplicationAdapter:
  private lazy val loader = new ObjLoader()
  private lazy val model = loader.loadModel(Gdx.files.internal("assets/wingNut.obj"))
  private lazy val instances: Array[ModelInstance] = physicsModel.states.map(_ => new ModelInstance(model)).toArray
  private lazy val modelBatch: ModelBatch = new ModelBatch()
  private lazy val camera: MyCamera = MyCamera()
  private lazy val shapeRenderer: ShapeRenderer = ShapeRenderer()

  private var isPaused: Boolean = true
  private var physicsModel: BodySystem[Joint3d] = factories(0)()

  private val environment = new Environment().tap { e =>
    e.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
    e.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f))
  }

  override def create(): Unit =
    Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth, Gdx.graphics.getHeight)
    camera.resize(Gdx.graphics.getWidth, Gdx.graphics.getHeight)

  private def getPressedNumber(): Option[Int] =
    (Input.Keys.NUM_0 to Input.Keys.NUM_9)
      .find(Gdx.input.isKeyJustPressed(_))
      .map(_ - Input.Keys.NUM_0)

  private def update(): Unit =
    val dt = Gdx.graphics.getDeltaTime.toDouble

    if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
      isPaused = !isPaused
    }

    if (!isPaused) {
      physicsModel.doStepRK2(dt = 0.01)
    }

    for(number <- getPressedNumber();
        factory <- factories.lift(number)) {
      physicsModel = factory()
    }

    for ((instance, body) <- instances.zip(physicsModel.states)) {
      val m4d = Matrix4d() := body.transform.rotation
      val translate = Matrix4d().setTranslation(body.transform.position)
      instance.transform := (translate * m4d)
      instance.calculateTransforms()
    }

    camera.rotateFromMouse()

  override def resize(width: Int, height: Int): Unit =
    Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth, Gdx.graphics.getHeight)
    camera.resize(Gdx.graphics.getWidth, Gdx.graphics.getHeight)

  private def renderModel(): Unit =
    modelBatch.begin(camera.camera)
    for (instance <- instances) {
      modelBatch.render(instance, environment)
    }
    modelBatch.end()

  private def renderArrows(center: IVector3d, rotation: IQuaternion, mag: Double = 1.0): Unit = {
    shapeRenderer.setColor(1, 0, 0, 1)
    shapeRenderer.line(center, center + rotation * Vector3d(mag, 0, 0))
    shapeRenderer.setColor(0, 1, 0, 1)
    shapeRenderer.line(center, center + rotation * Vector3d(0, mag, 0))
    shapeRenderer.setColor(0, 0, 1, 1)
    shapeRenderer.line(center, center + rotation * Vector3d(0, 0, mag))
  }

  private def renderJoints(): Unit =
    shapeRenderer.setColor(1, 0, 1, 1)
    for ((joint, i, j) <- physicsModel.joints) {
      val pi = physicsModel.states(i).transform.local2global(dr1local(joint))
      val pj = physicsModel.states(j).transform.local2global(dr2local(joint))
      shapeRenderer.line(pi, pj)

      joint match
        case angSpring: AngularSpring3d => renderAngularJoint(shapeRenderer, angSpring, physicsModel.states(i), physicsModel.states(j))
        case _ => ()
    }

  private def renderAngularJoint(shapeRenderer: ShapeRenderer, joint: AngularSpring3d,
                                 firstBody: State3d, secondBody: State3d): Unit =
    val dr1global = firstBody.q * joint.localPos1.r
    val dr2global = secondBody.q * joint.localPos2.r

    val globalPos1 = firstBody.transform.local2global(joint.localPos1)
    val globalPos2 = secondBody.transform.local2global(joint.localPos2)
    val r12 = globalPos2.r - globalPos1.r
    val r12Mag = r12.mag
    require(r12Mag > 1e-12)

    val r12Norm = r12 / r12Mag
    val oX = Vector3d(1, 0, 0)

    val dq1 = Quaternion.fromAxisToAxis(globalPos1.q * oX, r12Norm)
    val dq2 = Quaternion.fromAxisToAxis(globalPos2.q * oX, r12Norm)
      // dq1 and dq2 rotation axis are perpendicular to r

    val q1r12 = dq1 * globalPos1.q
    val q2r12 = dq2 * globalPos2.q
      // dq * q1r12 = q2r12
      // dq is rotation along r

    renderArrows(globalPos1.r, q1r12, mag = 0.5)
    renderArrows(globalPos2.r, q2r12, mag = 0.5)
  //    val dqr = q2r12 * q1r12.conjugated()

  private def dr1local(joint: Joint3d): IVector3d =
    joint match
      case spring: Spring3d => spring.dr1local
      case angularSpring: AngularSpring3d => angularSpring.localPos1.r
      case angularFriction3d: AngularFriction3d => angularFriction3d.localPos1
      case orientationSpring3d: OrientationSpring3d => Vector3d()

  private def dr2local(joint: Joint3d): IVector3d =
    joint match
      case spring: Spring3d => spring.dr2local
      case angularSpring: AngularSpring3d => angularSpring.localPos2.r
      case angularFriction3d: AngularFriction3d => angularFriction3d.localPos2
      case orientationSpring3d: OrientationSpring3d => Vector3d()

  private def renderArrows(): Unit =
    shapeRenderer.setProjectionMatrix(camera.camera.combined)
    shapeRenderer.begin(ShapeType.Line)

    renderJoints()
    renderArrows(Vector3d(), Quaternion.id) // center of coordinate system

    for (body <- physicsModel.states) {
      val center = body.transform.position
      val rotation = body.transform.rotation
//      renderArrows(center, rotation)
      shapeRenderer.setColor(0.5, 0.5, 0.5, 1)
      shapeRenderer.line(center, center + body.velocity.angular)
    }

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


