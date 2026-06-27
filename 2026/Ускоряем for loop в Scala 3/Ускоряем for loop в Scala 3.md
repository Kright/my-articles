---
title: "Ускоряем for loop в Scala 3"
author: Kright
date: 2026-06-27
---

В Scala стандартный цикл `for (i <- 0 until n)` выглядит лаконично, но за ним скрывается неожиданная проблема с производительностью.

`0 until n` создаёт объект `Range`, у которого есть метод `foreach`. Но этот метод принимает лямбду с обобщённым типом `Int => Unit`, а значит вместо числа случится боксинг и передадут объект `java.lang.Integer`. JIT-компилятор обычно справляется для одного цикла, но если в коде несколько вложенныз циклов, как при перемножении матриц, то случается фиаско с производительностью.

К счастью, Scala очень гибкий язык и в ней есть inline функции!

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

Если импортировать `FastRange.*`, компилятор выберет его методы `to` и `until` вместо стандартных из `Predef`, и привычный `0 until n` превратится в `FastRange`, а не в стандартный `Range`.

## Inline foreach

Ключевое место — `inline def foreach(inline body: Int => Unit)`. Слово `inline` перед `body` означает, что тело лямбды будет подставлено прямо внутрь цикла во время компиляции, без создания объекта-лямбды и без боксинга. Компилятор генерирует ровно тот же байткод, что и ручной `while`-цикл.

Есть ещё один приятный бонус: из `inline`-лямбды можно делать `return` из внешней функции.

## Сравнение с Kotlin

В компиляторе Kotlin есть оптимизации (lowerings) именно для этого случая. Компилятор ищет паттерны `for (i in 0..n)` и генерируют напрямую байткод, идентичный натуральному `while`.

В Scala такого встроенного механизма нет, но зато есть метапрограммирование и я добился почти того же самого без дописывания компилятора.


[Замер производительности](https://github.com/Kright/mySmallProjects/tree/master/2026/scalaJMH)
```
[info] Matrix4x4Benchmark.multiply                    avgt    5  511.062 ±  99.651  ns/op
[info] Matrix4x4Benchmark.multiplyFastRange           avgt    5   17.991 ±   0.730  ns/op
```
Разница в скорости - в полтора порядка!


Код умножения:

```
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
