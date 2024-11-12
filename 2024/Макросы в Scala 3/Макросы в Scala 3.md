Заметка не полная, дописываю потихоньку.

Документация Scala по макросам: [https://docs.scala-lang.org/scala3/guides/macros/macros.html](https://docs.scala-lang.org/scala3/guides/macros/macros.html)


Самый простой способ метапрограммирования - inline функции. Если можно решить задачу с их помощью, то в макросы можно и не лезть.

inline может использоваться в нескольких местах:

* `inline def smth() ... ` - инлайн функция
* `inline def log(inline msg: String) = if (debug) { print(msg) }` - вместо msg подставится выражение, вычисляющее аргумент. Если использовать msg несколько раз, то выражение будет вызвано несколько раз. По этой логике похоже на вызов по имени.
* `inline isDebug = false` - константа времени компиляции. Компилятор может подставлять её значения.
* `inline if ...`, `inline match ...` - привычные нам функции, но аргумент должен быть доступен на этапе компиляции. Аргументами могут быть типы. Например `T match { case String => ...; case Char => ... , ...} `


## Quoting, Splicing

Макросы вызываются из inline функций, компилятор подставляет туда неявный параметр Quotes. С Quotes доступны методы для сравнения типов и прочего.

Для меня лучшей аналогией стала интерполяция строк.

Scala
```
s"это строка"
s"это строка ${переменная внутри строки} снова строка"
s"это строка ${вызов фунции(s"внутренняя-строка-аргумент")} и снова строка"
s"это строка ${вызов фунции(s"внутренняя-строка-аргумент ${с переменной внутри}")} и снова строка"
```

Аналогичный переход делается в макросах.

Здесь используется quoting (цитируем код) и splicing (вставляем его куда-то). Они выглядят как ${} и '{}

Просто так объекты из разных "уровней" смешивать нельзя.

В начальном уровне это просто код и какие-то типы (например, x: Int).
На уровне макроса это выражение Expr[Int], макрос можкт создавать свои переменные (x: Int), которые не видны снаружи.

Если мы хотим сделать переход из одного мира в другой. например, превратить значение Int в выражение, которое возвратит значение (Expr[Int]), то надо вызвать конструктор Expr(x).

Обратный переход из Expr[Int] => Int не всегда возможен, потому что выражение может быть переменной и в момент компиляции мы не сможем её вычислить. Есть методы типа expr.value, которые попытаются вытащить константу, но работать будут не всегда.


## Распечатать выражение в виде строки

```Scala
inline def myShow(x: Int): String =
  ${ myShowImpl('x) }


def myShowImpl(x: Expr[Int])(using q: Quotes): Expr[List[String]] =
  import q.reflect.*
  Expr(x.show)
```

## Список полей в case-классе

```Scala
inline def getFields[T]: List[String] =
  ${ getFieldsImpl[T] }


def getFieldsImpl[T](using q: Quotes): Expr[List[String]] =
  import q.reflect.*
  import q.reflect.SymbolMethods
  // если не импортировать SymbolMethods явно, то в IDEA перестанет работать автодополнение
  val pointType: TypeRepr = TypeRepr.of[T]
  val fields: List[q.reflect.Symbol] = pointType.typeSymbol.caseFields
  Expr(fields.map(_.name))
```


## генерация toString


```Scala
import scala.quoted.{Expr, Quotes, Type}
import scala.quoted


object MyMacro:
 inline def str[T](p: T): String =
   ${ impl('p) }


 def impl[T: Type](t: Expr[T])(using q: Quotes): Expr[String] =
   import q.reflect.*
   import q.reflect.SymbolMethods
   // если не импортировать SymbolMethods явно, то в IDEA перестанет работать автодополнение

   val pointType: TypeRepr = TypeRepr.of[T]
   val typeSymbol: q.reflect.Symbol = pointType.typeSymbol
   val fields: List[q.reflect.Symbol] = typeSymbol.caseFields
   val className: String = pointType.typeSymbol.name


   val body: Expr[String] = '{
     val s = scala.StringBuilder()
     s.append(${ Expr(className) })
     s.append("(")
     ${
       val fieldsExprs: List[Expr[Unit]] = fields.map(field =>
         val fieldName: String = field.name
         val fieldGetter = Select(t.asTerm, field)
         val fieldGetterExpr: Expr[Any] = fieldGetter.asExprOf[Any]


         '{
           s.append(${ Expr(fieldName) })
           s.append("=")
           s.append(${ fieldGetterExpr }.toString)
         }
       )


       if (fieldsExprs.nonEmpty) {
         fieldsExprs.reduce((a, b) => '{ $a ; s.append(", ") ; $b })
       } else '{}
     }
     s.append(")")
     s.toString
   }


   // Expr(body.show)
   // можно вернуть body.show и посмотреть, что сгенерировал макрос.
   body

```
