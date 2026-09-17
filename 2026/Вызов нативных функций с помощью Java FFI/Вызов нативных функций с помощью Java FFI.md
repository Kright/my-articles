---
title: "Вызов нативных функций с помощью Java FFI"
author: Kright
date: 2026-01-19
---
В Java 22 появился стабильный Foreign function interface для вызова функций на Си и прочих языках.

Со старых времён есть ещё Java JNI, на FFI надо смотреть как на альтернативу. Вроде бы JNI выкидывать не планируют.

Краткая идея, как работает FFI:

1. Надо в динамической библиотеке найти функцию по имени и указать сигнатуру, чтобы было понятно как из JVM её вызывать.
2. Если надо передавать какие-нибудь массивы данных, выделить специальные участки памяти, которые JVM не имеет право двигать, и скопировать данные туда
3. Вызывать функцию.

Ниже пример с кодом для перемножения матриц 4x4, уложенных как массив из 16 чисел:

Код для умножения на Си:
```C
void matrix4x4_multiply(const double * restrict a, const double * restrict b, double * restrict result) {
    for (int row = 0; row < 4; row++) {
        for (int column = 0; column < 4; column++) {
            double sum = 0.0;
            for (int i = 0; i < 4; i++) {
                sum += a[row * 4 + i] * b[i * 4 + column];
            }
            result[row * 4 + column] = sum;
        }
    }
}
```

Скомпилируем его в библиотеку (и не забыть O2 или O3):
```Bash
gcc -shared -fPIC -o libcode.so code.c -O3
```

А теперь найдём функцию в библиотеке, укажем сигнатуру, выделим сегменты памяти для вызова и вызовем функцию. Я сделал двумя разными способами для сравнения производительности.

```Scala
import java.lang.foreign.{Arena, FunctionDescriptor, Linker, MemorySegment, SymbolLookup, ValueLayout}
import java.lang.invoke.MethodHandle


class Matrix4x4 {
  val data: Array[Double] = Array.ofDim[Double](16)
}


class NativeMultiplier {
  System.load(java.io.File("libcode.so").getAbsolutePath)

  val arena: Arena = Arena.ofConfined()
  val aSegment = arena.allocate(ValueLayout.JAVA_DOUBLE, 16L)
  val bSegment = arena.allocate(ValueLayout.JAVA_DOUBLE, 16L)
  val resultSegment = arena.allocate(ValueLayout.JAVA_DOUBLE, 16L)

  val linker = Linker.nativeLinker()
  val lookup = SymbolLookup.loaderLookup()
  val symbolOpt = lookup.find("matrix4x4_multiply").get()

  val matrixMultiplyHandle: MethodHandle = linker.downcallHandle(
    symbolOpt,
    FunctionDescriptor.ofVoid(
      ValueLayout.ADDRESS,
      ValueLayout.ADDRESS,
      ValueLayout.ADDRESS
    )
  )

  def multiply(a: Matrix4x4, b: Matrix4x4, result: Matrix4x4): Unit = {
    aSegment.copyFrom(MemorySegment.ofArray(a.data))
    bSegment.copyFrom(MemorySegment.ofArray(b.data))

    matrixMultiplyHandle.invoke(aSegment, bSegment, resultSegment)

    MemorySegment.ofArray(result.data).copyFrom(resultSegment)
  }

  def multiplyWithNewArea(a: Matrix4x4, b: Matrix4x4, result: Matrix4x4): Unit = {
    val newArena = Arena.ofConfined()

    try {
      val aSegment = newArena.allocate(ValueLayout.JAVA_DOUBLE, 16L)
      val bSegment = newArena.allocate(ValueLayout.JAVA_DOUBLE, 16L)
      val resultSegment = newArena.allocate(ValueLayout.JAVA_DOUBLE, 16L)

      aSegment.copyFrom(MemorySegment.ofArray(a.data))
      bSegment.copyFrom(MemorySegment.ofArray(b.data))

      matrixMultiplyHandle.invoke(aSegment, bSegment, resultSegment)

      MemorySegment.ofArray(result.data).copyFrom(resultSegment)
    }
    finally {
      newArena.close()
    }
  }
}
```


