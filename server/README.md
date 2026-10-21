# Сервер

Все серверные модули находятся внутри `server/`. Это одно приложение, один процесс
и один исполняемый `app.jar`. Пакеты и имена существующих классов сохранены.
Изменения Kotlin-кода относятся к обработке ошибок и валидации входных данных.

| Gradle-модуль | Содержимое |
| --- | --- |
| `:server` | Ktor, HTTP-маршруты, обработка ошибок, DI и запуск приложения |
| `:server:core` | Модели чатов и пользователей, DTO, контракты репозиториев, ошибки |
| `:server:business` | `ChatService`, `ModelService` |
| `:server:inference-api` | Контракт `InferenceEngine` |
| `:server:inference-triton` | HTTP/gRPC-клиенты Triton, адаптер и генерация protobuf |
| `:server:tokenizer` | Контракт токенайзера, реестр и адаптер Hugging Face |
| `:server:persistence-postgres` | Exposed, PostgreSQL, подключение к БД и миграции Flyway |

Зависимости направлены от `server` к реализации и бизнес-логике;
`business` использует `core`, `inference-api` и `tokenizer`;
`inference-triton` использует `inference-api`, который использует `core`;
`tokenizer` и `persistence-postgres` используют `core`.
Циклических зависимостей между Gradle-модулями нет.

Существующие связи внутри классов оставлены: `ChatService` сам открывает Exposed-транзакции,
`ModelService` использует конкретный `TokenizerRegistry`. Поэтому `business` зависит
от Exposed JDBC. В `User.kt` сохранён прежний импорт `io.ktor.util.Hash`, поэтому
у `core` остаётся зависимость компиляции на Ktor. Удаление этих связей требует
отдельного изменения исходных классов.

Команды из корня проекта:

```shell
./gradlew :server:test :server:tokenizer:test :server:inference-triton:test
./gradlew :server:buildFatJar
java -jar server/build/libs/app.jar
```

При запуске по-прежнему необходимы PostgreSQL, Triton и файлы токенайзеров.
Примеры запросов находятся в `requests/server.http` и `requests/triton.http`
относительно корня проекта. Текущий маршрут отправки сообщения:
`POST /chat/chat/messages`. Ответ содержит идентификаторы узлов, текст ответа
сохраняется в БД согласно существующему контракту.

## Ошибки

HTTP-ответ имеет формат `{"code":"...","message":"..."}`.

| HTTP | Код |
| --- | --- |
| 400 | `invalid_request` |
| 404 | `chat_not_found`, `message_not_found`, `model_not_found` |
| 502 | `inference_failed` |
| 503 | `inference_unavailable` |
| 504 | `inference_timeout` |
| 500 | `internal_error` |

Транспортные ошибки переводятся в ошибки сервиса в адаптере Triton.
Внутренние сообщения сбоев не отправляются клиенту, отмена coroutine пробрасывается.
Невалидный JSON и UUID дают 400; отсутствие `chatId` позволяет создать новый чат.

## Docker

Из корня проекта, с заполненным `docker/.env`:

```shell
docker compose -f docker/docker-compose.yaml up -d --build
docker compose -f docker/docker-compose.yaml ps
curl --fail http://localhost:8080/healthy
curl --fail http://localhost:8080/model/get-available
```

Dockerfile собирает все серверные Gradle-модули внутри Linux и копирует
`server/build/libs/app.jar` в runtime-образ. Локальная сборка JAR не требуется.
Gradle-кэш сохраняется через BuildKit между сборками.
Сервис `server` ждёт готовности PostgreSQL, применяет миграции при запуске
и перезапускается по политике `unless-stopped`. Его healthcheck проверяет `/healthy`.
Доступность моделей проверяется отдельно через `/model/get-available`.

`DATABASE_URL` в Compose указывает на сервис `postgres` и БД `POSTGRES_DB`.
`MODEL_LOCAL_DIR` из `.env` должен указывать на реальный репозиторий моделей;
относительный путь отсчитывается от каталога `docker/`.
Без загруженных моделей сервер запускается, но генерация недоступна.

```shell
docker compose -f docker/docker-compose.yaml logs --tail=100 server
# Остановка без удаления данных PostgreSQL:
docker compose -f docker/docker-compose.yaml down
```
