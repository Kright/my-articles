# Scala 3: transparent inline with Dynamic

## динамическая типизация внутри статической

В скале есть интерфейс Dynamic. От него можно унаследоваться и получить динамическую типизацию - при отсутствии поля в классе компилятор подставит вызов специального метода.

```Scala
import scala
import scala.language.dynamics

class MyClass extends Dynamic

val a = MyClass
a.x
// a.selectDynamic("x")
a.x = 2
// a.updateDynamic("x")(2)
```

Класс MyClass может быть любым, например таким:

```Scala
class MyClass extends Dynamic {
  private var x: Int = 0
	
  def selectDynamic(fieldName: String): Int = {
	fieldName match 
	  case "x" => return x 
  }
	
  def updateDynamic(fieldName: String)(value: Int): Unit = {
	fieldName match 
	  case "x" => x = value
  }
}
```

Конкретно такой класс получился не особо полезным - по поведению он похож на 

```Scala
class MyClass(){var x: Int = 0}
```

только хуже - во время компиляции мы не увидим ошибок, если обратимся к несуществующему полю, и код упадёт в рантайме. Впрочем, этот код нужен для иллюстрации возожностей языка.

Что, если нам захочется сделать класс с полями разных типов?

```Scala
class MyClass() {
    var x: Int = 0 
	var b: Boolean = false
}
```

Если попробуем написать альтернативу этому классу с помощью Dynamic, в качестве принимаемых и возвращаемых типов придётся указывать Any и кастовать к какому-то типу.
Впрочем, у нас тут Scala 3, в которой появились Union types, можно указать в качестве типа поля Int | Boolean и что-то иное типа строки компилятор не даст присвоить.

```Scala
class MyClass extends Dynamic {  
  private var x: Int = 0  
  private var b: Boolean = false  
  
  def selectDynamic(field: String): Int | Boolean = {  
    field match  
      case "x" => x  
      case "b" => b  
  }  
  
  def updateDynamic(field: String)(value: Int | Boolean): Unit = {  
    field match  
      case "x" =>  
        value match  
          case v: Int => x = v  
      case "y" =>  
        value match  
          case v: Boolean => b = v  
  }  
}
```

Но появилась ещё проблема - мы всё равно можем попробовать присвоить Boolean в x или Int в b, а потом упадём в рантайме. Такова участь динамической типизации, ничего не поделать.

И тут c ноги врывается transparent inline! Перепишем метод selectDynamic:

```Scala
import scala.compiletime.error

MyClass { 
    ...
	transparent inline def selectDynamic(inline field: String): Int | Boolean = {  
	  inline field match  
		case "x" => x
		case "b" => b
		case _ => error("unknown field in MyClass")
	}
}
```

Что будет дальше? В месте вызова ```a.x``` компилятор подставит вызов функции selectDynamic, пойдёт внутрь, в inline match, найдёт там подходящую строку "x" и заменит всё-всё-всё на простое обращение к полю x с типом Int.

Если быть точным и посмотреть байткод - у MyClass появится метод - геттер ```def inline$x(): Int```, который будет вызываться напрямую.

Ну либо на это можно смотреть так:

```Scala
val x: Int = a.selectDynamic("x")
val b: Boolean = a.selectDynamic("b")
```

Возвращаемый тип зависит от аргумента!

Кроме того, если обратиться к несуществующему полю, прямо во время компиляции подставится compiletime.error и компилятор покажет ошибку.

Поздравляю, мы снова вернулись к статической типизации. Какие выводы можно сделать?

* transparent inline - очень мощный инструмент, позволяющий гибко работать с типами. В scala 2.0 такое невозможно.
* Нет строго дуализма между runtime и compiletime преобразованиями, гибкость scala позволяет в какой-то мере заменить первое на второе. В С++ похожая ситуация.

Для полноты картины покажу, как можно переписать второй метод:

```Scala
transparent inline def updateDynamic(inline field: String)(inline value: Int | Boolean): Unit = { 
  inline field match  
    case "x" =>  
      inline value match  
        case v: Int => x = v  
        case _ => error("should be int")  
    case "y" =>  
      inline value match  
        case v: Boolean => b = v  
        case _ => error("should be boolean")  
    case _ => error("unknown field")  
}
```

Посмотрите на переход от динамической типизации обратно к статической при помощи более "умного" компилятора! Что-то подобное можно попробовать в динамических языках типа Python, чтобы ловить ошибки как можно раньше. Не все, но какую-то часть.

Пример кода выше не очень практичен - можно сделать обычный класс с полями x: Int, b:Boolean и он будет прекрасно работать. В классе c Dynamic уже известные поля можно заменить на реальные поля и это тоже будет прекрасно работать, компилятор будет обращаться напрямую к ним вместо вызова selectDynamic:

```Scala
class MyDynamic extends Dynamic{
    var x: Double = 0
	var b: Boolean = 0
	def selectDynamic(...) ...
}
```

В общем, код выше дублирует базовые возможности языка, бесполезен сам по себе, но, надеюсь, полезен для иллюстрации.

Для чего использовать transparent inline в реальном коде - не знаю. Люди как-то жили без этой возможности и прекрасно решали свои задачи. Но я точно уверен, что часть этих задач теперь можно решить более гибко и красиво.