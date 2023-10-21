package com.kright

import com.kright.math.{IVector3d, Vector3d}
import com.kright.physics3d.{Transform3d, *}

import scala.collection.mutable.ArrayBuffer
import scala.util.chaining.*
import scala.language.implicitConversions

object TestModel:
  val modelFactories: Seq[() => BodySystem[Joint3d]] = Seq(
    () => modelOfGyroscopePrecession(),
    () => linearModel(),
    () => modelTwistedSpring(kT = 1.0),
    () => modelChained(),
    () => modelChainedOneShifted(),
    () => modelChainedComboPart(),
  )

  def linearModel(friction: Friction = Friction.zero,
                  mass1: Double = 1.0,
                  mass2: Double = 1.0): BodySystem[Joint3d] =
    val model = BodySystem[Joint3d]()
    model.addBody(Inertia3d(mass1, Vector3d(1.0, 1.0, 1.0)), State3d().tap(_.transform.position.x = 1))
    model.addBody(Inertia3d(mass2, Vector3d(1.0, 1.0, 1.0)), State3d().tap(_.transform.position.x = -1))
    model.addJoint(Spring3d(Vector3d(), Vector3d(), r0 = 4.0, k=1.0, friction), 0, 1)
    model

  def modelTwistedSpring(kT: Double,
                         wrFriction: Friction = Friction.zero,
                         i1: Double = 1.0,
                         i2: Double = 1.0,
                         posShift: IVector3d = Vector3d(1, 0, 0)): BodySystem[Joint3d] =
    val model = BodySystem[Joint3d]()
    model.addBody(Inertia3d(1.0, Vector3d(i1, i1, i1)), State3d().tap{ s =>
      s.transform.position += posShift
      s.velocity.angular.x = 1
    })
    model.addBody(Inertia3d(1.0, Vector3d(i2, i2, i2)), State3d().tap{ s =>
      s.transform.position -= posShift
      s.velocity.angular.x = -1
    })

    model.addJoint(AngularSpring3d(
      localPos1 = Transform3d(),
      localPos2 = Transform3d(),
      k1 = 0.0,
      k2 = 0.0,
    ), 0, 1)

    model.addJoint(OrientationSpring3d(model.states(0).q, model.states(1).q, kT, wrFriction), 0, 1)

    model

  def modelChained(wpFriction: Friction = Friction.zero,
                   i1: Double = 1.0,
                   i2: Double = 1.0): BodySystem[Joint3d] =

    val model = BodySystem[Joint3d]()
    model.addBody(Inertia3d(1.0, Vector3d(i1, i1, i1)), State3d().tap(_.transform.position.z = 1).tap(_.velocity.angular.x = 1))
    model.addBody(Inertia3d(1.0, Vector3d(i2, i2, i2)), State3d().tap(_.transform.position.z = -1).tap(_.velocity.angular.x = 1))

    model.addJoint(Spring3d(Vector3d(), Vector3d(), k = 1.0, r0 = 2, friction = Friction.zero), 0, 1)
    model.addJoint(AngularFriction3d(Vector3d(), Vector3d(), AngularFriction3dParams(Friction.zero, wpFriction, wpFriction)), 0, 1)

    model

  def modelChainedOneShifted(linearFriction: Friction = Friction.zero,
                             wrFriction: Friction = Friction.zero,
                             wpFriction: Friction = Friction.zero,
                             i1: Double = 1.0,
                             i2: Double = 1.0): BodySystem[Joint3d] =
    val model = BodySystem[Joint3d]()

    model.addBody(Inertia3d(1.0, Vector3d(i1, i1, i1)), State3d().tap(_.transform.position.z = 1).tap(_.velocity.angular.x = 1))
    model.addBody(Inertia3d(1.0, Vector3d(i2, i2, i2)), State3d().tap(_.transform.position.z = -1).tap(_.velocity.angular.x = 1))

    model.addJoint(Spring3d(Vector3d(0, 0.5, 0), Vector3d(), k = 1.0, r0 = 2, linearFriction), 0, 1)
    model.addJoint(AngularFriction3d(Vector3d(0, 0.5, 0), Vector3d(), AngularFriction3dParams(wrFriction, wpFriction, wpFriction)), 0, 1)

    model

  def modelChainedComboPart(wpFriction: Friction = Friction.zero,
                            i1: Double = 1.0,
                            i2: Double = 1.0): BodySystem[Joint3d] =
    val model = BodySystem[Joint3d]()

    model.addBody(Inertia3d(1.0, Vector3d(i1, i1, i1)), State3d().tap { s =>
      s.transform.position.x = 2
      s.velocity.angular.z = 1
    })
    model.addBody(Inertia3d(1.0, Vector3d(i1, i1, i1)), State3d().tap { s =>
      s.transform.position.x = -2
      s.velocity.angular.x = 1
    })

    val localPos1 = Vector3d(0, 0, -0.5)
    val localPos2 = Vector3d(0, 0, 0)
    model.addJoint(Spring3d(localPos1, localPos2, k = 1.0, r0 = 3, friction = Friction.zero), 0, 1)
    model.addJoint(AngularFriction3d(localPos1, localPos2, AngularFriction3dParams(Friction.zero, wpFriction, wpFriction)), 0, 1)
    model

  def modelOfGyroscopePrecession(): BodySystem[Joint3d] =
    val model = new BodySystem[Joint3d]() {
      override def getDerivative(currentState: State, time: Double): Derivative =
        super.getDerivative(currentState, time).tap{ d =>
          d(0) := State3dDerivative() // fix first body
          d(1).velocity.linear.y -= 1.0 // add gravitation with g=1 to second body
        }
    }

    val dx = 0.5

    model.addBody(Inertia3d(100.0, Vector3d(1.0, 1.0, 1.0)), State3d().tap{ s =>
      s.transform.position.y = 5.0
    })
    model.addBody(Inertia3d(1.0, Vector3d(1.0, 1.0, 1.0)), State3d().tap { s =>
      s.transform.position.x = dx
      s.velocity.angular.x = 5.0
    })
    model.addJoint(Spring3d(Vector3d(), Vector3d(-dx, 0.0, 0.0), k = 1.0, r0 = 4.0, friction = Friction.zero), 0, 1)

    model


