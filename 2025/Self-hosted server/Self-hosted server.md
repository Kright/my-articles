---
title: "Self-hosted server"
author: Kright
date: 2025-05-12
---

Заметку буду дописывать со временем.

Больше трёх лет назад я сделал сервер из малинки и внешнего жёсткого диска, сейчас решил двигаться дальше и собрать что-то из десктопных комплектующих.

Большая часть деталей у меня уже была "лишняя" после апгрейдов, докупил только материнскую плату, оперативную память и жёсткий диск, получилась сборная солянка. На памяти и материнской плате решил не экономить - для меня сборка и настройка сервера это развлечение и не хочется высчитывать, сколько именно памяти мне нужно или как выкроить на выборе материнской платы (и остаться без возможности разгона или андервольтинга).

### Конфигурация

* Amd Ryzen 3700x со стандартным кулером
* 32x4 = 128 Gb unbuffered памяти с ECC KINGSTON 32GB DDR4 3200MHz ECC KTH-PL432E/32G
* M.2 SSD samsung evo 512 GB
* Asus ROG STRIX B550-F GAMING
* nvidia 1060 3 Gb
* Блок питания - не помню какой, на 750 ватт.
* жёсткий диск western digital gold на 16 терабайт (при включении сервера издаёт привычные звуки из детства)
* выдув воздуха из корпуса - попробовал Noctua NF-A12x25 PWM, работает тихо

### cryptsetup benchmark

```
cryptsetup benchmark
# Tests are approximate using memory only (no storage IO).
PBKDF2-sha1      1744718 iterations per second for 256-bit key
PBKDF2-sha256    3102295 iterations per second for 256-bit key
PBKDF2-sha512    1491573 iterations per second for 256-bit key
PBKDF2-ripemd160  801663 iterations per second for 256-bit key
PBKDF2-whirlpool  685343 iterations per second for 256-bit key
argon2i      10 iterations, 1048576 memory, 4 parallel threads (CPUs) for 256-bit key (requested 2000 ms time)
argon2id     10 iterations, 1048576 memory, 4 parallel threads (CPUs) for 256-bit key (requested 2000 ms time)
#     Algorithm |       Key |      Encryption |      Decryption
        aes-cbc        128b      1124.7 MiB/s      2228.8 MiB/s
    serpent-cbc        128b       118.8 MiB/s       675.5 MiB/s
    twofish-cbc        128b       231.2 MiB/s       402.0 MiB/s
        aes-cbc        256b       887.5 MiB/s      2104.9 MiB/s
    serpent-cbc        256b       118.9 MiB/s       675.9 MiB/s
    twofish-cbc        256b       231.2 MiB/s       401.5 MiB/s
        aes-xts        256b      2050.6 MiB/s      2061.8 MiB/s
    serpent-xts        256b       595.5 MiB/s       587.6 MiB/s
    twofish-xts        256b       371.3 MiB/s       370.0 MiB/s
        aes-xts        512b      1958.5 MiB/s      1957.6 MiB/s
    serpent-xts        512b       595.5 MiB/s       587.6 MiB/s
    twofish-xts        512b       371.6 MiB/s       370.2 MiB/s
```

Лучшая опция - [aes-xts](https://en.wikipedia.org/wiki/Disk_encryption_theory#XTS) с ключом 512 бит.
aes-cbc устаревший, использовать его не надо.

Получается, что sata-диски можно почти безболезненно шифровать, а вот с nvme наверно не стоит - шифрование будет медленнее, чем возможная скорость чтения или записи данных.



