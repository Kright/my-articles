package com.kright

import com.badlogic.gdx.math.Matrix4
import com.kright.math.{Matrix4d, Quaternion}

extension (matrix: Matrix4)
  def :=(m: com.kright.math.Matrix4d): Matrix4 =
    for(i <- matrix.`val`.indices) {
      matrix.`val`(i) = m.elements(i).toFloat
    }
    matrix.tra() // transpose, my math work in different way
    matrix

  def :=(q: Quaternion): Matrix4 =
    val m = Matrix4d() := q
    matrix := m
