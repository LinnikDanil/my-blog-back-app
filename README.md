# my-blog-back-app — проектная работа спринта 3

Бэкенд приложения‑блога на **Java 21** с использованием **Spring Framework 6.2** (без Spring Boot), 
базой данных **PostgreSQL**, сборкой **Gradle**, деплоем в сервлет‑контейнер **Tomcat**, и покрытием **JUnit 5** тестами.

---

## Содержание

* [Цели проекта](#цели-проекта)
* [Архитектура](#архитектура)
* [Фронтенд: запуск в Docker](#фронтенд-запуск-в-docker)
* [Бэкенд: сборка и запуск](#бэкенд-сборка-и-запуск)
* [Контракты REST API](#контракты-rest-api)
* [Сборка WAR и деплой в Tomcat](#сборка-war-и-деплой-в-tomcat)
* [Тестирование](#тестирование)
* [Структура проекта](#структура-проекта)

---

## Цели проекта

1. Реализовать бэкенд блога на Java 21 и Spring Framework (без Spring Boot).
2. Использовать **PostgreSQL** для хранения постов и комментариев.
3. Обеспечить REST‑интерфейс для фронтенда.
4. Собрать WAR‑файл Gradle’ом и задеплоить его в Tomcat.
5. Покрыть приложение тестами (JUnit 5, Spring Test, WebMvc, H2 для интеграционных тестов).

---

## Архитектура

```
Браузер ⇄ Nginx (frontend, :80) ⇄ Бэкенд (Tomcat, :8080) ⇄ PostgreSQL (БД)
```

* **Фронтенд** работает в контейнере Nginx, отдаёт статические файлы.
* **Бэкенд** — Spring MVC + Spring Data JDBC, развёрнутый в Tomcat.
* **БД** — PostgreSQL.

---

## Фронтенд: запуск в Docker

```bash
cd path/to/frontend

docker compose up -d
```

Проверить:

```bash
docker ps
```

Фронтенд будет доступен по адресу [http://localhost:80/](http://localhost:80/)

Остановить:

```bash
docker compose down
```

---

## Бэкенд: сборка и запуск

Требуется **JDK 21** и **Gradle 8.13** (или другая совместимая версии).

### Команды сборки и запуска тестов

```bash
./gradlew clean build
```

WAR‑файл будет создан в `build/libs/my-blog-back-app.war`

---

## Контракты REST API

Базовый URL: `http://localhost:8080`

Поддерживаются эндпоинты:

* `GET /api/posts?search=&pageNumber=&pageSize=` — список постов;
* `GET /api/posts/{id}` — получение поста;
* `POST /api/posts` — добавление поста;
* `PUT /api/posts/{id}` — редактирование поста;
* `DELETE /api/posts/{id}` — удаление поста;
* `POST /api/posts/{id}/likes` — лайк поста;
* `PUT /api/posts/{id}/image` — загрузка изображения;
* `GET /api/posts/{id}/image` — получение изображения;
* `GET /api/posts/{id}/comments` — список комментариев;
* `GET /api/posts/{id}/comment/{commentId}` — получение комментария;
* `POST /api/posts/{id}/comments` — добавление комментария;
* `PUT /api/posts/{id}/comments/{commentId}` — редактирование комментария;
* `DELETE /api/posts/{id}/comments/{commentId}` — удаление комментария.

Все ответы и запросы — в формате JSON.

---

## Сборка WAR и деплой в Tomcat

1. Соберите проект:

   ```bash
   ./gradlew clean build
   ```
2. Найдите файл `my-blog-back-app.war` в каталоге `build/libs/`.
3. Скопируйте его в каталог Tomcat:

   ```bash
   cp build/libs/my-blog-back-app.war $CATALINA_HOME/webapps/
   ```
4. Запустите Tomcat (`bin/startup.sh` или `bin\startup.bat`).
5. Приложение будет доступно по адресу: [http://localhost:8080/my-blog-back-app](http://localhost:8080/my-blog-back-app)

---

## Тестирование

* **JUnit 5** — юнит‑тесты сервисов.
* **Spring Test + MockMvc** — интеграционные тесты REST‑контроллеров.
* **H2 (in-memory)** — временная база для интеграционных тестов.

Запуск:

```bash
./gradlew test
```

## Структура проекта

```
my-blog-back-app/
├─ src/main/java/ru/practicum/blog/
│  ├─ configuration/
│  ├─ controller/
│  ├─ service/
│  ├─ repository/
│  └─ model/
├─ src/main/resources/
│  ├─ application.yml
│  └─ schema.sql
├─ src/main/webapp/WEB-INF/web.xml
├─ src/test/java/
├─ build.gradle.kts
├─ README.md
└─ .gitignore
```

Проект готов к развёртыванию в **Tomcat** и использует **PostgreSQL** как основную базу данных.
