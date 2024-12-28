TLDR: разобрался как запустить превью версию, но эффект на производительность пока что отрицательный.

Здесь можно скачать early-access build: [https://jdk.java.net/valhalla/](https://jdk.java.net/valhalla/) и распаковать в любую папку.

Потом зайти в bin, прямо в ней создать файл ValuePoint.java с кодом и запустить ./java --enable-preview --source 23 ValuePoint.java

В файле я накидал простой пример:

```java
public value class ValuePoint {
    public final double x;
    public final double y;

    public ValuePoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public ValuePoint add(ValuePoint p) {
        return new ValuePoint(x + p.x, y + p.y);
    }

    public ValuePoint sub(ValuePoint p) {
        return new ValuePoint(x - p.x, y - p.y);
    }

    public String toString() {
        return "ValuePoint(" + x + "," + y + ")";
    }

    public static void main(String[] args) {
        ValuePoint[] arr = new ValuePoint[1000];

        for (int i = 0; i < arr.length; i++) {
            arr[i] = new ValuePoint(i, i);
        }

        for (int k = 0; k < 100; k++) {
            long start = System.nanoTime();
            ValuePoint sum = new ValuePoint(0, 0);
            for (int i = 0; i < arr.length; ++i) {
                for (int j = 0; j < arr.length; ++j) {
                    sum.add(arr[j].sub(arr[i]));
                }
            }
            long end = System.nanoTime();
            System.out.println("sum = " + sum + " time = " + (end - start) + "ns");
        }
    }
}
```

Без слова value одна итерация цикла завершается за 0.7 мс, с этим словом 1.3 мс.
Вывод - фичу попробовать можно, но пока что результат на перформанс может быть отрицательным.

Надеюсь, разработчики когда-нибудь доведут value типы до релиза и дальше смогут сфокусироваться на оптимазациях.

Дата моего эксперимента - 2024-12-28, Build 23-valhalla+1-90 (2024/7/26)


