# Глоссарий для переводов RU -> EN

Общий словарь для переводов статей на английский. Цель - чтобы в разных статьях одни и те же слова и приёмы
переводились одинаково и чтобы английский текст звучал так же, как русский оригинал.
Правила работы переводчика - в [brief.md](brief.md). Если для статьи нужно другое решение - можно, но стоит дописать его сюда.

## Общие соглашения

| В оригинале | В переводе |
|---|---|
| тире « - » (дефис с пробелами) | так же " - ", без длинных тире |
| разговорные сокращения | it's, there's, you'd, I'd |
| «мы», «вы», безличное «можно» | we / you, как в оригинале |
| орфография | американская (behavior, color, tire) |
| абзац со строчной буквы | с заглавной |
| нет точки в конце пункта списка | тоже нет |
| фрагменты и заметки | остаются фрагментами, не дописываются до полных предложений |
| названия библиотек, языков, инструментов, игр, продуктов (libgdx, java, numpy, github, assetto corsa) | официальное написание: libGDX, Java, NumPy, GitHub, Assetto Corsa, DiRT Rally 2.0, MOZA, Wi-Fi |
| прочие английские термины латиницей (fma, ffb) | как есть, с тем же регистром |
| кириллические «скала», «си», «жигули» | Scala, C, Zhiguli |
| «Т.е., ...» в начале абзаца | "I.e., ..." |
| ссылки на ru.wikipedia | на английскую статью, если она есть |
| ссылки на другие статьи (`Статья.md`) | на `Статья.en.md`, если перевод существует |
| код | байт в байт, переводятся только русские комментарии и строки |

## Слова с отношением

| Оригинал | Перевод | Примечание |
|---|---|---|
| фишка | trick / feature | "the most important trick is..." |
| аж | no less / a whole | "a mantissa of over a hundred bits, no less", "a whole 360 ns" |
| казалось бы | though you'd think | |
| как ни странно | oddly enough | |
| вдруг | suddenly | |
| внезапно (в начале фразы) | unexpectedly / turns out | |
| глобально | globally | авторское словечко, не "broadly" |
| типа (вводное) | like, | "Like, f takes 1, returns..." |
| якобы | supposedly | |
| офигенно | freaking awesome | |
| прикольно / что прикольно | cool / the nice part - | |
| штуки / всякие штуки | things / all sorts of stuff | |
| реально | really / real | повтор «реальных... реально» сохраняется |
| я разошёлся | I got carried away | |
| закинул в ... | dumped into ... | |
| потыкаться | poke around | |
| выжать из | squeeze out of | |
| кушает (токены) | eats | |
| фиаско | fiasco | слово сохраняется |
| на ровном месте | out of nowhere | |
| одним махом | in one fell swoop | |
| прибито гвоздями | nailed down / nailed to | |
| извращения | perversions | резкость сохраняется |
| протухнет | go stale | |
| вопросы со звёздочкой | extra credit questions | |
| удачи в отладке | good luck debugging | |
| используйте на здоровье | use it and enjoy | |
| считаю, что это успех | I'd call that a success | |
| всё ок | everything's ok | |
| из минусов - | on the downside - | |
| в общем и целом - | all in all - | |
| итоги | wrapping up | |
| историческая справка | historical note | |
| не изобретать велосипед | not reinvent the wheel | |
| нельзя просто так взять и ... | one does not simply ... | это отсылка к мему, сохраняется |
| преисполнились (мем) | reached full enlightenment | |
| понаделали | churned out | |
| рисуют красивые (цифры) | paint pretty ... | |
| кайфанул / это кайф | got a real kick out of it / is pure joy | |
| как будто (= кажется) | it seems like / seems like | |
| костыли / костылить | kludges / hack it in | |
| велосипедостроение, велосипеды | reinventing the wheel, reinvented wheels | |
| стрёмный | sketchy | |
| кривые моменты | janky bits | |
| сгорел с (бага) | what made me rage was | |
| забить на | give up on ... altogether | |
| рисовать фигню | draw garbage | |
| пруфов не будет | there won't be any proof | |
| так уж и быть | then, fine, | снисходительная интонация |
| мамкин (дрифтер) | some wannabe (drifter) | |
| как ёжик в тумане | like the hedgehog in the fog | отсылка сохраняется |
| по каждому чиху | at every sneeze | |
| для галочки | to tick a box | |
| вьетнамские флешбеки | Vietnam flashbacks | |
| забытая технология древних | forgotten technology of the ancients | |
| пишите в личку | send me a DM | |
| когда-то я учился в школе | once upon a time I was in school | |
| тормозит, тормоза (про скорость работы) | lags, lag / slowness | не brakes |
| уменьшительные (пружинки, машинка, телефончики) | little springs, the little car, little old phones | где звучит естественно |

## LLM и агенты

