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
* Asus ROG STRIX B550-F GAMING (поддерживает сеть 2.5 гигабита)
* nvidia 1060 3 Gb
* Блок питания - не помню какой, на 750 ватт.
* жёсткий диск western digital gold на 16 терабайт WDC WD161KRYZ-01 (при включении сервера издаёт привычные звуки из детства)
* выдув воздуха из корпуса - попробовал Noctua NF-A12x25 PWM, работает тихо


## Домашняя сеть 

Материнская плата в сервере уже поддерживает 2.5 гигабита и мне захотелось в локальной сети увидеть эту скорость.
Итак, я купил свитч [TP-link DS105G-M2](https://www.omadanetworks.com/us/business-networking/omada-switch-unmanaged/ds105g-m2/). С ним ожидаемо никаких проблем не было. Ещё прикольно, что у каждого порта по две лампочки и прям на корпусе написано, что левая зелёная лампочка - 2.5 Гигабита, правая зелёная гигабит и правая жёлтая 100 Мбит. И есть лампочка, которая показывает питание. В общем, всё максимально наглядно и понятно.
В десктоп я купил сетевую карту - [TP-link TX201](https://www.tp-link.com/us/home-networking/pci-adapter/tx201/). И с ней тоже всё было прекрасно - я её вставил в слот, десктоп загрузился и без установки каких-либо драйверов сеть заработала. Моя ОС - Linux Mint 21.

Вообще раньше я почему-то считал свичи пережитком прошлого, но по-факту очень удобная штука для расширения сети - настраивать не надо, стоит на порядок дешевле, чем роутер с пятью аналогичными портами.

По локальной сети при копировании на сервер через sshfs удалось увидеть скорость 290 мегабайт в секунду. При копировании с сервера скорость почему-то держалась на уровне 110 мегабайт - для меня загадка, почему так - вроде это меньше, чем возможности жёсткого диска.

## Hard drive

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

## Immich

По-идее для запуска immich достаточно просто докера, но к сожалению он по-умолчанию запускает всё от рута и сохранённые файлы типа фотографий тоже будут от рута. Мне такое не понравилось. Причём если указать пользователя, то оно не работает, и придётся кое-какие дополнительные папки указывать.

В общем я специально под immich сделал такого же пользователя и добавил себя к нему в группу, чтобы от моего пользователя можно было безболезненно бэкапить файлы.

Прикреплю файл целиком:
```
#
# WARNING: To install Immich, follow our guide: https://immich.app/docs/install/docker-compose
#
# Make sure to use the docker-compose.yml of the current release:
#
# https://github.com/immich-app/immich/releases/latest/download/docker-compose.yml
#
# The compose file on main may not be compatible with the latest release.

name: immich

services:
  immich-server:
    user: $USER_ID:$GROUP_ID
    container_name: immich_server
    image: ghcr.io/immich-app/immich-server:${IMMICH_VERSION:-release}
    # extends:
    #   file: hwaccel.transcoding.yml
    #   service: cpu # set to one of [nvenc, quicksync, rkmpp, vaapi, vaapi-wsl] for accelerated transcoding
    volumes:
      # Do not edit the next line. If you want to change the media storage location on your system, edit the value of UPLOAD_LOCATION in the .env file
      - ${UPLOAD_LOCATION}:/usr/src/app/upload
      - /etc/localtime:/etc/localtime:ro
    env_file:
      - .env
    ports:
      - '2283:2283'
    depends_on:
      - redis
      - database
    restart: always
    healthcheck:
      disable: false

  immich-machine-learning:
    user: $USER_ID:$GROUP_ID
    container_name: immich_machine_learning
    # For hardware acceleration, add one of -[armnn, cuda, rocm, openvino, rknn] to the image tag.
    # Example tag: ${IMMICH_VERSION:-release}-cuda
    image: ghcr.io/immich-app/immich-machine-learning:${IMMICH_VERSION:-release}
    # extends: # uncomment this section for hardware acceleration - see https://immich.app/docs/features/ml-hardware-acceleration
    #   file: hwaccel.ml.yml
    #   service: cpu # set to one of [armnn, cuda, rocm, openvino, openvino-wsl, rknn] for accelerated inference - use the `-wsl` version for WSL2 where applicable
    volumes:
      - ${MACHINE_LEARNING_CACHE}:/cache # conflicting documentation for /cache or /.cache, have yet to test this
      - ${MACHINE_LEARNING_CONFIG}:/.config  
    env_file:
      - .env
    restart: always
    healthcheck:
      disable: false

  redis:
    user: $USER_ID:$GROUP_ID
    container_name: immich_redis
    image: docker.io/valkey/valkey:8-bookworm@sha256:42cba146593a5ea9a622002c1b7cba5da7be248650cbb64ecb9c6c33d29794b1
    healthcheck:
      test: redis-cli ping || exit 1
    restart: always
    volumes:
      - ${REDIS_DATA}:/data

  database:
    user: $USER_ID:$GROUP_ID
    container_name: immich_postgres
    image: docker.io/tensorchord/pgvecto-rs:pg14-v0.2.0@sha256:739cdd626151ff1f796dc95a6591b55a714f341c737e27f045019ceabf8e8c52
    environment:
      POSTGRES_PASSWORD: ${DB_PASSWORD}
      POSTGRES_USER: ${DB_USERNAME}
      POSTGRES_DB: ${DB_DATABASE_NAME}
      POSTGRES_INITDB_ARGS: '--data-checksums'
    volumes:
      # Do not edit the next line. If you want to change the database storage location on your system, edit the value of DB_DATA_LOCATION in the .env file
      - ${DB_DATA_LOCATION}:/var/lib/postgresql/data
    healthcheck:
      test: >-
        pg_isready --dbname="$${POSTGRES_DB}" --username="$${POSTGRES_USER}" || exit 1; Chksum="$$(psql --dbname="$${POSTGRES_DB}" --username="$${POSTGRES_USER}" --tuples-only --no-align --command='SELECT COALESCE(SUM(checksum_failures), 0) FROM pg_stat_database')"; echo "checksum failure count is $$Chksum"; [ "$$Chksum" = '0' ] || exit 1
      interval: 5m
      start_interval: 30s
      start_period: 5m
    command: >-
      postgres -c shared_preload_libraries=vectors.so -c 'search_path="$$user", public, vectors' -c logging_collector=on -c max_wal_size=2GB -c shared_buffers=512MB -c wal_compression=on
    restart: always

volumes:
  model-cache:

```

И .env

```
# You can find documentation for all the supported env variables at https://immich.app/docs/install/environment-variables

# The location where your uploaded files are stored
UPLOAD_LOCATION=./library

# The location where your database files are stored. Network shares are not supported for the database
DB_DATA_LOCATION=./postgres

# To set a timezone, uncomment the next line and change Etc/UTC to a TZ identifier from this list: https://en.wikipedia.org/wiki/List_of_tz_database_time_zones#List
# TZ=Etc/UTC

# The Immich version to use. You can pin this to a specific version like "v1.71.0"
IMMICH_VERSION=release

# Connection secret for postgres. You should change it to a random password
# Please use only the characters `A-Za-z0-9`, without special characters or spaces
DB_PASSWORD=add-password-here

# The values below this line do not need to be changed
###################################################################################
DB_USERNAME=postgres
DB_DATABASE_NAME=immich

USER_ID=111
GROUP_ID=112

MACHINE_LEARNING_CACHE=./machine_learning/cache # necessary for non-root use
MACHINE_LEARNING_CONFIG=./machine_learning/config # necessary for non-root use
REDIS_DATA=./redis # necessary for non-root use
```

А ещё я сделал сервис, чтобы запускать/останавливать с помощью systemd (наверняка есть и другие способы, но я сделал так)

```
[Unit]
Description="Immich in docker"

After=network-online.target nss-lookup.target

[Service]
Type=simple
TimeoutStartSec=86400
ExecStartPre=/bin/bash -c 'while [ ! -e PATH_TO_IMMICH_DIR ]; do sleep 10; done'
ExecStart=docker compose --project-directory PATH_TO_IMMICH_DIR up

[Install]
WantedBy=multi-user.target
```

Хитрый шаг - сервис запускается, когда нужная папка будет примонтирована.
Я сначала узнал что в systemd можно задать путь в специальной юните .path, потом он при запуске запустит сервис .service, а тот выставит .target и я из другого сервиса смогу указать "After=data.target", но это какое-то извращение. При этом повылазит куча нюансов типа того что делать, если папку отмонтировали и примонтировали снова. Я так попробовал, у меня в итоге не заработало и я вместо этого просто сделал костыль с `while [ ! -e PATH_TO_IMMICH_DIR ]; do sleep 10; done`.

Очень странно, что нельзя просто взять и указать, что сервис просто ждёт файла в папке, вместо этого надо фигачить стрёмные приседания типа .path -> .service -> .target -> .service waiting for target






