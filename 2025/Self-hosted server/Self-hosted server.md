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
* Asus ROG STRIX B550-F GAMING (поддерживает сеть 2.5 гигабита, но пока что второго устройства с такой скоростью у меня нет)
* nvidia 1060 3 Gb
* Блок питания - не помню какой, на 750 ватт.
* жёсткий диск western digital gold на 16 терабайт WDC WD161KRYZ-01 (при включении сервера издаёт привычные звуки из детства)
* выдув воздуха из корпуса - попробовал Noctua NF-A12x25 PWM, работает тихо

## Hard drive

###


Посмотреть, какие логические и физические размеры секторов на дисках:

```
sudo fdisk -l

Disk /dev/sda: 14.55 TiB, 16000900661248 bytes, 3906469888 sectors
Disk model: WDC WD161KRYZ-01
Units: sectors of 1 * 4096 = 4096 bytes
Sector size (logical/physical): 4096 bytes / 4096 bytes
I/O size (minimum/optimal): 4096 bytes / 4096 bytes
```

По-умолчанию логический размер будет 512 байт из-за соображений совместимости с компьютерами, выпущенными до 2010 года.
Но можно выбрать и побольше (например, я хочу чтобы логический размер совпадал с физическим - всё равно диск пишет-читает по 4кб зараз). 

Для начала надо посмотреть, какие размеры вообще поддерживаются:
```
sudo hdparm -I /dev/sdX
```

На моём диске был логический размер 512 байт и он поддерживает логический размер в 4кб. Я ради интереса почитал интернет и наткнулся на пост какого-то бедолаги, у которого диск подключался через usb и после изменения найстройки через usb-адаптер подключаться перестал. 
Но к счастью у меня нормальное подключение через sata и диск абсолютно пустой, потому что все данные с диска потеряются при изменении размера сектора.

```
sudo hdparm --set-sector-size 4096 --please-destroy-my-drive /dev/sda
```

Так что я поменял размер сектора и перезагрузил сервер.
После перезагрузки всё было ок, но надо будет заново разметить разделы на диске.

https://unix.stackexchange.com/questions/606072/change-logical-sector-size-to-4k

А вообще очень красиво получается - оперативная память выделяется страницами по 4кб и блоками в 4кб пишется на диск. 

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


Создать новый раздел на диске можно с помощью
```
sudo gdisk
```

Убедиться, что всё выровнено
```
sudo parted /dev/sda align-check optimal 1
```

Создать зашифрованный раздел на sdaX
```
sudo cryptsetup luksFormat --type=luks2 \
--sector-size=4096 \
-c aes-xts-plain64 \
-s 512 \
-h sha512 \
--pbkdf argon2id \
--pbkdf-memory 4194304 \
--pbkdf-parallel 8 \
--iter-time 5000 \
--use-urandom \
/dev/sdaX
```

подключить в /dev/mapper/data:

```
sudo cryptsetup open /dev/sda1 data
```

создать файловую систему:

```
sudo mkfs.btrfs -d single -m dup -L databtrfs -f /dev/mapper/data
```

Примонтировать:
```
sudo mount -t btrfs -o noatime,nossd,autodefrag,space_cache=v2,compress=zstd:3 /dev/mapper/data /mnt/data/
```