| Оригинал | Перевод |
|---|---|
| нейронка, нейронки | the LLM, LLMs (не "neural net", не "AI") |
| модель | model |
| навайбкожено, вайбкодинг | vibe-coded, vibe coding |
| размышления (thinking) | thinking |
| рассуждение | reasoning |
| схема (мышления) агентов | flow ("thinking flow"), не scheme/schema |
| агент-режиссёр | director agent |
| очки здоровья | health points |

## Программирование

| Оригинал | Перевод |
|---|---|
| боксинг | boxing |
| ручной цикл | hand-written loop |
| натуральный `while` | plain `while` |
| замер производительности | benchmark |
| на порядок / в полтора порядка | an order of magnitude / one and a half orders of magnitude |
| накладные расходы | overhead |
| кусочки памяти | little pieces of memory |
| «лочить» объекты | "lock" the objects (в кавычках, как у автора) |
| Foreign function interface (Java) | Foreign Function & Memory API при первом упоминании, дальше FFI |
| Си | C |
| дописывание компилятора | extending the compiler |
| скобочки | parens |
| дженерики | generics |
| мономорфизация | monomorphization |
| тайпклассы | typeclasses |
| ковариантность / контрвариантность / инвариантность / бивариантность | covariance / contravariance / invariance / bivariance |
| линейные типы | linear types |
| корутины | coroutines |
| сайд-эффекты | side effects |
| сборка мусора, сборщик мусора | garbage collection, garbage collector (GC) |
| подсчёт ссылок | reference counting |
| модель владения | ownership model |
| кеш-линия | cache line |
| заголовок объекта | object header |
| ось, размер вдоль оси | axis, size along the axis |
| броадкаст | broadcast |
| интервал (в срезах ArrayView) | range |
| движок (в текстах про машину) | physics engine / game engine | просто engine читается как двигатель |
| лесенки (алиасинг) | jaggies |
| частота опроса N мс | polling interval |
| теорема Котельникова | Nyquist theorem |
| наследовался от (интерфейса) | implemented |
| единицы: Мгц, Мб, кб | MHz, MB, kB |

## Арифметика с плавающей точкой

| Оригинал | Перевод |
|---|---|
| мантисса | mantissa |
| удвоенная точность (промежуточного результата) | twice the precision (не "double precision" - путается с типом double) |
| денормализованные числа | denormal numbers |
| переполнение | overflow |
| ошибка округления | rounding error |
| алгоритм Кэхэна | Kahan summation algorithm |

## Серия про автомобиль и физика

Заголовок серии: «Делаю демо с автомобилем и корректной физикой N» -> "I'm making a car demo with correct physics N".

| Оригинал | Перевод |
|---|---|
| корректная / честная физика | correct / honest physics |
| твёрдое тело | rigid body |
| момент инерции | moment of inertia |
| крутящий момент | torque |
| угловая скорость | angular velocity |
| подвеска | suspension |
| пружина | spring |
| амортизатор | damper |
| жёсткость | stiffness |
| сжатие / отбой | compression / rebound |
| стабилизатор поперечной устойчивости | anti-roll bar |
| рычаг (подвески) | control arm |
| шина | tire |
| диск | rim |
| пятно контакта | contact patch |
| сцепление (шин с дорогой) | grip |
| сцепление (узел) | clutch |
| проскальзывание | slip (slip angle, slip ratio - как есть) |
| занос | skid / drift (если намеренный) |
| недостаточная / избыточная поворачиваемость | understeer / oversteer |
| руль (игровой) | wheel / racing wheel |
| руль (в машине) | steering wheel |
| force feedback, FFB | как есть |
| дифференциал, блокировка | differential, locking |
| коробка передач | gearbox |
| обороты | RPM / revs |
| тяга (двигателя, на колесе) | torque |
| диагональное вывешивание, вывешенное колесо | diagonal wheel lift, the lifted wheel |
| геометрическая алгебра | geometric algebra (PGA - как есть) |
| бивектор, мотор | bivector, motor |
| солвер, констрейнт | solver, constraint |
| шаг симуляции | simulation step |
| Рунге-Кутта | Runge-Kutta (RK4) |
| жигули | Zhiguli (при первом упоминании можно "Zhiguli (Lada)") |
| копейка | the kopeyka (VAZ-2101) |
| сайлентблок | rubber bushing |
| схождение, развал, кастор | toe, camber, caster |
| угол Аккермана, плечо обката | Ackermann angle, scrub radius |
| задний мост | solid rear axle |
| карданный вал | driveshaft |
| редуктор (главная пара) | final drive |
| дифференциал повышенного трения | limited-slip differential |
| преднатяг | preload |
| закрытый / заблоченный / залоченный дифференциал | locked differential |
| фрикционы | clutch plates |
| люфт | backlash |
| отсечка | rev limiter |
| бросить сцепление | dump the clutch |
| дроссель | throttle |
| трамплин | jump |
| зацепиться за колею | hook into a rut |
| закрытый поворот | blind corner |
| клотоида | clothoid |
| база руля, коробка (периферия руля) | wheel base, shifter |
| выпрямляющее усилие на руле | self-aligning force |
