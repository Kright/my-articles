package com.kright.benchmark

import com.kright.physics3d.{Force3d, Inertia3d, State3d, State3dDerivative}
import com.kright.math.DifferentialSolvers

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

class SolverRK2 extends Solver:
  def getNextState(inertia: Inertia3d, initial: State3d, force: Force3d, dt: Double): State3d =
    DifferentialSolvers.rungeKutta2(initial, 0.0, dt,
      getDerivative = (s, t) => inertia.getDerivative(s, force),
      nextState = (s, d, dt) => s.updated(d, dt),
    )

class SolverRK4 extends Solver:
  def getNextState(inertia: Inertia3d, initial: State3d, force: Force3d, dt: Double): State3d =
    DifferentialSolvers.rungeKutta4(initial, 0.0, dt,
      getDerivative = (s, t) => inertia.getDerivative(s, force),
      nextState = (s, d, dt) => s.updated(d, dt),
      newZeroDerivative = () => new State3dDerivative(),
      madd = (acc, d, m) => acc.madd(d, m)
    )

