---
title: "Fused multiply-add"
author: Kright
date: 2026-07-25
lang: en
ref: fma
---

At first glance, it's just addition and multiplication glued together into a single operation.

```scala
fma(a, b, c) = a * b + c
```

But the most important trick is in the precision of the calculation. The result of that intermediate multiplication is computed at twice the precision (for double that's a mantissa of over a hundred bits, no less), then c is added, and only at the very end the result gets rounded, once.

For example, there's the double-double arithmetic approach, where we store a pair of numbers (high, low): the high one holds the number and the low one holds the correction. FMA lets you do this in a couple of steps:

```scala
val high = a * b
val low = fma(a, b, -high)
```

high holds the usual rounded result of the multiplication, and the fma computes the difference between the exact result of the multiplication and the rounded one.

Note: this trick may not work on denormal numbers and on overflow.

Before fma, people used Dekker's product, which is built on Veltkamp splitting. There's no Wikipedia article about it, but I did find Dekker's [original paper](https://gdz.sub.uni-goettingen.de/id/PPN362160546_0018?tify=%7B%22pages%22%3A%5B230%5D%2C%22pan%22%3A%7B%22x%22%3A0.401%2C%22y%22%3A0.767%7D%2C%22view%22%3A%22info%22%2C%22zoom%22%3A0.406%7D) itself, you can take a look and enjoy how much simpler everything is with FMA and how many fewer operations on numbers you have to do.

Another example: we want to compute `a * b - c * d`. We lose precision, because we're subtracting two large numbers from each other.

You can use something similar to the [Kahan summation algorithm](https://en.wikipedia.org/wiki/Kahan_summation_algorithm):

```scala
def diffOfProducts(a: Double, b: Double, c: Double, d: Double): Double =
  val cd  = c * d
  val err = fma(c, d, -cd)   // rounding error of cd
  val dop = fma(a, b, -cd)
  dop - err
```

All in all - FMA is used in algorithms like polynomial evaluation, matrix multiplication and so on.

On the downside - replacing a * b + c with fma changes the behavior. For example, the code `sqrt(x * x - y * y)` with fma and x=y can suddenly return NaN because of the square root of a negative number. Because of this, the JIT compiler in Java isn't allowed to substitute fma on its own.


### Historical note

The IEEE 754-2008 standard makes the operation mandatory.

All modern processors and architectures (including ARM and RISC-V) have FMA. But, oddly enough, on desktops it showed up relatively recently - AMD added it in 2011 (as FMA4, and FMA3, the one compatible with Intel's, in 2012) and Intel in 2013. Though you'd think - it was already there in IBM POWER1 back in 1990 and in Intel Itanium in 2001.

On GPUs the designers went and reached full enlightenment: they churned out a bunch of fma blocks, and both multiplication and addition go through them as `fma(a, b, 0.0)` and `fma(a, 1.0, b)`. While they're at it, they count one fma block as doing two floating point operations at a time, and paint pretty FLOPS in their marketing reports.