## Важные детали

В FFI при вызове функции, мы сами укладываем данные в специальные буферы. Функция взаимодействует с ними и ничего не знает о JVM. В JNI был другой подход - надо было писать обёртку на C/C++, из которой мы могли залазить прямо в Java объекты, но надо было специальными вызовами как бы "лочить" объекты, чтобы GC их не двигал в памяти.

Есть соблазн вызывать `System.load()` при загрузке класса и положить всё в статические переменные, но тогда есть риск что вылетит исключение и JVM не сможет загрузить класс.

`MemorySegment.ofArray(array)` оборачивает массив, но в функцию на Си его передавать нельзя. Это просто обёртка, чтобы было удобнее вызывать код типа `aSegment.copyFrom(segmentFromArray)`

Arena это по сути как arena allocator в C++. Она умеет выделять кусочки памяти, но не умеет освобождать. Единственное что можно - вызвать arena.close() и освободить сразу все кусочки вместе с ареной. Такое иногда используют в играх, когда делают специальную арену для объектов игрового уровня, и при переходе на следующий игровой уровень просто очищают её целиком.

В JVM есть несколько арен:
* Arena.ofConfined() - арена и сегменты должны использоваться из одного и того же потока. Ограничение сделано ради производительности.
* Arena.ofShared() - можно использовать из разных потоков.
* Arena.ofAuto() - закрывать нельзя, она сама "закроется", когда GC соберёт все объекты, указывающие на её сегменты памяти.


## Производительность.

```
[info] Benchmark                                      Mode  Cnt    Score    Error  Units
[info] Matrix4x4Benchmark.multiplyFastLoop            avgt   40   18.183 ±  0.417  ns/op
[info] Matrix4x4Benchmark.multiplyNative              avgt   40   27.397 ±  0.415  ns/op
[info] Matrix4x4Benchmark.multiplyNativeWithNewArena  avgt   40  104.631 ±  2.978  ns/op
[info] Matrix4x4Benchmark.multiply                    avgt   40  519.939 ± 13.276  ns/op
```

* Быстрее всего матрицы 4x4 перемножать кодом в самой jvm - 18 наносекунд.
* Если один раз выделить арену и сегменты памяти, и потом переиспользовать при вызовах нативной функции - время немного больше, 27 наносекунд. То есть, сам по себе вызов функции из Си недорогой и быстрый.
* Если при каждом вызове создавать новую арену и выделять новые сегменты памяти, производительность падает почти в 4 раза, получается 105 наносекунд - много, но возможно для долго работающих функций не критично.
* Если использовать неэффективную итерацию в Scala `for (row <- 0 to 3)`, то JIT компиляция не справится с оптимизацией кода и перемножение займёт аж 520 нс.

В общем, конкретно на моём примере получается, что и JVM и нативный код могут работать быстро, если написать код хорошо. Если написать плохо - и там и там можно получить замедление от 4 до 30 раз.


Ещё я ради интереса написал функцию на Си, которая ничего не делает:
```C
double getDoubleZero() {
    return 0.0;
}
```

```
[info] Benchmark                                      Mode  Cnt    Score    Error  Units
[info] Matrix4x4Benchmark.getZero                     avgt   40    0.455 ±  0.018  ns/op
[info] Matrix4x4Benchmark.getZeroNative               avgt   40    5.369 ±  0.109  ns/op
```

То есть, накладные расходы на вызов порядка 5 наносекунд.

Полный код бенчмарка [https://github.com/Kright/mySmallProjects/tree/master/2026/scalaJMH](https://github.com/Kright/mySmallProjects/tree/master/2026/scalaJMH)


