package com.kright.benchmark

import com.github.kright.math.*
import com.github.kright.physics3d.*

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.collection.mutable.ArrayBuffer

class ErrorsLog(val body: Inertia3d, initialEnergy: Double, initialL: IVector3d):
  def this(body: Inertia3d, state3d: State3d) = this(body, body.getEnergy(state3d), body.getL(state3d))

  private val errorsERelative = new ArrayBuffer[Double]()
  private val errorsLRelative = new ArrayBuffer[Double]()
  
  def update(state: State3d): Unit =
    update(body.getEnergy(state), body.getL(state))

  def update(e: Double, l: IVector3d): Unit =
    errorsERelative += (e - initialEnergy) / initialEnergy
    errorsLRelative += l.distance(initialL) / initialL.mag
  
  def update(energy: Double, magL: Double): Unit =
    errorsERelative += (energy - initialEnergy) / initialEnergy
    errorsLRelative += math.abs(initialL.mag - magL) / initialL.mag  

  override def toString: String =
    val eMax = errorsERelative.max
    val eMin = errorsERelative.min
    val errE = if (eMax > -eMin) eMax else eMin
    val errL = errorsLRelative.max
    s"Error(energy = ${errE}, L = ${errL})"

  def saveCsv(path: Path): Unit =
    val text = errorsERelative.zip(errorsLRelative)
      .map((de, dl) => s"${de},${dl}")
      .mkString("\n")

    Files.write(path, text.getBytes(StandardCharsets.UTF_8))
