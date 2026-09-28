# Вызов скорой помощи — консольная информационная система (КР1)

Консольное Java-приложение с хранением данных в PostgreSQL.
Архитектура: `Console UI -> Service -> Repository (JDBC) -> PostgreSQL`.

## Структура проекта

```
src/main/java/ru/mirea/ambulance/
├── Main.java
├── model/          User, Call, CallStatus (enum), CallPriority (enum)
├── repository/      Repository<T> (интерфейс), UserRepository, CallRepository
├── service/         UserService, CallService, CallStatistics
├── exception/        EntityNotFoundException, BusinessException
├── util/             DatabaseManager, ExcelExporter
└── ui/               ConsoleUI
sql/schema.sql         создание таблиц + тестовые данные
src/main/resources/db.properties   настройки подключения к БД
```

## 1. Подготовка базы данных

Нужен установленный локально PostgreSQL.

```bash
# Создать базу данных
createdb ambulance
# (или через psql: CREATE DATABASE ambulance;)

# Создать таблицы и загрузить тестовые данные
psql -d ambulance -f sql/schema.sql
```

Если имя пользователя/пароль/порт отличаются от значений по умолчанию —
поправь `src/main/resources/db.properties`:

```properties
db.url=jdbc:postgresql://localhost:5432/ambulance
db.user=postgres
db.password=postgres
```

## 2. Сборка и запуск

Нужен установленный Maven и JDK 17+.

```bash
mvn clean package
java -jar target/ambulance-dispatch-jar-with-dependencies.jar
```

## Что реализовано (базовый минимум для КР1)

- Две связанные сущности: `users` (диспетчеры) и `calls` (вызовы), связь по `dispatcher_id`.
- Два enum: `CallStatus` и `CallPriority`.
- Полный CRUD по вызовам: создание, список, поиск по ID, изменение, удаление.
- 5 бизнес-правил (в сервисном слое, не в UI):
  1. Нельзя создать вызов без адреса.
  2. Нельзя создать вызов без ФИО пациента.
  3. Нельзя указать несуществующего диспетчера.
  4. Нельзя вернуть завершённый/отменённый вызов в работу (запрещённый переход статуса).
  5. Нельзя удалить вызов, который сейчас в статусе `IN_PROGRESS`.
- Обработка некорректного ввода (текст вместо числа, несуществующий ID, отсутствие подключения к БД, ошибки SQL) — приложение не падает.
- Поиск (по ФИО пациента, по адресу), фильтрация (по статусу, по приоритету, по диапазону дат), сортировка (по дате, по приоритету) — через Stream API.
- Статистика: 7 показателей (диспетчеры, вызовы по всем статусам, вызовы с высоким/критическим приоритетом).
- Экспорт всех вызовов в `calls_export.xlsx` (Apache POI).
- Один интерфейс (`Repository<T>`), реализованный двумя классами — полиморфизм.
- Свои исключения: `EntityNotFoundException`, `BusinessException`.
- Многослойная архитектура, PreparedStatement везде, try-with-resources.