def makeSnapshotFromVec(name: String, v: IVector3d): Map[String, Double] =
  Map(
    s"$name.x" -> v.x,
    s"$name.y" -> v.y,
    s"$name.z" -> v.z,
  )

def makeSnapshot[T <: Joint3d](model: BodySystem[T]): Map[String, Double] =
  val stats = model.calculateStats()
  val potentialEnergy = model.getPotentialEnergy()

  Map(
    "kinetic energy" -> stats.kineticEnergy,
    "potential energy" -> potentialEnergy,
    "total energy" -> (potentialEnergy + stats.kineticEnergy),
    "body 0 = L.x" -> model.states(0).velocity.angular.x,
    "body 1 = L.x" -> model.states(1).velocity.angular.x,
  )
    //    ++ makeSnapshotFromVec("center of mass", model.centerOfMass)
    //    ++ makeSnapshotFromVec("linear impulse", model.linearImpulse)
    ++ makeSnapshotFromVec("angular impulse", stats.angularImpulse)

@main
def ModelTestLinear(): Unit =
  val model = TestModel.modelTwistedSpring(1.0, Friction.linear(1.0), i1 = 1.0, i2 = 2.0, posShift = Vector3d(1, 1, 0))

  val dt = 0.01
  val totalTime = 10.0

  def str(s: State3d): String =
    s"State3d(${s.transform}, ${s.velocity})"

  def printModelState(step: Int): Unit =
    val stats = model.calculateStats()
    val potentialEnergy = model.getPotentialEnergy()

    println(
      s"""${step}:
         |model kinetic energy = ${stats.kineticEnergy}
         |model potential energy = ${potentialEnergy}
         |model total energy = ${potentialEnergy + stats.kineticEnergy}
         |first body = ${str(model.states(0))}
         |second body = ${str(model.states(1))}
         |centerOfMass = ${stats.center}
         |impulse = ${stats.impulse}
         |""".stripMargin
    )

  val snapshots = ArrayBuffer[Map[String, Double]]()
  snapshots += makeSnapshot(model)
  printModelState(0)

  for (i <- 0 until (totalTime / dt).toInt) {
    model.doStepRK2(dt)

    printModelState(i + 1)
    snapshots += makeSnapshot(model)
  }

  showChart(snapshots.toIndexedSeq, dt)
