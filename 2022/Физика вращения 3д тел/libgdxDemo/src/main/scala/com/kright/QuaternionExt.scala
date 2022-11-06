package com.kright

import com.kright.math.{IQuaternion, Quaternion}

object QuaternionExt:
  extension (q: IQuaternion)
    def *(m: Double): Quaternion =
      Quaternion(
        q.w * m,
        q.x * m,
        q.y * m,
        q.z * m
      )
  
    def +(r: IQuaternion): Quaternion =
      Quaternion(
        q.w + r.w,
        q.x + r.x,
        q.y + r.y,
        q.z + r.z
      )

