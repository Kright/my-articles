package com.kright

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.kright.math.IVector3d

extension (r: ShapeRenderer)
  def line(a: IVector3d, b: IVector3d): Unit =
    r.line(
      a.x.toFloat, a.y.toFloat, a.z.toFloat,
      b.x.toFloat, b.y.toFloat, b.z.toFloat
    )
