---
title: "ArrayView - a Scala library for multidimensional arrays"
author: Kright
date: 2025-06-17
---

[Code on github](https://github.com/Kright/ArrayView)

At first I wanted to write a class for the 2d case, but while improving the code I suddenly realized that it's easy to generalize to 3d and 4d, and you get a simple, efficient and universal library.

The design is inspired by numpy and by some of the features of Scala 3.

Below I'll explain why I made the choices I made.

# The key idea

So, straight to the point, a two-dimensional array will look roughly like this:

```Scala
trait ArrayView2d[T]:
  val data: Array[T]

  def shape0: Int
  def shape1: Int

  def offset: Int
  def stride0: Int
  def stride1: Int

  def getIndex(i0: Int, i1: Int): Int =
    offset + i0 * stride0 + i1 * stride1

  inline def apply(i0: Int, i1: Int): T =
    data(getIndex(i0, i1))

  inline def update(i0: Int, i1: Int, value: T): Unit =
    data(getIndex(i0, i1)) = value
```

I.e., I store the sizes along the axes, the position of the "zero" element relative to the start of the array, and how much the position in the array changes when one index or another is incremented.

I picked this idea up from numpy, and it's a very interesting way to make all sorts of "representations" of the same data.

1. Take a part of the array: adjust `offset`, `shape0` and `shape1`
2. Transpose the array: swap `stride0` and `stride1`, and also `shape0` and `shape1`
3. Skip the elements with odd indices - multiply `stride` by 2, divide the size by 2.
4. Reverse the order - multiply `stride` by -1, adjust `offset`.
5. Add a third axis of size one - create a 3d `view`, set `shape2 = 1` for the third axis
6. Broadcast an axis of size one to any size: zero out `stride`, set any `shape`.

The reshape operation has its nuances, but in some cases you can reuse the same data array in views with a different number of dimensions. For example, (48), (3, 16) and (3, 4, 4)

I.e., a whole bunch of operations for manipulating multidimensional data fit this model beautifully, and the data itself almost never has to be copied or moved around.

# Why this works efficiently.

If we make an `ArrayView[Int]` in Scala, then unlike Kotlin and Java, the Scala compiler will put a normal array of ints into data, not an array of Integer objects. This wonderful fact is what pushed me to write the library - you can use `ArrayView2d[Int]`, `ArrayView2d[Double]`, `ArrayView2d[SomeType]`, and all of it will be stored as efficiently as possible.

I really wanted to make temporary objects for two-dimensional indices and, instead of hardcoding shape0 and shape1, just store `shape = Shape2(i0, i1)`, but the JVM still has no value types, and creating temporary objects on every array access can hurt performance.

And one more reason why that's bad - a typical processor has a cache line size of 64 bytes. The size of our fields - the data pointer at 4 or 8 bytes and five ints at 4 bytes each - 24 or 28 bytes in total, plus an object header of 12-16 bytes. I.e., such an object fits nicely into a single processor cache line, and access to the object's fields will be as fast as it gets. If I suddenly make a separate Shape2 class instead, then just to get a size you'd have to follow the shape reference and pull the number out of there - that's definitely less efficient than just grabbing an int field.

Why I use a one-dimensional data array and not an array of arrays or something else - again, accessing an element inside an array of arrays takes two jumps in memory - that's slower. And it'd also be a lot of objects in memory instead of one.

The finishing touch - the apply and update methods, which are used to access elements, are marked as inline.

The catch is that if there's an `ArrayView[T]` and we call a method with return type `T`, that type will be an object. And we get boxing of primitive types out of nowhere.
And this is where the inline modifier saves us - for `ArrayView[Double]` the compiler knows that data is exactly an array of doubles, and the bytecode will have a plain array access, something like this:

```Scala
val element: Double = arrayView2d.data(arrayView2d.getIndex(i0, i1))
```

Maybe in some cases escape analysis and JVM-level inlining will kick in, and java will also put the Shape object next to the ArrayView and it'll work fast - but instead I decided to go with a design that's as simple and as close to the hardware as possible.

I measured the performance in JMH - it's close to the speed of code with the indices substituted by hand. And faster than an array of arrays. I'd call that a success!

# transparent inline and context functions

If you remember, in numpy the `arr[...]` method is very flexible and can take anything. For example `arr[0, 1:-1, ::-1]` - here we take a fixed first index, and leave the second and third axes as axes, we just drop the edge values along the second axis and reverse the order along the third one. So the dimensionality of the result depends on the types of the arguments - each can be either a number or a range.

And there's a tricky bit - a negative index means an offset "from the end" of the array. But if you happen to make a mistake and code like `arr[i:j]` accidentally ends up with negative `i` and `j` - good luck debugging.

In Scala you can do something even cooler with `transparent inline`:

```Scala
  transparent inline def view [T1 <: Int | Range,
                               T2 <: Int | Range](inline range0: AxisSize.Size ?=> T1,
                                                  inline range1: AxisSize.Size ?=> T2) = {
    val t0 = AxisSize.withAxisSize(shape0, range0)
    val t1 = AxisSize.withAxisSize(shape1, range1)

    val start0 = ArrayViewUtil.getFirst(t0, shape0)
    val start1 = ArrayViewUtil.getFirst(t1, shape1)

    val offset = getIndex(start0, start1)

    inline (t0, t1) match {
      case (a: Int, b: Int) => ArrayView0dImpl(data, offset = offset)
      case (a: Range, b: Int) => ArrayView1dImpl(data, shape0 = a.size, offset = offset, stride0 = stride0 * a.step)
      case (a: Int, b: Range) => ArrayView1dImpl(data, shape0 = b.size, offset = offset, stride0 = stride1 * b.step)
      case (a: Range, b: Range) => ArrayView2dImpl(data, shape0 = a.size, shape1 = b.size, offset = offset, stride0 = stride0 * a.step, stride1 = stride1 * b.step)
    }
  }
```

So, each argument also comes with a context, and that context carries size - the size along the axis.

Now that numpy example can be written like this:

```Scala
val arrayView2d = arrayView3d.view(0, 1 until (size - 1), all.reversed)
```

Overall my approach isn't perfect, but it's the most convenient thing I could come up with.

For 3d and 4d it's all the same, if you don't look at the combinatorial explosion up to 16 variants for 4d. But that happens at compile time, the bytecode will only contain the code that's needed.

# Wrapping up

I didn't add matrix operations like arithmetic to the library, because that's a specific scenario for arrays of numbers.

Instead I focused on getting a simple and efficient library for working with multidimensional data of any type.

The library is fast, simple, has no dependencies and is written in pure Scala (supports scala js). [Use it and enjoy](https://github.com/Kright/ArrayView)
