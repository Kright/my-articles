package com.kright


import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.{Matrix4, Vector3}
import com.kright.math.{IVector3d, Matrix4d, Quaternion, Vector3d}

extension (matrix: Matrix4)
  def :=(m: com.kright.math.Matrix4d): Matrix4 =
    for (i <- matrix.`val`.indices) {
      matrix.`val`(i) = m.elements(i).toFloat
    }
    matrix.tra() // transpose, my math work in different way
    matrix

  def :=(q: Quaternion): Matrix4 =
    val m = Matrix4d() := q
    matrix := m


extension (v: Vector3d)
  def toVector3(): Vector3 =
    Vector3(v.x.toFloat, v.y.toFloat, v.z.toFloat)


extension (r: ShapeRenderer)
  def line(a: IVector3d, b: IVector3d): Unit =
    r.line(
      a.x.toFloat, a.y.toFloat, a.z.toFloat,
      b.x.toFloat, b.y.toFloat, b.z.toFloat
    )
