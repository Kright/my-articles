package com.kright.benchmark

import com.kright.QuaternionExt.*
import com.kright.math.{DifferentialSolvers, IQuaternion, Quaternion}
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
      madd
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
    val result = DifferentialSolvers.rungeKutta4[State3d, State3d](initial, 0.0, dt,
      getDerivative(inertia, force),
      nextState,
      newZeroDerivative,
      madd
    )
    result.transform.rotation.normalize()
    result


trait SolverAlt extends Solver:
  protected def getDerivative(inertia: Inertia3d, force: Force3d)(state: State3d, t: Double): State3d =
    val w = state.velocity.angular
    val q = state.transform.rotation
    val I = inertia.getGlobalI(q)
    val e = I.inverted() * (-w.cross(I * w))
    val dq = Quaternion(0.0, w.x, w.y, w.z) * q * 0.5
    State3d(
      Transform3d(state.velocity.linear, dq),
      Velocity3d(force.linear / inertia.mass, e)
    )

  protected def nextState(state: State3d, derivative: State3d, dt: Double): State3d =
    val nextState = copy(state)
    madd(nextState, derivative, dt)
    nextState.transform.rotation.normalize()
    nextState

  protected def copy(state: State3d): State3d =
    State3d(state.transform.copy(), state.velocity.copy())

  protected def madd(state: State3d, add: State3d, weight: Double): State3d =
    state.transform.position.madd(add.transform.position, weight)
    state.transform.rotation := state.transform.rotation + add.transform.rotation * weight
    state.velocity.madd(add.velocity, weight)
    state

  protected def newZeroDerivative(): State3d =
    val result = State3d()
    result.transform.rotation := (0.0, 0.0, 0.0, 0.0)
    result
