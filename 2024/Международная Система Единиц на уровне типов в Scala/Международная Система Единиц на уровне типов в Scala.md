Текст больше про написание кода на Scala, чем про физику.

В физике есть понятие размерности. Например, килограммы нет смысла складывать с метрами или с секундами.

Если писать физические формулы и где-то ошибаться, то потом можно проверить размерности и найти часть ошибок.

Но при написании кода мы обычно возвращаемся в каменный век - скорее всего, там будут просто числа с плавающей точкой. Любые числа можно произвольно складывать, и не важно что в одной перемеенной были метры, а в другой - дюймы. Вся надежда на сознательность программиста, который может ошибаться.

Дальше я напишу, как можно Международную Систему Измерений (СИ) описать в системе типов в Scala 3.

В СИ есть базовые единицы измерения - метры, килограммы, секунды, градусы Кельвина, Амперы и т.п.
Их перемножением и делением можно получить различные полезные сочетания. Например, скорость это метры делённые на секунды, а плотность - килограммы, делённые на кубические метры.

В принципе, если завести специальный тип "Метры" и все расстояния измерять в них, то проблему со сложением дюймов с сантиметрами можно будет избежать. Но таких типов будет много - длина, площадь, объём, скорость, ускорение, работа, мощность, вязкость ... и писать руками каждый тип не хочется.

В примерах кода ограничусь тремя единицами - метрами, килограммами и секундами, остальное можно добавить по аналогии.

Ещё обращу внимание, что степени размерностей целые - например, метры могут быть в квадратными, кубическими, но не в степени 1.34.

```Scala
object SI:
  opaque type Value[M <: Int, KG <: Int, S <: Int] = Double

  type Distance = SI.Value[1, 0, 0]
  type Mass = SI.Value[0, 1, 0]
  type Time = SI.Value[0, 0, 1]
```

Тип обозначен как непрозрачный (Opaque). С точки зрения байткода это будет просто Double, с точки зрения системы типов - это другой тип, никак не связанный с Double. Единственный мостик между ними - внутри объекта SI, где мы знаем, что Value[...] это просто Double и можем присваивать одно в другое.

Степень каждой единицы измерения - целое число. Дистанция, масса и время - наши базовые типы.

Сделаем конструкторы для типов:
```Scala
object SI:
  ...
  def zero[M <: Int, KG <: Int, S <: Int]: Value[M, KG, S] = 0.0

  extension (v: Double)
    def km: Distance = v * 1000.0
    def m: Distance = v
    def mm: Distance = v * 0.001

    def kg: Mass = v
    def g: Mass = v * 0.001

    def second: Time = v
    def minute: Time = v * 60.0
    def ms: Time = v * 0.001

  extension (v: Distance)
    def asKm: Double = v * 0.001
    def asM: Double = v
    ...
```

Итак, теперь мы можем создавать из Double наши типы и конвертировать их обратно в Double
```Scala
import SI.*

val t: Time = 2.0.minute
val s: Distance = 123.0.mm
val sInMeters: Double = s.asM
```

В принципе вместо extension методов это могли бы быть просто функции, типа `minute(2.0).asSeconds`

Уже сейчас мы можем конвертировать метры в миллиметры. Возможно, не так впечатляет, как конвертировать между собой дюймы, мили и футы с ярдами, но там принцип точно такой же. Для времени и массы код пишется аналогично.

Теперь добавим сложение для переменных одной размерности

```Scala
object SI:
  ...
  extension [M <: Int, KG <: Int, S <: Int](v: Value[M, KG, S])
    def +(v2: Value[M, KG, S]): Value[M, KG, S] = v + v2
```

Ура, теперь метры можно складывать с метрами, а секунды с секундами, и перепутать не получится.

Теперь добавим умножение. При умножении степени, как известно, складываются, при делении вычитаются.

```
object SI:
  ...
  import scala.compiletime.ops.int.*

  extension [M <: Int, KG <: Int, S <: Int](v: Value[M, KG, S])
    def *[M2 <: Int, KG2 <: Int, S2 <: Int](v2: Value[M2, KG2, S2]): Value[M + M2, KG + KG2, S + S2] = v * v2
    def /[M2 <: Int, KG2 <: Int, S2 <: Int](v2: Value[M2, KG2, S2]): Value[M - M2, KG - KG2, S - S2] = v / v2
```

M, KG и т.п. - всё целые числа, и мы из compiletime для них импортируем операции сложения, вычитания и т.п.
Вся эта машинерия со сложением чисел и типами вида Value[a, b, c] будет на этапе компиляции, в рантайме останется только арифметическая операция с двумя Double.

В некоторых языках можно сделать числа Чёрча типа `Zero, Next[Zero], Next[Next[Zero]]` и потом развлекаться с ними, но к счастью у нас всё удобно и параметром дженерика может быть обычный Int.

Сделаем операцию возведения в целую степень
```
import scala.compiletime.constValue

object SI:
  ...
  extension [M <: Int, KG <: Int, S <: Int](v: Value[M, KG, S])
    inline def pow[T <: Int]: Value[M * T, KG * T, S * T] =
      Math.pow(v, constValue[T])
```

Поскольку T это Int, constValue[T] превратит тип в значение. Например, constValue[42] = 42.
Функция inline, чтобы было доступно constValue[T] - для каждого вызова оно может быть своё.

