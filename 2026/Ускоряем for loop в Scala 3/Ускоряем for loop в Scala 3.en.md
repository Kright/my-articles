---
title: "Speeding up the for loop in Scala 3"
author: Kright
date: 2026-06-27
---

In Scala the standard loop `for (i <- 0 until n)` looks concise, but there's an unexpected performance problem hiding behind it.

`0 until n` creates a `Range` object, which has a `foreach` method. But this method takes a lambda with the generic type `Int => Unit`, which means that instead of a number you get boxing and a `java.lang.Integer` object is passed. The JIT compiler usually copes with a single loop, but if the code has several nested loops, like in matrix multiplication, performance turns into a fiasco.

Luckily, Scala is a very flexible language and it has inline functions!

```Scala
object FastRange:
  inline def apply(endExclusive: Int) = new FastRange(endExclusive)
  inline def apply(inline zero: 0, endExclusive: Int) = new FastRange(endExclusive)
  inline def apply(start: Int, endExclusive: Int) = new FastRangeWithStart(start, endExclusive)

  extension (t: Int) inline infix def until(v: Int): FastRangeWithStart = FastRange(t, v)
  extension (inline zero: 0) inline infix def until(v: Int): FastRange = FastRange(0, v)

  extension (t: Int) inline infix def to(v: Int): FastRangeWithStart = FastRange(t, v + 1)
  extension (inline zero: 0) inline infix def to(v: Int): FastRange = FastRange(0, v + 1)


final class FastRange(val endExclusive: Int):
  inline def foreach(inline body: Int => Unit): Unit = {
    var i = 0
    while (i < endExclusive) {
      body(i)
      i += 1
    }
  }


final class FastRangeWithStart(val start: Int,
                               val endExclusive: Int):
  inline def foreach(inline body: Int => Unit): Unit = {
    var i = start
    while (i < endExclusive) {
      body(i)
      i += 1
    }
  }
```

## Shadowing

If you import `FastRange.*`, the compiler will pick its `to` and `until` methods instead of the standard ones from `Predef`, and the familiar `0 until n` turns into a `FastRange` and not the standard `Range`.

## Inline foreach

The key part is `inline def foreach(inline body: Int => Unit)`. The word `inline` before `body` means that the body of the lambda will be substituted right into the loop at compile time, without creating a lambda object and without boxing. The compiler generates exactly the same bytecode as a hand-written `while` loop.

There's one more nice bonus: from an `inline` lambda you can `return` from the enclosing function.

## Comparison with Kotlin

The Kotlin compiler has optimizations (lowerings) for exactly this case. The compiler looks for `for (i in 0..n)` patterns and directly generates bytecode identical to a plain `while`.

Scala has no built-in mechanism like that, but it does have metaprogramming, and I got almost the same thing without having to extend the compiler.


[Benchmark](https://github.com/Kright/mySmallProjects/tree/master/2026/scalaJMH)
```
[info] Matrix4x4Benchmark.multiply                    avgt    5  511.062 ±  99.651  ns/op
[info] Matrix4x4Benchmark.multiplyFastRange           avgt    5   17.991 ±   0.730  ns/op
```
The difference in speed - one and a half orders of magnitude!


The multiplication code:

```Scala
def multiply(a: Matrix4x4, b: Matrix4x4, result: Matrix4x4): Unit = {
  for (row <- 0 to 3) {
    for (column <- 0 to 3) {
      var sum = 0.0
      for (i <- 0 to 3) {
        sum += a(row, i) * b(i, column)
      }
      result(row, column) = sum
    }
  }
}


def multiplyFastRange(a: Matrix4x4, b: Matrix4x4, result: Matrix4x4): Unit = {
  import FastRange.*

  for (row <- 0 to 3) {
    for (column <- 0 to 3) {
      var sum = 0.0
      for (i <- 0 to 3) {
        sum += a(row, i) * b(i, column)
      }
      result(row, column) = sum
    }
  }
}
```
