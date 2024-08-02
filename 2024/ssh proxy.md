Можно сделать прокси через ssh туннель.

С опцией -R можно сделать reverse proxy.
Примеры на [stackoverflow](https://superuser.com/questions/370930/ssh-reverse-socks-tunnel)

Пример:
```
ssh -R 1080 user@192.168.1.15
```
Порт 1080 для прокcи считается стандартным.
Проверить работоспособность на удалённом сервере:
```
curl --socks5 localhost https://example.com
```

По умолчанию порт будет открыт только для localhost
Если захочется сделать порт доступным для других компов, надо будет сделать две вещи:

1. указать не только порт, но и адрес
2. в настройках /etc/ssh/sshd_config поменять опцию GatewayPorts с no на yes или clientspecified.

С yes подключение будет разрешено всем, создавать так:
```
ssh -R 192.168.1.15:1080 user@192.168.1.15
```

C clientspecified- только специально указанному адресу. Я не пробовал, пример [с просторов интернета](https://www.ssh.com/academy/ssh/tunneling-example):
```
ssh -R 52.194.1.73:8080:localhost:80 host147.aws.example.com
```


### Firefox

В настройках надо будет поставить галочку напротив socks5 proxy, а галочки у http и https - не включать!