Теперь кубические метры можно записать так:
```
val cubeMeters = 1.0.m.pow[3]
```

Операцию нахождения корня их кубических метров предлагаю попробовать сделать самостоятельно.


Для отладки могут помочь функции превращения в строку
```Scala
object SI:
  ...
  extension [M <: Int, KG <: Int, S <: Int](v: Value[M, KG, S])
    inline def typeStr: String =
      s"m^${constValue[M]}*kg^${constValue[KG]}*s^${constValue[S]}"

    inline def toStr: String =
      s"$v $typeStr"
```

Вот так относительно лаконично мы объяснили компилятору правила арифметических операции с размерными величинами.

Для удобства, само собой, можно дать красивые названия часто используемым типам:
```Scala
object SI:
  ...
  type Velocity = Value[1, 0, -1]
  type Acceleration = Value[1, 0, -2]
```

Но сами типы тоже хочется как-то более красиво записать, попробуем напоследок сделать и это.

В этот раз типы надо объявить снаружи от объекта SI. Потому что внутри объекта SI тип Value[_, _, _] и Double это одно и то же, и компилятор будет ругаться на паттерн матчинг по типам.

Я им некрасивые имена (не +, *, и т.п.), чтобы не пересекались с теми что импортируются из `scala.compiletime.ops.int`


```Scala
type SiDiv[V1, V2] =
  (V1, V2) match {
    case (SI.Value[m, kg, s], SI.Value[m2, kg2, s2]) => SI.Value[m - m2, kg - kg2, s - s2]
  }

type SiMul[V1, V2] =
  (V1, V2) match {
    case (SI.Value[m, kg, s], SI.Value[m2, kg2, s2]) => SI.Value[m + m2, kg + kg2, s + s2]
  }
```

Теперь можно писать так:
```Scala
  type Velocity = SiDiv[SI.Distance, SI.Time]
  type Acceleration = SiDiv[SI.Velocity, SiMul[SI.Time, SI.Time]]
```

В принице, наверно можно и красивые имена им дать, тогда читаемость будет чуть повыше. Но это больше вопрос наименований, суть не поменяется.

```Scala
  type Velocity = Distance / Time
  type Acceleration = Distance / (Time * Time)
```



Весь код:
```Scala
import scala.compiletime.constValue
import scala.compiletime.ops.int.*

type SiDiv[V1, V2] =
  (V1, V2) match {
    case (SI.Value[m, kg, s], SI.Value[m2, kg2, s2]) => SI.Value[m - m2, kg - kg2, s - s2]
  }

type SiMul[V1, V2] =
  (V1, V2) match {
    case (SI.Value[m, kg, s], SI.Value[m2, kg2, s2]) => SI.Value[m + m2, kg + kg2, s + s2]
  }

object SI:
  opaque type Value[M <: Int, KG <: Int, S <: Int] = Double

  type Distance = Value[1, 0, 0]
  type Mass = Value[0, 1, 0]
  type Time = Value[0, 0, 1]

  type Velocity = SiDiv[SI.Distance, SI.Time]
  type Acceleration = SiDiv[SI.Velocity, SiMul[SI.Time, SI.Time]]
  type Area = SiMul[SI.Distance, SI.Distance]

  def zero[M <: Int, KG <: Int, S <: Int]: Value[M, KG, S] =
    0.0

  extension (v: Double)
    def km: Distance = v * 1000.0
    def m: Distance = v
    def cm: Distance = v * 0.01
    def mm: Distance = v * 0.001

    def kg: Mass = v
    def g: Mass = v * 0.001

    def second: Time = v
    def minute: Time = v * 60.0
    def ms: Time = v * 0.001

  extension (v: Velocity)
    def kmh: Double = v * 3.6

  extension (v: Distance)
    def asKm: Double = v * 0.001
    def asM: Double = v
    def asCm: Double = v * 100.0
    def asMm: Double = v * 1000.0

  extension [M <: Int, KG <: Int, S <: Int](v: Value[M, KG, S])
    def +(v2: Value[M, KG, S]): Value[M, KG, S] =
      v + v2

    def square: Value[M * 2, KG * 2, S * 2] =
      v * v

    inline def pow[T <: Int]: Value[M * T, KG * T, S * T] =
      Math.pow(v, constValue[T])

    inline def typeStr: String =
      s"m^${constValue[M]}*kg^${constValue[KG]}*s^${constValue[S]}"

    inline def toStr: String =
      s"${v} ${typeStr}"

  extension [M <: Int, KG <: Int, S <: Int](v: Value[M, KG, S])
    def *[M2 <: Int, KG2 <: Int, S2 <: Int](v2: Value[M2, KG2, S2]): Value[M + M2, KG + KG2, S + S2] = v * v2
    def /[M2 <: Int, KG2 <: Int, S2 <: Int](v2: Value[M2, KG2, S2]): Value[M - M2, KG - KG2, S - S2] = v / v2

```

И пример использования
```Scala

import SI.*

@main
def main(): Unit = {
  val t = 10.minute
  val s = 10.km

  val velocity = s / t
  println(velocity.toStr)

  val acceleration = 9.8.m / 1.second.square
  println(acceleration.toStr)

  println(1.0.m.pow[3].toStr)
}
```

На момент написания кода использовалась Scala 3.5.2







