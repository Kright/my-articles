package com.kright.benchmark

import com.github.kright.math.Vector3d
import com.github.kright.physics3d.{Force3d, Inertia3d, State3d}

import java.io.File
import java.nio.file.Paths

@main
def runPrecisionTest(): Unit =
  val body: Inertia3d = new Inertia3d(1.0, Vector3d(3.0, 2.0, 1.0))
  val initialState: State3d = new State3d()
  initialState.velocity.angular := (1.0, 1.0, 1.0)

  val csvDir = new File("../csv")
  csvDir.mkdirs()

  val solvers = List(
    SolverEulerNaive(),
    SolverEuler2(),
    SolverEuler2Alt(),
    SolverEuler2AltIterative(4),
    SolverRK2(),
    SolverRK2Alt(),
    SolverRK4(),
    SolverRK4Alt()
  )

  val totalTime = 100
  for(
    solver <- solvers;
    stepDt <- List(1.0, 0.1, 0.01, 0.001)
  ) {
    val solverName = solver.getClass.getSimpleName
    val stepsCount = (totalTime / stepDt).toInt

    val log = runBenchmark(body, initialState, stepDt, stepsCount, solver)
    println(s"solver = $solverName stepsDt = $stepDt errors = $log")
    log.saveCsv(Paths.get(csvDir.getPath, s"${solverName} ${stepDt}.csv"))
  }


private def runBenchmark(body: Inertia3d, initial: State3d, stepDt: Double, stepsCount: Int, solver: Solver): ErrorsLog =
  val log = new ErrorsLog(body, initial)

  val zeroForce = Force3d()
  val currentState = State3d() := initial

  for(_ <- 0 until stepsCount) {
    currentState := solver.getNextState(body, currentState, zeroForce, stepDt)
    log.update(currentState)
  }

  log
