---
title: "Calling native functions with Java FFI"
author: Kright
date: 2026-01-19
---
Java 22 got a stable Foreign Function & Memory API for calling functions written in C and other languages.

There's also Java JNI from the old days, FFI should be seen as an alternative to it. Looks like they're not planning to throw JNI out.

The short idea of how FFI works:

1. You need to find the function by name in a dynamic library and specify its signature, so it's clear how to call it from the JVM.
2. If you need to pass some arrays of data, allocate special chunks of memory that the JVM has no right to move, and copy the data there
3. Call the function.

Below is an example with code for multiplying 4x4 matrices, laid out as an array of 16 numbers:

The multiplication code in C:
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

Let's compile it into a library (and don't forget O2 or O3):
```Bash
gcc -shared -fPIC -o libcode.so code.c -O3
```

And now let's find the function in the library, specify the signature, allocate memory segments for the call and call the function. I did it in two different ways to compare performance.

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


## Important details

With FFI, when calling a function, we lay the data out in special buffers ourselves. The function works with them and knows nothing about the JVM. JNI had a different approach - you had to write a wrapper in C/C++, from which we could reach right into Java objects, but you had to sort of "lock" the objects with special calls, so the GC wouldn't move them around in memory.

It's tempting to call `System.load()` at class loading and put everything into static variables, but then there's a risk that an exception gets thrown and the JVM won't be able to load the class.

`MemorySegment.ofArray(array)` wraps an array, but you can't pass it to a C function. It's just a wrapper, to make it more convenient to call code like `aSegment.copyFrom(segmentFromArray)`

Arena is essentially like an arena allocator in C++. It can allocate little pieces of memory, but can't free them. The only thing you can do is call arena.close() and free all the pieces at once together with the arena. This is sometimes used in games, when they make a special arena for the objects of a game level, and on moving to the next level just wipe it entirely.

The JVM has several arenas:
* Arena.ofConfined() - the arena and its segments must be used from one and the same thread. The restriction is there for the sake of performance.
* Arena.ofShared() - can be used from different threads.
* Arena.ofAuto() - you can't close it, it "closes" by itself when the GC collects all the objects pointing to its memory segments.


## Performance.

```
[info] Benchmark                                      Mode  Cnt    Score    Error  Units
[info] Matrix4x4Benchmark.multiplyFastLoop            avgt   40   18.183 ±  0.417  ns/op
[info] Matrix4x4Benchmark.multiplyNative              avgt   40   27.397 ±  0.415  ns/op
[info] Matrix4x4Benchmark.multiplyNativeWithNewArena  avgt   40  104.631 ±  2.978  ns/op
[info] Matrix4x4Benchmark.multiply                    avgt   40  519.939 ± 13.276  ns/op
```

* The fastest way to multiply 4x4 matrices is with code in the JVM itself - 18 nanoseconds.
* If you allocate the arena and memory segments once, and then reuse them when calling the native function - the time is a bit more, 27 nanoseconds. That is, calling a C function is by itself cheap and fast.
* If you create a new arena and allocate new memory segments on every call, performance drops almost 4x, you get 105 nanoseconds - a lot, but maybe for long-running functions it's not critical.
* If you use inefficient iteration in Scala `for (row <- 0 to 3)`, JIT compilation won't manage to optimize the code and the multiplication will take a whole 520 ns.

So, at least in my particular example, it turns out that both the JVM and native code can run fast, if you write the code well. If you write it badly - in both cases you can get a slowdown of anywhere from 4x to 30x.


Also, just out of curiosity, I wrote a C function that does nothing:
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

That is, the overhead of a call is about 5 nanoseconds.

Full benchmark code [https://github.com/Kright/mySmallProjects/tree/master/2026/scalaJMH](https://github.com/Kright/mySmallProjects/tree/master/2026/scalaJMH)


