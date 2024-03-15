package com.kright

import com.badlogic.gdx.math.Matrix4
import com.github.kright.math.{DifferentialSolvers, IVector3d, Matrix4d, Vector3d}
import com.github.kright.physics3d.*

class WingNut:
  val body: Inertia3d = new Inertia3d(1.0, Vector3d(3.0, 2.0, 1.0))
  val state: State3d = new State3d()
  state.velocity.angular := (0.05, 4.0, 0.05)

  val step = 0.001
  var t: Double = 0.0

  def setToMatrix(m: Matrix4): Unit =
    val q = state.transform.rotation
    val m4d = Matrix4d() := q
    m := m4d

  def simulate(dtSeconds: Double): Unit =
    val tt = t + dtSeconds
    while (t < tt) {
      t += step
      doStep(step)
    }

  private def doStep(dt: Double): Unit =
    val (k1, k2, k3, k4) = DifferentialSolvers.rungeKutta4K(state, time = 0.0, dt,
      getDerivative = (state, time) => State3dDerivative(state, body, Force3d()),
      nextState = (state, derivative, dt) => state.copy().madd(derivative, dt).normalize(),
    )
    state
      .madd(k1, dt * (1.0 / 6.0))
      .madd(k2, dt * (2.0 / 6.0))
      .madd(k3, dt * (2.0 / 6.0))
      .madd(k4, dt * (1.0 / 6.0))
      .normalize()