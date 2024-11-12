Внутри arch linux, пакетный менеджер - pacman

Вначале надо будет придумать и установить свой пароль для sudo. Как сделал я уже забыл.

```sh
sudo steamos-readonly disable
sudo steamos-readonly enable
```

Если я правильно понимаю, после обновления ОС все эти изменения пропадут (кроме того что в home)

Для установки пакетов недостаточно выключить readonly:

```sh
sudo pacman-key --init
sudo pacman -S archlinux-keyring 
sudo pacman-key --populate archlinux
```

но к сожалению у меня не заработало, пришлось отключить проверку подписи файлов. В /etc/pacman.conf:
```
SigLevel = TrustAll
#SigLevel = Required Databaseoptional
```

## SSH

генерация ssh ключа
```
ssh-keygen =t ecdsa -b 521
```
либо более старый подход типа 
```
ssh-keygen -t rsa -b 4096
```
подробнее [тут](https://www.ssh.com/academy/ssh/keygen#choosing-an-algorithm-and-key-size)

ssh сервер уже установлен, полезные команды:

```sh
sudo systemctl enable sshd
sudo systemctl start sshd
sudo systemctl restart sshd
```

ключи добавить в ~/.ssh/authorized_keys

узнать адрес:
```
ip addr
```

```
ssh deck@192.168.*.*
```

