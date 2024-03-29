
# О чем пойдет речь?
Часто нагрузочное тестирование требует большой нагрузки на систему, которую не может обеспечить 1 экземпляр JMeter. На данный момент я не нашел ни одного работающего решения для выполнения подобой задачи. Поэтому было разработано решение с возможностью параллельного запуска нескольких экземпляров Jmeter с нескольких машин. 
Помимо своей основной функции, данное решение позволяет отслеживать результат в режиме реального времени при запуске теста.


> Дисклеймер: в данном примере я умышленно не использую docker контейнеры и выполняю настройку окружения на чистой ВМ, т.к, по моему мнению, только такой вариант установки дает полную картину взаимодействия сервисов, что в свою очередь полезно для новичков.

Apache Jmeter умеет отправлять данные во внешние источники, что полезно для отслеживание выполнения тестов в реальном времени. В данном примере я расскажу как настроить подобное взаимодействие и поделюсь готовым решением, по настройке дашбордов, которые использую на множестве проектов ни один год.

# Установка InfluxDB
Для начала нам понадобится InfluxDB, я использую довольно старую версию 1.8, в ней нет графического интерфейса, который, впрочем, нам не понадобится, потому как для построения дашбордов мы будем использовать Grafana.

```bash
# Добавляем репозиторий в систему:
sudo apt-key adv --keyserver keyserver.ubuntu.com --recv-keys D8FF8E1F7DF8B07E
echo 'deb  https://repos.influxdata.com/debian stable main' | sudo tee /etc/apt/sources.list.d/influxdata.list
# Устанавливаем и запускаем InfluxDB
sudo apt update && sudo apt install influxdb 
sudo systemctl unmask influxdb.service
sudo systemctl start influxdb
# Проверяем правильность установки:
influx -version
```
Если в терминале появилось  "InfluxDB shell version: 1.8.10", значит мы все сделали правильно!

Далее, для того чтоб JMeter мог подключиться к нашей базе, необходимо создать данные для подключения
```bash
# Открываем консоль работы с БД:
influx
# Создаем базу в которую JMeter будет писать данные:
create database "performance"
# Создаем пользователя для подключения и выдаем ему права на доступ к базе:
CREATE USER jmeter WITH PASSWORD 'passwordfortest'
GRANT ALL ON performance TO jmeter 
```
На этом настройка БД завершена. 

# Настройка JMeter

Есть 2 хороших листенера для работы с данными из JMeter:
1. rocks.nt.apm.jmeter.JMeterInfluxDBBackendListenerClient 
2. org.apache.jmeter.visualizers.backend.influxdb.HttpMetricsSender

Я покажу настройку для обоих, потому что они в какой-то мере дополняют друг друга, однако стоит отметить, что для многопотоковой нагрузки полноценно подходит лишь первый, потому что только в нем имеется информация о количестве активных тредов, которые можно просуммировать, в случае если вы запускаете тесты одновременно с нескольких машин, как в моем случае.

Скачиваем файл https://github.com/NovatecConsulting/JMeter-InfluxDB-Writer/releases/download/v-1.2/JMeter-InfluxDB-Writer-plugin-1.2.jar и кладем его в папку с JMeter >/lib/ext
Далее добавляем Backend Listener в наш проект и настраиваем его

![Добавляем листенер](./images/jmeter-addlistener.png)
![Настраиваем rocks листенер](./images/jmeter-influxsettimgrocks.png)
![Настраиваем apache листенер](./images/jmeter-influxsettingsapache.png)

где 127.0.0.1 - адрес сервера, на котором располагается наша InfluxDB

# Установка и настройка Grafana

Для красивого отображения записанных в Influx данных нам потребуется Grafana.
```bash
# Устанавливаем зависимости
sudo apt install -y adduser libfontconfig1 musl
# Скачиваем Grafana загрузка недоступна из России, но мы то знаем что делать :)
wget https://dl.grafana.com/enterprise/release/grafana-enterprise_10.1.4_amd64.deb
# Установка и запуск
sudo dpkg -i grafana-enterprise_10.1.4_amd64.deb
sudo systemctl start grafana-server
```
После успешного старта можно открывать в браузере http://127.0.0.1:3000 , где 127.0.0.1 - адрес хоста, на котором установлена Grafana. Первый вход осуществляется с логином и паролем admin, после входа система попросит ввести новый пароль админа.
Подключаем InfluxDB в качестве источника данных на странице: http://127.0.0.1:3000/datasources
![Настройка датасорса](./images/grafana-influx-connect.png)




Я приведу в качестве примера 2 своих дашборда под каждый из листенеров, в графана импортируем дашборд из JSON:

 1. https://github.com/kschepkin/perf-jmeter/blob/main/dashboards/Jmeter-Rocks-Dashboard.json
 2. https://github.com/kschepkin/perf-jmeter/blob/main/dashboards/Jmeter-Apache-Dashboard.json

# Описание дашбордов

В результате описанных действий у нас появится 2 дашборда:
Rocks предназнеачен под параллельный запуск нескольких экземпляров JMeter на разных машинах.
![Rocks дашборд](./images/Rocks-parallel-dashboard.png)
Apache не предполагает более одного инстанса нагрузки, однако удобен в качестве дополнительного инструмента анализа данных, т.к. это единственный листенер, который передает детальную информацию об ошибках.
![Apache дашборд](./images/Apache-dashboard.png)


## Бонус: Настройка тестового прогона в Jenkins
Для прогона тестов я использую одновременно несколько тестовых машин, однако при таком подходе встает вопрос как быстро настроить окружение, особенно с учетом частой смены этих машин.
Я решил вопрос написанием Pipeline скрипта для Jenkins, скрипт по умолчанию расчитан на 4 ноды, однако легко модернизируется под большее количество, а для отключения ноды укажите OFF в качестве адреса сервера.
Для работы вам потребуется несколько тестовых машин, на которых предварительно требуется установить Java
```bash
# Устанавливаем Java
sudo apt install default-jre default-jdk
```
Ссылка на Pipeline скрипт для Jenkins:
https://github.com/kschepkin/perf-jmeter/blob/inProgress/jenkins-pipelines/parallel_pipeline.groovy
При настройке джобы в Jenkins требуется включить опцию "Это - параметризированная сборка" и добавить String параметр с именем "PERF_TEST".
Также я приложил Pipeline и скрипт Kill, который принудительно завершает все экземпляры JMeter - понадобится, если требуется завершить тесты раньше времени.

# Docker
Также вы можете использовать предварительно собранный мной докер контейнер, инструкция по запуску есть репозитории: [Docker Hub](https://hub.docker.com/repository/docker/kschepkin/perf-test/general)
