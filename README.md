# My Blog Back App

Backend для блог-платформы, который хорошо показывает мой фундамент как backend-разработчика: чистое слоистое приложение, аккуратная работа с SQL и PostgreSQL, предсказуемые REST-контракты и полноценная тестовая стратегия. Это компактный проект по масштабу, но очень показательный с точки зрения инженерной дисциплины.

## Технологии

- Java 21
- Spring Boot 3.5.7
- Spring MVC, Bean Validation
- Spring Data JDBC, PostgreSQL
- Log4j2
- Maven
- JUnit 5, Spring Boot Test, MockMvc, Testcontainers

## Почему проект важен

- Показывает, что я умею строить аккуратные REST API без лишней магии и хаотичной бизнес-логики.
- Демонстрирует уверенную работу с SQL и Spring Data JDBC, когда нужен больший контроль над persistence-слоем.
- Подчеркивает тестовую зрелость: unit, controller, repository и integration уровни.
- Хорошо раскрывает мою базу: layered architecture, validation, exception handling, logging и конфигурация окружения.

## Архитектурные особенности

- Приложение построено как понятный монолит с явным разделением на `web`, `service`, `repository`, `domain`.
- Репозиторный слой реализован через Spring Data JDBC и `NamedParameterJdbcTemplate`, а SQL вынесен в отдельный слой констант, что делает работу с БД прозрачной и контролируемой.
- В сервисном слое есть нормализация данных, проверка инвариантов, обработка edge cases и аккуратная работа с транзакциями.
- В контроллерах и advice-слое выстроена предсказуемая модель REST-ошибок, которую удобно поддерживать и тестировать.

## Что видно по истории проекта

- `module_one_sprint_three_branch`: базовая REST-архитектура, SQL-слой, exception handling, конфигурация и логирование.
- `module_one_sprint_four_branch`: интеграционные тесты, packaging в executable jar и доведение проекта до более зрелого состояния.

## Функциональность

- REST‑бэкенд для фронтенда блога: посты, комментарии, лайки, изображения.
- Хранение данных в PostgreSQL, инициализация схемы через `schema.sql`.
- Конфигурация через Spring Boot и файлы `application.yaml` / переменные окружения.
- Юнит‑ и интеграционные тесты на Spring Boot Test и Testcontainers.

## Требования к окружению

- **JDK 21**
- **Maven 3.9+** (в репозитории есть `mvnw`/`mvnw.cmd` для запуска без установленного Maven)
- Docker — обязателен для интеграционных тестов, использующих Testcontainers.
- PostgreSQL 14+ (локально или в контейнере) для запуска приложения.

## Конфигурация

Основные параметры задаются через переменные окружения, которые читает `src/main/resources/application.yaml`:

| Переменная | Назначение |
| --- | --- |
| `SPRING_DATASOURCE_URL` | JDBC‑строка подключения к PostgreSQL. |
| `SPRING_DATASOURCE_USERNAME` | Пользователь базы данных. |
| `SPRING_DATASOURCE_PASSWORD` | Пароль пользователя базы данных. |
| `SPRING_CORS_ALLOWED_ORIGINS` | Разрешённые источники для CORS (через запятую). |
| `LOGGING_PRACTICUM_LEVEL` | Уровень логирования пакета `ru.practicum`. |

Для локального запуска можно взять значения из `src/main/resources/application-local.yaml` и экспортировать их в оболочке:

```bash
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/postgres?currentSchema=blog_app"
export SPRING_DATASOURCE_USERNAME="postgres"
export SPRING_DATASOURCE_PASSWORD="postgres"
export SPRING_CORS_ALLOWED_ORIGINS="http://localhost"
export LOGGING_PRACTICUM_LEVEL="DEBUG"
```

## Сборка и запуск

### Сборка исполняемого Jar

```bash
./mvnw clean package
```

Готовый Jar лежит в `target/blog-0.0.1-SNAPSHOT.jar` и уже содержит встроенный Tomcat.

### Запуск

Запустите приложение напрямую из Maven или из собранного Jar:

```bash
./mvnw spring-boot:run
# или
java -jar target/blog-0.0.1-SNAPSHOT.jar
```

По умолчанию приложение слушает `http://localhost:8080`.

Для работы приложения локально и использования `application-local.yaml` нужно указать active profile `local`.

## Тестирование

В проекте используются два набора тестов:

- Юнит‑тесты сервисов и контроллеров (JUnit 5, Spring Boot Test, MockMvc).
- Интеграционные тесты репозиториев и REST‑слоя с использованием Testcontainers PostgreSQL.

Команды запуска:

```bash
./mvnw test
./mvnw verify
```

`./mvnw test` запускает только юнит-тесты, а `./mvnw verify` последовательно запускает unit- и integration-тесты (`*IT.java`).

Интеграционные тесты требуют запущенного Docker‑демона, чтобы поднять контейнер PostgreSQL.

## Контракты REST API

Базовый URL: `http://localhost:8080`

Поддерживаются эндпоинты:

- `GET /api/posts?search=&pageNumber=&pageSize=` — список постов;
- `GET /api/posts/{id}` — получение поста;
- `POST /api/posts` — добавление поста;
- `PUT /api/posts/{id}` — редактирование поста;
- `DELETE /api/posts/{id}` — удаление поста;
- `POST /api/posts/{id}/likes` — лайк поста;
- `PUT /api/posts/{id}/image` — загрузка изображения;
- `GET /api/posts/{id}/image` — получение изображения;
- `GET /api/posts/{id}/comments` — список комментариев;
- `GET /api/posts/{id}/comments/{commentId}` — получение комментария;
- `POST /api/posts/{id}/comments` — добавление комментария;
- `PUT /api/posts/{id}/comments/{commentId}` — редактирование комментария;
- `DELETE /api/posts/{id}/comments/{commentId}` — удаление комментария.

Все ответы и запросы — в формате JSON.

## Структура проекта

```text
my-blog-back-app/
├─ mvnw, mvnw.cmd
├─ pom.xml
├─ README.md
├─ src/
│  ├─ main/
│  │  ├─ java/ru/practicum/blog/
│  │  │  ├─ BlogApplication.java
│  │  │  ├─ config/
│  │  │  ├─ domain/
│  │  │  ├─ repository/
│  │  │  ├─ service/
│  │  │  └─ web/
│  │  └─ resources/
│  │     ├─ application.yaml
│  │     ├─ application-local.yaml
│  │     ├─ log4j2.xml
│  │     └─ schema.sql
│  └─ test/
│     ├─ java/ru/practicum/blog/
│     │  ├─ ... тесты ...
│     └─ resources/
│        ├─ application-test.yaml
│        └─ schema.sql
```

Проект готов к локальному запуску как Spring Boot приложение и использует PostgreSQL в качестве постоянного хранилища данных.
