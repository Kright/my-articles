---
title: "Scala 3, Monocle, иерархия неизменяемых объектов"
author: Kright
date: 2025-08-09
---

TL;DR: Есть библиотека Monocle ([документация](https://www.optics.dev/Monocle/docs/optics), [гитхаб](https://github.com/optics-dev/Monocle)) и её помощью удобно делать копии сложных неизменяемых case-классов.

Если классы изменяемые, то можно и без библиотеки написать так:
```Scala
car.body.mass += 1.0
println(car)
```
И проблем нет, даже если много объектов вложено по цепочке.
Если Вас такой вариант устраивает, то дальше можно не читать.

Если же объекты неизменяемые, то можно использовать метод copy(), но для вложенных неизменяемых объектов получается страшный код, в котором легко ошибиться:
```Scala
val newCar = car.copy(body = car.body.copy(mass = car.body.mass + 1.0))
println(newCar)
```

С помощью Monocle всё снова становится простым:
```Scala
import monocle.syntax.all.*

val newCar = car.focus(_.body.mass).modify(_ + 1.0)
println(newCar)
```

# Подробности

Допустим, есть какие-то неизменяемые классы для описания автомобиля с каким-то размером, массой и произвольным количеством пар колёс.

```Scala
case class AxleSpec(track: Double, canTurn: Boolean, hasDrive: Boolean)

case class BodySpec(width: Double, height: Double, length: Double, mass: Double)

case class CarSpec(body: BodySpec, axles: ArraySeq[AxleSpec])

val car = CarSpec(
  body = BodySpec(width = 1.735, height = 1.23, length = 3.915, mass = 1030),
  axles = ArraySeq(
    AxleSpec(track = 1.495, canTurn = true, hasDrive = false),
    AxleSpec(track = 1.505, canTurn = false, hasDrive = true),
  ),
)
```

Метод focus - это макрос, который возвращает "линзу", "сфокусированную" на поле `mass`. Методы типа `modify(_ + 1.0)`, `get` и `replace(1000)` будут работать с ним.
Модифицирующие операции будут возвращать копию `car` с копией `body` с новым значением `mass`.

Сейчас IntelliJ IDEA с автодополнением не справляется, но можно указать типы руками. И в примерах я явно пишу типы, чтобы было более понятно что там происходит.

```
import monocle.AppliedLens
import monocle.syntax.all.*

val a: AppliedLens[CarSpec, Double] = car.focus(_.body.mass)
println(a.get)
println(a.replace(1000)) // напечатает новый CarSpec с другой массой
```

AppliedLens уже привязана к конкретному объекту.

Так же можно сделать "не привязанную" никуда линзу и применять её потом:

```Scala
import monocle.{Lens, Focus}

val carMassLens: Lens[CarSpec, Double] = Focus[CarSpec](_.body.mass)
val increaseCarMass: CarSpec => CarSpec = carMassLens.modify(_ + 1.0)

println(carMassLens.get(car))
println(increaseCarMass(car))
```

Я не знаю, почему в стандартной имплементации поддерживаются только классы List и Vector, но можно дописать код для любой коллекции и иметь доступ по индексу. Я добавил к immutable.ArraySeq:

```Scala
import monocle.function.Index
import monocle.Optional
import scala.collection.immutable.ArraySeq

given arraySeq[A]: Index[ArraySeq[A], Int, A] =
  Index(i =>
    Optional[ArraySeq[A], A](_.lift(i))(a => s => s.updated(i, a))
  )

val frontAxleDrive: Optional[CarSpec, Boolean] =
  Focus[CarSpec](_.axles.index(0).hasDrive)

println(frontAxleDrive.replace(true)(car))
println(frontAxleDrive.getOption(car))
```

В отличие от Lens, Optional значит, что поля может и не быть. Например, если попадётся пустой массив. Тогда методы типа modify или replace ничего не изменяет, а getOption вместо Some(false) вернёт None.

Ещё добавлю, что можно делать компизиции линз или optional друг с другом. Макросы позволяют написать сразу `focus(_.a.b.c)` и поэтому необходимость делать композицию вручную у меня не возникала. Но под капотом само собой там делается композиция линз `_.a`, `_.b`, `_.c`


## Смысл названий

Вообще оно красиво, но когда впервые смотришь описание библиотеки, ничего не понятно.

* **Lens:** линза, которая позволяет рассмотреть что-то внутри объектов. Умеет не только получать значения, но и делать копии родительских объектов с нужным новым значением.
* **AppliedLens:** То же самое, но мы уже выбрали конкретный объект и просто получаем/меняем значения. Для остальных классов в библиотеке тоже есть их Applied версии.
* **Optional:** линза, но смотрит на поле в объекте, которого может и не быть.
* [**Prism:**](https://www.optics.dev/Monocle/docs/optics/prism) стеклянная призма раскладывает свет на радугу, а тут она помогает разложить sealed класс на его отдельные имплементации.
* [**Iso:**](https://www.optics.dev/Monocle/docs/optics/iso) кажется, тут тоже игра слов с оптикой, но это изоморфизм, который умеет переводить один тип в другой и обратно.
* [**Traversal:**](https://www.optics.dev/Monocle/docs/optics/traversal) оптических аналогий не будет, штука позволяет что-то сделать сразу со всеми элементами коллекции

Про последние классы я не написал, потому что сам с ними не до конца разобрался, а в документации всё более-менее описано. Но мне кажется, что это продвинутые фичи для каких-то конкретных сценариев, а самое главное - это начать.












