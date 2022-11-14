package com.kright.benchmark

import com.kright.math.{DifferentialSolvers, IQuaternion, Quaternion, Vector3d}
import com.kright.physics3d.*

trait Solver:
  def getNextState(inertia: Inertia3d, initial: State3d, force: Force3d, dt: Double): State3d


class SolverEulerNaive extends Solver:
  def getNextState(inertia: Inertia3d, initial: State3d, force: Force3d, dt: Double): State3d =
    val derivative = inertia.getDerivative(initial, force)
    initial.updated(derivative, dt)

class SolverEuler2 extends Solver:
  def getNextState(inertia: Inertia3d, initial: State3d, force: Force3d, dt: Double): State3d =
    DifferentialSolvers.euler2(initial, 0.0, dt,
      getDerivative = (s, t) => inertia.getDerivative(s, force),
      nextState = (s, d, dt) => s.updated(d, dt),
      newZeroDerivative = () => new State3dDerivative(),
      madd = (acc, d, m) => acc.madd(d, m)
    )

class SolverEuler2Alt extends SolverAlt:
  override def getNextState(inertia: Inertia3d, initial: State3d, force: Force3d, dt: Double): State3d =
    val result = DifferentialSolvers.euler2[State3d, State3d](initial, 0.0, dt,
      getDerivative(inertia, force),
      nextState,
      newZeroDerivative,
      madd = (state, add, weight) => state.madd(add, weight)
    )
    result.transform.rotation.normalize()
    result

class SolverRK2 extends Solver:
  def getNextState(inertia: Inertia3d, initial: State3d, force: Force3d, dt: Double): State3d =
    DifferentialSolvers.rungeKutta2(initial, 0.0, dt,
      getDerivative = (s, t) => inertia.getDerivative(s, force),
      nextState = (s, d, dt) => s.updated(d, dt),
    )

class SolverRK2Alt extends SolverAlt:
  override def getNextState(inertia: Inertia3d, initial: State3d, force: Force3d, dt: Double): State3d =
    val result = DifferentialSolvers.rungeKutta2[State3d, State3d](initial, 0.0, dt,
      getDerivative(inertia, force),
      nextState
    )
    result.transform.rotation.normalize()
    result

class SolverRK4 extends Solver:
  def getNextState(inertia: Inertia3d, initial: State3d, force: Force3d, dt: Double): State3d =
    DifferentialSolvers.rungeKutta4(initial, 0.0, dt,
      getDerivative = (s, t) => inertia.getDerivative(s, force),
      nextState = (s, d, dt) => s.updated(d, dt),
      newZeroDerivative = () => new State3dDerivative(),
      madd = (acc, d, m) => acc.madd(d, m)
    )

class SolverRK4Alt extends SolverAlt:
  override def getNextState(inertia: Inertia3d, initial: State3d, force: Force3d, dt: Double): State3d =
    val (k1, k2, k3, k4) = DifferentialSolvers.rungeKutta4K[State3d, State3d](initial, 0.0, dt,
      getDerivative(inertia, force),
      nextState,
    )
    initial.copy()
      .madd(k1, dt * (1.0 / 6.0))
      .madd(k2, dt * (2.0 / 6.0))
      .madd(k3, dt * (2.0 / 6.0))
      .madd(k4, dt * (1.0 / 6.0))
      .normalize()


trait SolverAlt extends Solver:
  /**
   * equal code:
   *
   * val w = state.velocity.angular
   * val q = state.transform.rotation
   * val I = inertia.getGlobalI(q)
   * val e = I.inverted() * (-w.cross(I * w))
   * val dq = 0.5 * Quaternion(0.0, w.x, w.y, w.z) * q
   * State3d(Transform3d(state.velocity.linear, dq), Velocity3d(force.linear / inertia.mass, e))
   *
   */
  protected def getDerivative(inertia: Inertia3d, force: Force3d)(state: State3d, t: Double): State3d =
    State3dDerivative(state, inertia, force)

  protected def nextState(state: State3d, derivative: State3d, dt: Double): State3d =
    state.copy().madd(derivative, dt).normalize()

  protected def newZeroDerivative(): State3d =
    val result = State3d()
    result.transform.rotation := (0.0, 0.0, 0.0, 0.0)
    result
