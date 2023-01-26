> Дисклеймер: в данном примере я умышленно не использую docker контейнеры и выполняю настройку окружения на чистой ВМ, т.к, по моему мнению, только такой вариант установки дает полную картину взаимодействия сервисов, что в свою очередь полезно для новичков.

Apache Jmeter умеет отправлять данные во внешние источники, что полезно для отслеживание выполнения тестов "в прямом эфире". В данном примере я расскажу как настроить подобное взаимодействие и поделюсь готовым решением, по настройке дашбордов, которые использую на множестве проектов ни один год.

Для начала нам понадобится InfluxDB, я использую довольно старую версию 1.8, в ней нет графического интерфейса, который, впрочем, нам не понадобится, потому как для построение дашбордов мы будем использовать Grafana.

```bash
# Добавляем репозиторий в систему:
sudo apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 684A14CF2582E0C5
echo 'deb  https://repos.influxdata.com/debian stable main' | sudo tee /etc/apt/sources.list.d/influxdata.list
```
```bash
# Устанавливаем и запускаем InfluxDB
sudo apt update && sudo apt install influxdb
sudo systemctl unmask influxdb.service
sudo systemctl start influxdb
```
```bash
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

**Настройка JMeter**
Есть 2 хороших листенера для работы с данными из JMeter:
1. rocks.nt.apm.jmeter.JMeterInfluxDBBackendListenerClient 
2. org.apache.jmeter.visualizers.backend.influxdb.HttpMetricsSender

Я покажу настройку для обоих, потому что они в какой-то мере дополняют друг друга, однако стоит отметить, что для многопотоковой нагрузки полноценно подходит лишь первый, потому что только в нем имеется информация о количестве активных тредов, которые можно просуммировать, в случае если вы запускаете тесты одновременно с нескольких машин, как в моем случае.

Скачиваем файл https://github.com/NovatecConsulting/JMeter-InfluxDB-Writer/releases/download/v-1.2/JMeter-InfluxDB-Writer-plugin-1.2.jar и кладем его в папку с JMeter >/lib/ext
Далее добавляем Backend Listener в наш проект и настраиваем его
**--скрин--**
где 127.0.0.1 - адрес сервера, на котором располагается наша InfluxDB

Установка и настройка Grafana
```bash
# Устанавливаем зависимости
sudo apt install -y adduser libfontconfig1
# Скачиваем и устанавливаем Grafana (загрузка недоступна из России)
wget https://dl.grafana.com/enterprise/release/grafana-enterprise_9.3.4_amd64.deb
# Установка и запуск
sudo dpkg -i grafana-enterprise_9.3.4_amd64.deb
sudo systemctl start grafana-server
```
После успешного старта можно открывать в браузере http://127.0.0.1:3000 , где 127.0.0.1 - адрес хоста, на котором установлена Grafana. Первый вход осуществляется с логином и паролем admin, после входа система попросит ввести новый пароль админа.
Подключаем InfluxDB в качестве источника данных на странице: http://127.0.0.1:3000/datasources





Я приведу в качестве примера 2 своих дашборда под каждый из листенеров Импортируем дашборд из JSON:

<details>
  <summary>Дашборд1</summary>
```bash
{
  "annotations": {
    "list": [
      {
        "builtIn": 1,
        "datasource": "-- Grafana --",
        "enable": true,
        "hide": true,
        "iconColor": "rgba(0, 211, 255, 1)",
        "limit": 100,
        "name": "Annotations & Alerts",
        "showIn": 0,
        "type": "dashboard"
      },
      {
        "datasource": "Performance",
        "enable": false,
        "iconColor": "rgb(23, 255, 0)",
        "name": "Test Start",
        "query": "SELECT * FROM testStartEnd WHERE type='started'",
        "tagsColumn": "type",
        "titleColumn": "testName"
      },
      {
        "datasource": "Performance",
        "enable": false,
        "iconColor": "rgba(255, 96, 96, 1)",
        "name": "Test End",
        "query": "SELECT * FROM testStartEnd WHERE type='finished'",
        "tagsColumn": "type",
        "titleColumn": "testName"
      }
    ]
  },
  "description": "This dashboard shows live load test metrics provided by JMeter.",
  "editable": true,
  "gnetId": 1152,
  "graphTooltip": 1,
  "id": 2,
  "iteration": 1674740053140,
  "links": [],
  "panels": [
    {
      "datasource": null,
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "thresholds"
          },
          "custom": {
            "align": null,
            "filterable": false
          },
          "mappings": [],
          "thresholds": {
            "mode": "absolute",
            "steps": [
              {
                "color": "green",
                "value": null
              },
              {
                "color": "red",
                "value": 80
              }
            ]
          }
        },
        "overrides": [
          {
            "matcher": {
              "id": "byName",
              "options": "nodeName"
            },
            "properties": [
              {
                "id": "custom.width",
                "value": 174
              }
            ]
          },
          {
            "matcher": {
              "id": "byName",
              "options": "ActiveUsers"
            },
            "properties": [
              {
                "id": "custom.width",
                "value": 116
              }
            ]
          },
          {
            "matcher": {
              "id": "byName",
              "options": "Time"
            },
            "properties": [
              {
                "id": "custom.width",
                "value": 144
              }
            ]
          }
        ]
      },
      "gridPos": {
        "h": 6,
        "w": 8,
        "x": 0,
        "y": 0
      },
      "id": 28,
      "options": {
        "showHeader": true,
        "sortBy": [
          {
            "desc": true,
            "displayName": "ActiveUsers"
          }
        ]
      },
      "pluginVersion": "7.5.1",
      "targets": [
        {
          "groupBy": [
            {
              "params": [
                "$__interval"
              ],
              "type": "time"
            },
            {
              "params": [
                "null"
              ],
              "type": "fill"
            }
          ],
          "orderByTime": "ASC",
          "policy": "default",
          "query": "SELECT last(\"startedThreads\") AS ActiveUsers FROM \"virtualUsers\" WHERE $timeFilter GROUP BY nodeName ",
          "rawQuery": true,
          "refId": "A",
          "resultFormat": "table",
          "select": [
            [
              {
                "params": [
                  "value"
                ],
                "type": "field"
              },
              {
                "params": [],
                "type": "mean"
              }
            ]
          ],
          "tags": []
        }
      ],
      "timeFrom": null,
      "timeShift": null,
      "title": "Per Node Users",
      "transparent": true,
      "type": "table"
    },
    {
      "cacheTimeout": null,
      "colorBackground": false,
      "colorValue": false,
      "colors": [
        "rgba(245, 54, 54, 0.9)",
        "rgba(237, 129, 40, 0.89)",
        "rgba(50, 172, 45, 0.97)"
      ],
      "datasource": "Performance",
      "editable": true,
      "error": false,
      "fieldConfig": {
        "defaults": {},
        "overrides": []
      },
      "format": "none",
      "gauge": {
        "maxValue": 100,
        "minValue": 0,
        "show": false,
        "thresholdLabels": false,
        "thresholdMarkers": true
      },
      "gridPos": {
        "h": 5,
        "w": 3,
        "x": 8,
        "y": 0
      },
      "height": "120",
      "id": 14,
      "interval": null,
      "links": [],
      "mappingType": 1,
      "mappingTypes": [
        {
          "name": "value to text",
          "value": 1
        },
        {
          "name": "range to text",
          "value": 2
        }
      ],
      "maxDataPoints": 100,
      "nullPointMode": "connected",
      "nullText": null,
      "postfix": "",
      "postfixFontSize": "50%",
      "prefix": "",
      "prefixFontSize": "50%",
      "rangeMaps": [
        {
          "from": "null",
          "text": "N/A",
          "to": "null"
        }
      ],
      "sparkline": {
        "fillColor": "rgba(31, 118, 189, 0.18)",
        "full": false,
        "lineColor": "rgb(31, 120, 193)",
        "show": false
      },
      "tableColumn": "",
      "targets": [
        {
          "dsType": "influxdb",
          "groupBy": [],
          "measurement": "virtualUsers",
          "orderByTime": "ASC",
          "policy": "default",
          "query": "SELECT sum(\"U\") from (\nSELECT last(\"startedThreads\") AS \"U\" FROM \"virtualUsers\" WHERE $timeFilter GROUP BY nodeName\n)\n",
          "rawQuery": true,
          "refId": "A",
          "resultFormat": "table",
          "select": [
            [
              {
                "params": [
                  "meanActiveThreads"
                ],
                "type": "field"
              }
            ]
          ],
          "tags": []
        }
      ],
      "thresholds": "",
      "title": "Active Users",
      "transparent": true,
      "type": "singlestat",
      "valueFontSize": "80%",
      "valueMaps": [
        {
          "op": "=",
          "text": "N/A",
          "value": "null"
        }
      ],
      "valueName": "current"
    },
    {
      "cacheTimeout": null,
      "colorBackground": false,
      "colorValue": false,
      "colors": [
        "rgba(245, 54, 54, 0.9)",
        "rgba(237, 129, 40, 0.89)",
        "rgba(50, 172, 45, 0.97)"
      ],
      "datasource": "Performance",
      "decimals": 1,
      "editable": true,
      "error": false,
      "fieldConfig": {
        "defaults": {},
        "overrides": []
      },
      "format": "percentunit",
      "gauge": {
        "maxValue": 1,
        "minValue": null,
        "show": true,
        "thresholdLabels": false,
        "thresholdMarkers": true
      },
      "gridPos": {
        "h": 6,
        "w": 5,
        "x": 11,
        "y": 0
      },
      "height": "180",
      "id": 18,
      "interval": null,
      "links": [],
      "mappingType": 1,
      "mappingTypes": [
        {
          "name": "value to text",
          "value": 1
        },
        {
          "name": "range to text",
          "value": 2
        }
      ],
      "maxDataPoints": 100,
      "nullPointMode": "connected",
      "nullText": null,
      "postfix": "",
      "postfixFontSize": "30%",
      "prefix": "",
      "prefixFontSize": "50%",
      "rangeMaps": [
        {
          "from": "null",
          "text": "N/A",
          "to": "null"
        }
      ],
      "sparkline": {
        "fillColor": "rgba(31, 118, 189, 0.18)",
        "full": false,
        "lineColor": "rgb(31, 120, 193)",
        "show": false
      },
      "tableColumn": "",
      "targets": [
        {
          "dsType": "influxdb",
          "groupBy": [],
          "measurement": "virtualUsers",
          "policy": "default",
          "query": "SELECT 1 - sum(\"errorCount\")/count(\"responseTime\") FROM \"requestsRaw\" WHERE $timeFilter",
          "rawQuery": true,
          "refId": "A",
          "resultFormat": "time_series",
          "select": [
            [
              {
                "params": [
                  "finishedThreads"
                ],
                "type": "field"
              }
            ]
          ],
          "tags": []
        }
      ],
      "thresholds": "-1,-1",
      "title": "Success Rate",
      "transparent": true,
      "type": "singlestat",
      "valueFontSize": "80%",
      "valueMaps": [
        {
          "op": "=",
          "text": "N/A",
          "value": "null"
        }
      ],
      "valueName": "current"
    },
    {
      "cacheTimeout": null,
      "colorBackground": false,
      "colorValue": false,
      "colors": [
        "rgba(245, 54, 54, 0.9)",
        "rgba(237, 129, 40, 0.89)",
        "rgba(50, 172, 45, 0.97)"
      ],
      "datasource": "Performance",
      "decimals": 0,
      "editable": true,
      "error": false,
      "fieldConfig": {
        "defaults": {},
        "overrides": []
      },
      "format": "none",
      "gauge": {
        "maxValue": 100,
        "minValue": 0,
        "show": false,
        "thresholdLabels": false,
        "thresholdMarkers": true
      },
      "gridPos": {
        "h": 5,
        "w": 4,
        "x": 16,
        "y": 0
      },
      "height": "120",
      "id": 20,
      "interval": null,
      "links": [],
      "mappingType": 1,
      "mappingTypes": [
        {
          "name": "value to text",
          "value": 1
        },
        {
          "name": "range to text",
          "value": 2
        }
      ],
      "maxDataPoints": 100,
      "nullPointMode": "connected",
      "nullText": null,
      "postfix": "",
      "postfixFontSize": "30%",
      "prefix": "",
      "prefixFontSize": "50%",
      "rangeMaps": [
        {
          "from": "null",
          "text": "N/A",
          "to": "null"
        }
      ],
      "sparkline": {
        "fillColor": "rgba(31, 118, 189, 0.18)",
        "full": false,
        "lineColor": "rgb(31, 120, 193)",
        "show": false
      },
      "tableColumn": "",
      "targets": [
        {
          "dsType": "influxdb",
          "groupBy": [],
          "measurement": "virtualUsers",
          "policy": "default",
          "query": "SELECT  count(\"responseTime\") FROM \"requestsRaw\" WHERE $timeFilter",
          "rawQuery": true,
          "refId": "A",
          "resultFormat": "time_series",
          "select": [
            [
              {
                "params": [
                  "finishedThreads"
                ],
                "type": "field"
              }
            ]
          ],
          "tags": []
        }
      ],
      "thresholds": "",
      "title": "Request Count",
      "transparent": true,
      "type": "singlestat",
      "valueFontSize": "80%",
      "valueMaps": [
        {
          "op": "=",
          "text": "N/A",
          "value": "null"
        }
      ],
      "valueName": "current"
    },
    {
      "cacheTimeout": null,
      "colorBackground": false,
      "colorValue": false,
      "colors": [
        "rgb(109, 109, 109)",
        "rgba(237, 129, 40, 0.89)",
        "rgba(126, 0, 0, 0.9)"
      ],
      "datasource": "Performance",
      "decimals": 2,
      "editable": true,
      "error": false,
      "fieldConfig": {
        "defaults": {},
        "overrides": []
      },
      "format": "percentunit",
      "gauge": {
        "maxValue": 1,
        "minValue": null,
        "show": true,
        "thresholdLabels": false,
        "thresholdMarkers": true
      },
      "gridPos": {
        "h": 6,
        "w": 4,
        "x": 20,
        "y": 0
      },
      "height": "180",
      "id": 21,
      "interval": null,
      "links": [],
      "mappingType": 1,
      "mappingTypes": [
        {
          "name": "value to text",
          "value": 1
        },
        {
          "name": "range to text",
          "value": 2
        }
      ],
      "maxDataPoints": 100,
      "nullPointMode": "connected",
      "nullText": null,
      "postfix": "",
      "postfixFontSize": "30%",
      "prefix": "",
      "prefixFontSize": "50%",
      "rangeMaps": [
        {
          "from": "null",
          "text": "N/A",
          "to": "null"
        }
      ],
      "sparkline": {
        "fillColor": "rgba(31, 118, 189, 0.18)",
        "full": false,
        "lineColor": "rgb(31, 120, 193)",
        "show": false
      },
      "tableColumn": "",
      "targets": [
        {
          "dsType": "influxdb",
          "groupBy": [],
          "measurement": "virtualUsers",
          "policy": "default",
          "query": "SELECT sum(\"errorCount\")/count(\"responseTime\") FROM \"requestsRaw\" WHERE $timeFilter",
          "rawQuery": true,
          "refId": "A",
          "resultFormat": "time_series",
          "select": [
            [
              {
                "params": [
                  "finishedThreads"
                ],
                "type": "field"
              }
            ]
          ],
          "tags": []
        }
      ],
      "thresholds": "0.01,0.1",
      "title": "Error Rate",
      "transparent": true,
      "type": "singlestat",
      "valueFontSize": "80%",
      "valueMaps": [
        {
          "op": "=",
          "text": "N/A",
          "value": "null"
        }
      ],
      "valueName": "current"
    },
    {
      "aliasColors": {},
      "bars": false,
      "dashLength": 10,
      "dashes": false,
      "datasource": "Performance",
      "editable": true,
      "error": false,
      "fieldConfig": {
        "defaults": {
          "links": []
        },
        "overrides": []
      },
      "fill": 1,
      "fillGradient": 0,
      "grid": {},
      "gridPos": {
        "h": 8,
        "w": 24,
        "x": 0,
        "y": 6
      },
      "height": "300",
      "hiddenSeries": false,
      "id": 6,
      "legend": {
        "avg": false,
        "current": false,
        "max": false,
        "min": false,
        "show": true,
        "total": false,
        "values": false
      },
      "lines": true,
      "linewidth": 1,
      "links": [],
      "nullPointMode": "null",
      "options": {
        "alertThreshold": true
      },
      "percentage": false,
      "pluginVersion": "7.5.1",
      "pointradius": 5,
      "points": false,
      "renderer": "flot",
      "seriesOverrides": [
        {
          "$$hashKey": "object:551",
          "alias": "Throughput",
          "yaxis": 2
        }
      ],
      "spaceLength": 10,
      "stack": false,
      "steppedLine": false,
      "targets": [
        {
          "alias": "Num Users",
          "dsType": "influxdb",
          "groupBy": [
            {
              "params": [
                "10s"
              ],
              "type": "time"
            },
            {
              "params": [
                "null"
              ],
              "type": "fill"
            }
          ],
          "hide": false,
          "measurement": "virtualUsers",
          "orderByTime": "ASC",
          "policy": "default",
          "query": "SELECT sum(\"U\") from (\nSELECT mean(\"startedThreads\") AS \"U\" FROM \"virtualUsers\" WHERE $timeFilter GROUP BY nodeName\n)\nGROUP BY time(10s) \n \n",
          "rawQuery": true,
          "refId": "A",
          "resultFormat": "time_series",
          "select": [
            [
              {
                "params": [
                  "startedThreads"
                ],
                "type": "field"
              },
              {
                "params": [],
                "type": "mean"
              }
            ]
          ],
          "tags": []
        },
        {
          "alias": "Throughput",
          "dsType": "influxdb",
          "groupBy": [
            {
              "params": [
                "10s"
              ],
              "type": "time"
            },
            {
              "params": [
                "null"
              ],
              "type": "fill"
            }
          ],
          "hide": false,
          "measurement": "requestsRaw",
          "orderByTime": "ASC",
          "policy": "default",
          "query": "SELECT  count(\"responseTime\")/$aggregation FROM \"requestsRaw\" WHERE $timeFilter GROUP BY time([[aggregation]]s) fill(null)",
          "rawQuery": true,
          "refId": "B",
          "resultFormat": "time_series",
          "select": [
            [
              {
                "params": [
                  "responseTime"
                ],
                "type": "field"
              },
              {
                "params": [],
                "type": "count"
              }
            ]
          ],
          "tags": []
        }
      ],
      "thresholds": [],
      "timeFrom": null,
      "timeRegions": [],
      "timeShift": null,
      "title": "Load",
      "tooltip": {
        "msResolution": false,
        "shared": true,
        "sort": 0,
        "value_type": "individual"
      },
      "type": "graph",
      "xaxis": {
        "buckets": null,
        "mode": "time",
        "name": null,
        "show": true,
        "values": []
      },
      "yaxes": [
        {
          "format": "short",
          "label": null,
          "logBase": 1,
          "max": null,
          "min": null,
          "show": true
        },
        {
          "format": "ops",
          "label": "",
          "logBase": 1,
          "max": null,
          "min": null,
          "show": true
        }
      ],
      "yaxis": {
        "align": false,
        "alignLevel": null
      }
    },
    {
      "aliasColors": {
        "95 perc": "#F9934E",
        "99 perc": "#E24D42",
        "Max": "#BF1B00"
      },
      "bars": false,
      "dashLength": 10,
      "dashes": false,
      "datasource": "Performance",
      "editable": true,
      "error": false,
      "fieldConfig": {
        "defaults": {
          "links": []
        },
        "overrides": []
      },
      "fill": 3,
      "fillGradient": 0,
      "grid": {},
      "gridPos": {
        "h": 9,
        "w": 24,
        "x": 0,
        "y": 14
      },
      "height": "300",
      "hiddenSeries": false,
      "id": 1,
      "legend": {
        "alignAsTable": false,
        "avg": false,
        "current": false,
        "max": false,
        "min": false,
        "show": true,
        "total": false,
        "values": false
      },
      "lines": true,
      "linewidth": 1,
      "links": [],
      "nullPointMode": "null",
      "options": {
        "alertThreshold": true
      },
      "percentage": false,
      "pluginVersion": "7.5.1",
      "pointradius": 5,
      "points": false,
      "renderer": "flot",
      "seriesOverrides": [
        {
          "$$hashKey": "object:228",
          "alias": "Mean",
          "zindex": 3
        },
        {
          "$$hashKey": "object:229",
          "alias": "90 perc",
          "zindex": 2
        },
        {
          "$$hashKey": "object:230",
          "alias": "95 perc",
          "zindex": 1
        },
        {
          "$$hashKey": "object:231",
          "alias": "99 perc",
          "zindex": 0
        },
        {
          "$$hashKey": "object:232",
          "alias": "Max",
          "zindex": -1
        }
      ],
      "spaceLength": 10,
      "stack": false,
      "steppedLine": false,
      "targets": [
        {
          "alias": "",
          "dsType": "influxdb",
          "groupBy": [
            {
              "params": [
                "$aggregation"
              ],
              "type": "time"
            },
            {
              "params": [
                "null"
              ],
              "type": "fill"
            }
          ],
          "measurement": "requestsRaw",
          "orderByTime": "ASC",
          "policy": "default",
          "query": "SELECT mean(\"responseTime\") FROM \"requestsRaw\" WHERE  $timeFilter GROUP BY requestName , time([[aggregation]]s) fill(null)",
          "rawQuery": true,
          "refId": "E",
          "resultFormat": "time_series",
          "select": [
            [
              {
                "params": [
                  "responseTime"
                ],
                "type": "field"
              },
              {
                "params": [],
                "type": "mean"
              }
            ]
          ],
          "tags": [
            {
              "key": "requestName",
              "operator": "=~",
              "value": "/^$request$/"
            }
          ]
        }
      ],
      "thresholds": [],
      "timeFrom": null,
      "timeRegions": [],
      "timeShift": null,
      "title": "Response Times (mean)",
      "tooltip": {
        "msResolution": true,
        "shared": true,
        "sort": 2,
        "value_type": "individual"
      },
      "transformations": [
        {
          "id": "renameByRegex",
          "options": {
            "regex": "requestsRaw.mean {requestName: ",
            "renamePattern": ""
          }
        },
        {
          "id": "renameByRegex",
          "options": {
            "regex": "}",
            "renamePattern": ""
          }
        }
      ],
      "type": "graph",
      "xaxis": {
        "buckets": null,
        "mode": "time",
        "name": null,
        "show": true,
        "values": []
      },
      "yaxes": [
        {
          "format": "ms",
          "label": null,
          "logBase": 1,
          "max": null,
          "min": null,
          "show": true
        },
        {
          "format": "short",
          "label": null,
          "logBase": 1,
          "max": null,
          "min": null,
          "show": true
        }
      ],
      "yaxis": {
        "align": false,
        "alignLevel": null
      }
    },
    {
      "datasource": "Performance",
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "thresholds"
          },
          "custom": {
            "align": null,
            "filterable": false
          },
          "decimals": 2,
          "mappings": [],
          "thresholds": {
            "mode": "absolute",
            "steps": [
              {
                "color": "rgba(50, 172, 45, 0.97)",
                "value": null
              },
              {
                "color": "rgba(237, 129, 40, 0.89)",
                "value": 0
              },
              {
                "color": "rgba(245, 54, 54, 0.9)",
                "value": 0
              }
            ]
          },
          "unit": "short"
        },
        "overrides": [
          {
            "matcher": {
              "id": "byName",
              "options": "Time"
            },
            "properties": [
              {
                "id": "unit",
                "value": "time: YYYY-MM-DD HH:mm:ss"
              },
              {
                "id": "custom.align",
                "value": null
              }
            ]
          },
          {
            "matcher": {
              "id": "byName",
              "options": "requestName"
            },
            "properties": [
              {
                "id": "custom.width",
                "value": 301
              }
            ]
          },
          {
            "matcher": {
              "id": "byName",
              "options": "Error Rate"
            },
            "properties": [
              {
                "id": "custom.displayMode",
                "value": "color-text"
              },
              {
                "id": "unit",
                "value": "percentunit"
              }
            ]
          },
          {
            "matcher": {
              "id": "byName",
              "options": "Count"
            },
            "properties": [
              {
                "id": "custom.width",
                "value": 122
              }
            ]
          },
          {
            "matcher": {
              "id": "byName",
              "options": "Min"
            },
            "properties": [
              {
                "id": "custom.width",
                "value": 109
              }
            ]
          },
          {
            "matcher": {
              "id": "byName",
              "options": "Max"
            },
            "properties": [
              {
                "id": "custom.width",
                "value": 135
              }
            ]
          }
        ]
      },
      "gridPos": {
        "h": 11,
        "w": 24,
        "x": 0,
        "y": 23
      },
      "id": 9,
      "links": [],
      "options": {
        "showHeader": true,
        "sortBy": [
          {
            "desc": true,
            "displayName": "Mean"
          }
        ]
      },
      "pluginVersion": "7.5.1",
      "targets": [
        {
          "alias": "",
          "dsType": "influxdb",
          "groupBy": [
            {
              "params": [
                "$interval"
              ],
              "type": "time"
            },
            {
              "params": [
                "null"
              ],
              "type": "fill"
            }
          ],
          "hide": false,
          "measurement": "requests",
          "orderByTime": "ASC",
          "policy": "default",
          "query": "SELECT count(responseTime) as Count,  min(responseTime) as Min,  max(responseTime) as Max, mean(responseTime) as Mean,  percentile(responseTime, 90) as \"90%\", percentile(responseTime, 99) as \"99%\",(sum(errorCount)/count(responseTime)) as \"Error Rate\" FROM \"requestsRaw\"  WHERE $timeFilter GROUP BY requestName",
          "rawQuery": true,
          "refId": "A",
          "resultFormat": "table",
          "select": [
            [
              {
                "params": [
                  "value"
                ],
                "type": "field"
              },
              {
                "params": [],
                "type": "mean"
              }
            ]
          ],
          "tags": []
        }
      ],
      "title": "Metrics Overview",
      "transformations": [
        {
          "id": "merge",
          "options": {
            "reducers": []
          }
        },
        {
          "id": "organize",
          "options": {
            "excludeByName": {
              "Time": true
            },
            "indexByName": {},
            "renameByName": {}
          }
        }
      ],
      "type": "table"
    },
    {
      "aliasColors": {
        "95 perc": "#F9934E",
        "99 perc": "#E24D42",
        "Max": "#BF1B00"
      },
      "bars": false,
      "dashLength": 10,
      "dashes": false,
      "datasource": "Performance",
      "editable": true,
      "error": false,
      "fieldConfig": {
        "defaults": {
          "links": []
        },
        "overrides": []
      },
      "fill": 3,
      "fillGradient": 0,
      "grid": {},
      "gridPos": {
        "h": 8,
        "w": 12,
        "x": 0,
        "y": 34
      },
      "height": "300",
      "hiddenSeries": false,
      "id": 24,
      "legend": {
        "alignAsTable": false,
        "avg": false,
        "current": false,
        "max": false,
        "min": false,
        "show": true,
        "total": false,
        "values": false
      },
      "lines": true,
      "linewidth": 1,
      "links": [],
      "nullPointMode": "null",
      "options": {
        "alertThreshold": true
      },
      "percentage": false,
      "pluginVersion": "7.5.1",
      "pointradius": 5,
      "points": false,
      "renderer": "flot",
      "seriesOverrides": [
        {
          "alias": "Mean",
          "zindex": 3
        },
        {
          "alias": "90 perc",
          "zindex": 2
        },
        {
          "alias": "95 perc",
          "zindex": 1
        },
        {
          "alias": "99 perc",
          "zindex": 0
        },
        {
          "alias": "Max",
          "zindex": -1
        }
      ],
      "spaceLength": 10,
      "stack": false,
      "steppedLine": false,
      "targets": [
        {
          "alias": "Mean",
          "dsType": "influxdb",
          "groupBy": [
            {
              "params": [
                "$aggregation"
              ],
              "type": "time"
            },
            {
              "params": [
                "null"
              ],
              "type": "fill"
            }
          ],
          "hide": true,
          "measurement": "requestsRaw",
          "orderByTime": "ASC",
          "policy": "default",
          "query": "SELECT mean(\"responseTime\") FROM \"requestsRaw\" WHERE  $timeFilter GROUP BY requestName , time([[aggregation]]s) fill(null)",
          "rawQuery": true,
          "refId": "A",
          "resultFormat": "time_series",
          "select": [
            [
              {
                "params": [
                  "responseTime"
                ],
                "type": "field"
              },
              {
                "params": [],
                "type": "mean"
              }
            ]
          ],
          "tags": [
            {
              "key": "requestName",
              "operator": "=~",
              "value": "/^$request$/"
            }
          ]
        },
        {
          "alias": "90 perc",
          "dsType": "influxdb",
          "groupBy": [
            {
              "params": [
                "$aggregation"
              ],
              "type": "time"
            },
            {
              "params": [
                "null"
              ],
              "type": "fill"
            }
          ],
          "hide": true,
          "measurement": "requestsRaw",
          "orderByTime": "ASC",
          "policy": "default",
          "query": "SELECT percentile(\"responseTime\", 90) FROM \"requestsRaw\" WHERE \"requestName\" =~ /^$request$/ AND $timeFilter GROUP BY time([[aggregation]]s) fill(null)",
          "rawQuery": true,
          "refId": "B",
          "resultFormat": "time_series",
          "select": [
            [
              {
                "params": [
                  "responseTime"
                ],
                "type": "field"
              },
              {
                "params": [
                  "90"
                ],
                "type": "percentile"
              }
            ]
          ],
          "tags": [
            {
              "key": "requestName",
              "operator": "=~",
              "value": "/^$request$/"
            }
          ]
        },
        {
          "alias": "95 perc",
          "dsType": "influxdb",
          "groupBy": [
            {
              "params": [
                "$aggregation"
              ],
              "type": "time"
            },
            {
              "params": [
                "null"
              ],
              "type": "fill"
            }
          ],
          "hide": false,
          "measurement": "requestsRaw",
          "orderByTime": "ASC",
          "policy": "default",
          "query": "SELECT percentile(\"responseTime\", 95) FROM \"requestsRaw\" WHERE $timeFilter GROUP BY time([[aggregation]]s) fill(null)",
          "rawQuery": true,
          "refId": "D",
          "resultFormat": "time_series",
          "select": [
            [
              {
                "params": [
                  "responseTime"
                ],
                "type": "field"
              },
              {
                "params": [
                  95
                ],
                "type": "percentile"
              }
            ]
          ],
          "tags": [
            {
              "key": "requestName",
              "operator": "=~",
              "value": "/^$request$/"
            }
          ]
        },
        {
          "alias": "99 perc",
          "dsType": "influxdb",
          "groupBy": [
            {
              "params": [
                "$aggregation"
              ],
              "type": "time"
            },
            {
              "params": [
                "null"
              ],
              "type": "fill"
            }
          ],
          "hide": true,
          "measurement": "requestsRaw",
          "orderByTime": "ASC",
          "policy": "default",
          "query": "SELECT percentile(\"responseTime\", 99) FROM \"requestsRaw\" WHERE \"requestName\" =~ /^$request$/ AND $timeFilter GROUP BY time([[aggregation]]s) fill(null)",
          "rawQuery": true,
          "refId": "C",
          "resultFormat": "time_series",
          "select": [
            [
              {
                "params": [
                  "responseTime"
                ],
                "type": "field"
              },
              {
                "params": [
                  "99"
                ],
                "type": "percentile"
              }
            ]
          ],
          "tags": [
            {
              "key": "requestName",
              "operator": "=~",
              "value": "/^$request$/"
            }
          ]
        },
        {
          "alias": "Max",
          "dsType": "influxdb",
          "groupBy": [
            {
              "params": [
                "$aggregation"
              ],
              "type": "time"
            },
            {
              "params": [
                "null"
              ],
              "type": "fill"
            }
          ],
          "hide": true,
          "measurement": "requestsRaw",
          "orderByTime": "ASC",
          "policy": "default",
          "query": "SELECT max(\"responseTime\") FROM \"requestsRaw\" WHERE \"requestName\" =~ /^$request$/ AND $timeFilter GROUP BY time([[aggregation]]s) fill(null)",
          "rawQuery": true,
          "refId": "E",
          "resultFormat": "time_series",
          "select": [
            [
              {
                "params": [
                  "responseTime"
                ],
                "type": "field"
              },
              {
                "params": [],
                "type": "max"
              }
            ]
          ],
          "tags": [
            {
              "key": "requestName",
              "operator": "=~",
              "value": "/^$request$/"
            }
          ]
        }
      ],
      "thresholds": [],
      "timeFrom": null,
      "timeRegions": [],
      "timeShift": null,
      "title": "Response Times for some req",
      "tooltip": {
        "msResolution": true,
        "shared": true,
        "sort": 0,
        "value_type": "individual"
      },
      "type": "graph",
      "xaxis": {
        "buckets": null,
        "mode": "time",
        "name": null,
        "show": true,
        "values": []
      },
      "yaxes": [
        {
          "format": "ms",
          "label": null,
          "logBase": 1,
          "max": null,
          "min": null,
          "show": true
        },
        {
          "format": "short",
          "label": null,
          "logBase": 1,
          "max": null,
          "min": null,
          "show": true
        }
      ],
      "yaxis": {
        "align": false,
        "alignLevel": null
      }
    },
    {
      "aliasColors": {},
      "bars": false,
      "dashLength": 10,
      "dashes": false,
      "datasource": "Performance",
      "editable": true,
      "error": false,
      "fieldConfig": {
        "defaults": {
          "links": []
        },
        "overrides": []
      },
      "fill": 1,
      "fillGradient": 0,
      "grid": {},
      "gridPos": {
        "h": 8,
        "w": 12,
        "x": 12,
        "y": 34
      },
      "height": "300px",
      "hiddenSeries": false,
      "id": 26,
      "legend": {
        "alignAsTable": true,
        "avg": true,
        "current": true,
        "max": true,
        "min": true,
        "show": true,
        "total": false,
        "values": true
      },
      "lines": true,
      "linewidth": 1,
      "links": [],
      "nullPointMode": "connected",
      "options": {
        "alertThreshold": true
      },
      "percentage": false,
      "pluginVersion": "7.5.1",
      "pointradius": 5,
      "points": false,
      "renderer": "flot",
      "seriesOverrides": [],
      "spaceLength": 10,
      "stack": false,
      "steppedLine": false,
      "targets": [
        {
          "alias": "Throughput",
          "dsType": "influxdb",
          "groupBy": [
            {
              "params": [
                "10s"
              ],
              "type": "time"
            },
            {
              "params": [
                "null"
              ],
              "type": "fill"
            }
          ],
          "measurement": "requestsRaw",
          "orderByTime": "ASC",
          "policy": "default",
          "query": "SELECT count(\"responseTime\")/$aggregation FROM \"requestsRaw\" WHERE $timeFilter GROUP BY time([[aggregation]]s) fill(null)",
          "rawQuery": true,
          "refId": "A",
          "resultFormat": "time_series",
          "select": [
            [
              {
                "params": [
                  "responseTime"
                ],
                "type": "field"
              },
              {
                "params": [],
                "type": "count"
              }
            ]
          ],
          "tags": [
            {
              "key": "requestName",
              "operator": "=~",
              "value": "/^$request$/"
            }
          ]
        }
      ],
      "thresholds": [],
      "timeFrom": null,
      "timeRegions": [],
      "timeShift": null,
      "title": "Throughput",
      "tooltip": {
        "msResolution": false,
        "shared": true,
        "sort": 0,
        "value_type": "individual"
      },
      "type": "graph",
      "xaxis": {
        "buckets": null,
        "mode": "time",
        "name": null,
        "show": true,
        "values": []
      },
      "yaxes": [
        {
          "format": "ops",
          "label": "",
          "logBase": 1,
          "max": null,
          "min": null,
          "show": true
        },
        {
          "format": "short",
          "label": null,
          "logBase": 1,
          "max": null,
          "min": null,
          "show": true
        }
      ],
      "yaxis": {
        "align": false,
        "alignLevel": null
      }
    },
    {
      "aliasColors": {},
      "bars": false,
      "dashLength": 10,
      "dashes": false,
      "datasource": "Performance",
      "editable": true,
      "error": false,
      "fieldConfig": {
        "defaults": {
          "links": []
        },
        "overrides": []
      },
      "fill": 1,
      "fillGradient": 0,
      "grid": {},
      "gridPos": {
        "h": 11,
        "w": 24,
        "x": 0,
        "y": 42
      },
      "height": "300",
      "hiddenSeries": false,
      "id": 11,
      "legend": {
        "alignAsTable": true,
        "avg": false,
        "current": false,
        "max": false,
        "min": false,
        "show": true,
        "sort": "total",
        "sortDesc": true,
        "total": true,
        "values": true
      },
      "lines": true,
      "linewidth": 2,
      "links": [],
      "nullPointMode": "connected",
      "options": {
        "alertThreshold": true
      },
      "percentage": false,
      "pluginVersion": "7.5.1",
      "pointradius": 5,
      "points": false,
      "renderer": "flot",
      "seriesOverrides": [],
      "spaceLength": 10,
      "stack": false,
      "steppedLine": false,
      "targets": [
        {
          "alias": "",
          "groupBy": [
            {
              "params": [
                "$__interval"
              ],
              "type": "time"
            },
            {
              "params": [
                "null"
              ],
              "type": "fill"
            }
          ],
          "orderByTime": "ASC",
          "policy": "default",
          "query": "SELECT sum(\"errorCount\")/$aggregation as errorRate FROM \"requestsRaw\" WHERE  $timeFilter GROUP BY requestName ,  time([[aggregation]]s) fill(null) ",
          "rawQuery": true,
          "refId": "H",
          "resultFormat": "time_series",
          "select": [
            [
              {
                "params": [
                  "value"
                ],
                "type": "field"
              },
              {
                "params": [],
                "type": "mean"
              }
            ]
          ],
          "tags": []
        }
      ],
      "thresholds": [],
      "timeFrom": null,
      "timeRegions": [],
      "timeShift": null,
      "title": "Error Rate",
      "tooltip": {
        "msResolution": false,
        "shared": true,
        "sort": 0,
        "value_type": "cumulative"
      },
      "transformations": [
        {
          "id": "renameByRegex",
          "options": {
            "regex": "requestsRaw.errorRate {requestName: ",
            "renamePattern": ""
          }
        },
        {
          "id": "renameByRegex",
          "options": {
            "regex": "}",
            "renamePattern": ""
          }
        }
      ],
      "type": "graph",
      "xaxis": {
        "buckets": null,
        "mode": "time",
        "name": null,
        "show": true,
        "values": []
      },
      "yaxes": [
        {
          "format": "short",
          "label": "errors / s",
          "logBase": 1,
          "max": null,
          "min": "0",
          "show": true
        },
        {
          "format": "short",
          "label": "",
          "logBase": 1,
          "max": null,
          "min": null,
          "show": true
        }
      ],
      "yaxis": {
        "align": false,
        "alignLevel": null
      }
    }
  ],
  "refresh": false,
  "schemaVersion": 27,
  "style": "dark",
  "tags": [],
  "templating": {
    "list": [
      {
        "allValue": null,
        "current": {
          "selected": true,
          "text": [
            "All"
          ],
          "value": [
            "$__all"
          ]
        },
        "datasource": "Performance",
        "definition": "",
        "description": null,
        "error": null,
        "hide": 0,
        "includeAll": true,
        "label": "Request",
        "multi": true,
        "name": "request",
        "options": [],
        "query": "SHOW TAG VALUES FROM \"requestsRaw\" WITH KEY = \"requestName\"",
        "refresh": 1,
        "regex": "",
        "skipUrlSync": false,
        "sort": 0,
        "tagValuesQuery": null,
        "tags": [],
        "tagsQuery": null,
        "type": "query",
        "useTags": false
      },
      {
        "allValue": null,
        "auto": false,
        "current": {
          "selected": false,
          "text": "10s",
          "value": "10"
        },
        "datasource": null,
        "description": null,
        "error": null,
        "hide": 0,
        "includeAll": false,
        "label": "Aggregation Interval",
        "multi": false,
        "name": "aggregation",
        "options": [
          {
            "selected": false,
            "text": "1s",
            "value": "1"
          },
          {
            "selected": true,
            "text": "10s",
            "value": "10"
          },
          {
            "selected": false,
            "text": "30s",
            "value": "30"
          },
          {
            "selected": false,
            "text": "1m",
            "value": "60"
          },
          {
            "selected": false,
            "text": "10m",
            "value": "600"
          },
          {
            "selected": false,
            "text": "30m",
            "value": "1800"
          },
          {
            "selected": false,
            "text": "1h",
            "value": "3600"
          }
        ],
        "query": "1,10,30,60,600,1800,3600",
        "queryValue": "",
        "refresh": 0,
        "skipUrlSync": false,
        "type": "custom"
      }
    ]
  },
  "time": {
    "from": "now-15m",
    "to": "now"
  },
  "timepicker": {
    "refresh_intervals": [
      "10s",
      "30s",
      "1m",
      "5m",
      "15m",
      "30m",
      "1h",
      "2h",
      "1d"
    ],
    "time_options": [
      "5m",
      "15m",
      "1h",
      "6h",
      "12h",
      "24h",
      "2d",
      "7d",
      "30d"
    ]
  },
  "timezone": "browser",
  "title": "JMeter Load Test",
  "uid": "qyJj1xIiz",
  "version": 31
}
```
  
</details>

