# Вызов скорой помощи — JavaFX-приложение (КР №2)

Настольное приложение на JavaFX. Продолжение КР №1: **та же предметная область, те же сущности
(диспетчер, вызов), те же бизнес-правила и та же база PostgreSQL**. Приложение подключается к базе
напрямую через JDBC (REST/Spring здесь нет — они появятся в КР №3).

```
FXML (View) -> Controller -> Service -> Repository (JDBC) -> PostgreSQL
```

---

## 1. Что нужно установить

| Что | Версия | Зачем |
|---|---|---|
| JDK | **21** | компиляция и запуск (в КР №1 был 17 — для КР №2 нужен 21) |
| Maven | 3.9+ | сборка и запуск (`mvn javafx:run`) |
| PostgreSQL | 14+ | база данных из КР №1 |
| IntelliJ IDEA | любая | по желанию, удобнее всего |
| Docker Desktop | любая | готовая система из архива и (по желанию) своя БД в контейнере (раздел 6) |

Проверка: `java -version` (должно быть 21), `mvn -version` (3.9+, Java 21).

---

## 2. База данных (3НФ)

Схема базы приведена к **третьей нормальной форме**: пациенты вынесены в отдельную таблицу,
статусы и приоритеты — в справочники.

* ER-диаграмма: `docs/er-diagram.png` (и `docs/er-diagram.svg` — векторная, для отчёта);
* объяснение нормализации и функциональных зависимостей: `docs/3nf.md`.

```
users ──< calls >── patients        users            — диспетчеры
              ^  ^                   patients         — пациенты (ФИО, телефон)
              |  └── call_priorities call_priorities  — справочник приоритетов
              └───── call_statuses   call_statuses    — справочник статусов
```

Как понять, какая у вас база сейчас: выполните в базе
`SELECT column_name FROM information_schema.columns WHERE table_name = 'calls';`
Если среди столбцов есть `patient_name` — база **старая** (КР №1), если `patient_id` — уже **новая** (3НФ).

### Вариант А. Создать базу с нуля (данные не важны)

```bash
set PGCLIENTENCODING=UTF8
createdb -U postgres ambulance
psql -U postgres -d ambulance -f sql/schema.sql
```
Загрузятся 5 диспетчеров, 9 пациентов и 10 вызовов. Если база `ambulance` уже есть, `createdb` не нужен:
`schema.sql` сам удалит старые таблицы и создаст новые (**все данные в них будут потеряны**).

### Вариант Б. Обновить старую базу из КР №1 и сохранить данные

1. Закройте все программы, работающие с базой (JavaFX-приложение, консольное приложение КР №1).
2. Сделайте резервную копию:
   ```bash
   pg_dump -U postgres -d ambulance -f backup_before_3nf.sql
   ```
3. Запустите миграцию:
   ```bash
   psql -U postgres -d ambulance -f sql/migrate_to_3nf.sql
   ```
   Скрипт работает в одной транзакции: при любой ошибке изменения откатываются и база остаётся старой.
   В конце он выводит количество строк в каждой таблице. Повторный запуск безопасен — скрипт
   остановится с сообщением «Миграция уже выполнена».
4. Проверьте результат (число вызовов должно совпасть с тем, что было до миграции):
   ```sql
   SELECT COUNT(*) FROM calls;
   SELECT c.id, p.full_name, p.phone, pr.code AS priority, st.code AS status
   FROM calls c
   JOIN patients p ON p.id = c.patient_id
   JOIN call_priorities pr ON pr.id = c.priority_id
   JOIN call_statuses st ON st.id = c.status_id
   ORDER BY c.id;
   ```
5. Запустите приложение. Если что-то пошло не так, восстановите базу из копии:
   ```bash
   dropdb -U postgres ambulance
   createdb -U postgres ambulance
   psql -U postgres -d ambulance -f backup_before_3nf.sql
   ```

Что делает миграция: создаёт справочники и `patients` (уникальные пары ФИО + телефон из старых вызовов,
лишние пробелы обрезаются, пустой телефон приравнивается к отсутствующему), заполняет в `calls` ссылки
`patient_id`, `priority_id`, `status_id`, затем удаляет старые столбцы `patient_name`, `phone`, `priority`, `status`.
Идентификаторы вызовов и диспетчеров не меняются.

### Windows и pgAdmin
* Если `psql`/`pg_dump` не находятся, используйте полный путь, например
  `"C:\Program Files\PostgreSQL\16\bin\psql.exe"`.
* В **pgAdmin**: ПКМ по базе → *Query Tool* → открыть файл (`schema.sql` или `migrate_to_3nf.sql`) → выполнить (F5).
  В файле миграции **удалите первую строку `\set ON_ERROR_STOP on`** — это команда psql, pgAdmin её не понимает.
  Резервную копию в pgAdmin: ПКМ по базе → *Backup…*.
* Если русские буквы в консоли «кракозябрами»: `set PGCLIENTENCODING=UTF8` (Windows) или
  `export PGCLIENTENCODING=UTF8` (Linux/macOS).

> Приложение КР №2 (и консольное КР №1 из пакета `ambulance-dispatch`) работают **только с новой схемой**.
> Со старой базой они выдадут ошибку вроде `relation "patients" does not exist`.

---

## 3. Настройка подключения

Файл `src/main/resources/database.properties`:

```properties
db.url=jdbc:postgresql://localhost:5432/ambulance
db.user=postgres
db.password=postgres
```

Поменяйте под свою установку:
- `ambulance` — имя вашей базы;
- `5432` — порт PostgreSQL (если у вас другой — укажите его);
- `db.user` / `db.password` — ваши логин и пароль PostgreSQL.

Если база запущена в Docker (раздел 6.2), порт будет **5433**.
Пароль хранится только в этом файле, в Java-коде его нет.
После изменения файла перезапустите приложение.

---

## 4. Запуск

### Через командную строку
```bash
cd ambulance-javafx
mvn clean compile        # проверить, что проект собирается
mvn javafx:run           # запустить приложение
```

### Через IntelliJ IDEA
1. `File → Open` → выберите папку с `pom.xml` → дождитесь загрузки Maven-зависимостей.
2. `File → Project Structure → Project → SDK` = JDK 21 (и Language level 21).
3. Запустите класс **`ru.mirea.ambulance.Launcher`** (правая кнопка → Run). Не запускайте
   `AmbulanceApp` напрямую — будет ошибка «JavaFX runtime components are missing».
   Либо в панели Maven: `Plugins → javafx → javafx:run`.

При старте приложение проверяет подключение к БД. Если подключиться не удалось, появится окно
с понятным сообщением и приложение закроется.

---

## 5. Как пользоваться

- **Таблица вызовов** — основные поля; строки подсвечены: критические — красным, высокие — оранжевым,
  завершённые/отменённые — серым. Кликом по заголовку столбца таблицу можно сортировать.
- **Сведения о выбранном вызове** (внизу) — полные данные вызова и связанного диспетчера.
- **Добавить / Изменить** (или двойной клик по строке) — форма. Диспетчер выбирается из списка.
  Ошибки заполнения показываются в самой форме красным блоком.
- **Удалить** — с окном подтверждения. Вызов «в работе» удалить нельзя.
- **Поиск** (по пациенту, адресу, телефону, симптомам) и **фильтры** (статус, приоритет, период дат)
  применяются сразу при вводе. Выпадающий список **сортировки**: по дате (2 варианта), по приоритету,
  по ФИО пациента. **Сбросить** — возвращает всё по умолчанию.
- **Обновить** — заново читает данные из БД (нужно, если базу меняли снаружи).
- **Статистика** — окно с 10 показателями (всего, по статусам с процентами, срочные, среднее время
  обработки, за 24 часа, самый загруженный диспетчер и др.).
- **Экспорт** — сохраняет то, что сейчас показано в таблице, в `.xlsx` или `.csv`.
  Место сохранения выбирается в окне `FileChooser`.

### Бизнес-правила (те же, что в КР №1, реализованы в `CallService`)
1. Нельзя создать вызов без адреса.
2. Нельзя создать вызов без ФИО пациента.
3. Нельзя указать несуществующего диспетчера.
4. Нельзя вернуть завершённый/отменённый вызов в работу.
5. Нельзя удалить вызов со статусом «В работе».

Валидация формы (обязательные поля, длины 150/255/500, формат телефона, корректность периода дат)
тоже находится в `CallService`.

---

## 6. Docker (Windows + Docker Desktop)

В КР №2 Docker используется для двух вещей:

* **6.1** — развернуть и показать **готовую систему из архива** (backend, веб-интерфейс, PostgreSQL; это требование задания);
* **6.2** — запустить **PostgreSQL нашего проекта** в контейнере (по желанию, вместо локально установленного).

JavaFX-приложение в Docker **не запускается** никогда — оно работает на вашем компьютере как обычная программа.

### 6.0. Установка и проверка Docker Desktop

1. Скачайте Docker Desktop для Windows (docker.com → Docker Desktop) и установите. Оставьте включённым
   **WSL 2** (рекомендуемый режим). После установки перезагрузите компьютер.
2. Если Docker просит включить виртуализацию: она включается в BIOS/UEFI (Intel VT-x / AMD-V / SVM Mode).
   Если просит обновить WSL: откройте PowerShell и выполните `wsl --update`.
3. Запустите **Docker Desktop** и дождитесь, пока слева внизу появится зелёный статус **Engine running**.
4. Откройте PowerShell и проверьте:
   ```powershell
   docker --version
   docker compose version
   docker info
   ```
   Все три команды должны отработать без ошибок (в `docker info` не должно быть «error during connect»).

Docker Desktop должен быть **запущен** при каждой работе с контейнерами.

### 6.1. Готовая система из архива задания

1. Распакуйте архив в **отдельную папку с простым путём**, лучше без русских букв и пробелов, например
   `C:\kr2\student-hub`. Внутри должны лежать `backend`, `javafx-client`, `android-client` и файл **`.env`**
   (он начинается с точки; если не виден в Проводнике — включите «Показывать скрытые файлы»).
   Не переносите папки по отдельности.
2. Откройте PowerShell **в корне распакованного архива** (в Проводнике: нажмите в адресной строке, введите
   `powershell`, Enter) и запустите серверную часть и PostgreSQL:
   ```powershell
   docker compose --env-file .env -f backend/docker-compose.yml up -d --wait
   ```
   Либо готовым скриптом:
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\backend\start-compose.ps1
   ```
   Первый запуск долгий (несколько минут): Docker скачивает образы и собирает backend. Флаг `--wait`
   заставляет команду дождаться, пока контейнеры станут «healthy».
3. Проверьте состояние контейнеров:
   ```powershell
   docker compose --env-file .env -f backend/docker-compose.yml ps
   ```
   PostgreSQL и backend должны быть в состоянии `running` / `healthy`. То же видно в Docker Desktop → вкладка *Containers*.
4. Откройте в браузере:
   * `http://localhost:8080/` — должна открыться страница системы;
   * `http://localhost:8080/actuator/health` — должно быть `"status":"UP"`.
5. Войдите администратором: логин и пароль указаны на странице задания и в файле `.env`
   (`BOOTSTRAP_ADMIN_EMAIL` / `BOOTSTRAP_ADMIN_PASSWORD`). Система может предложить сменить пароль.
6. **Только после этого** запускайте пример JavaFX из архива:
   ```powershell
   cd javafx-client
   .\run.ps1
   ```
   или `mvn javafx:run`. Если JavaFX открыть раньше, окно появится, но данные не загрузятся.
7. Остановка (данные сохраняются):
   ```powershell
   docker compose --env-file .env -f backend/docker-compose.yml down
   ```
   **Не добавляйте `-v`** — это удалит данные базы.

Порядок всегда такой: **Docker Desktop → PostgreSQL и backend → проверка в браузере → только потом JavaFX**.

Если что-то не запускается, посмотрите логи:
```powershell
docker compose --env-file .env -f backend/docker-compose.yml logs backend
docker compose --env-file .env -f backend/docker-compose.yml logs db
```

Порты готовой системы: веб и backend — **8080**, PostgreSQL с компьютера — **55432**
(в нашем проекте эти порты не используются, конфликта нет). Для подключения к этой базе с компьютера
(например, из pgAdmin): `localhost:55432`, база `student_hub`, пользователь и пароль — в `.env`.

### 6.2. PostgreSQL нашего проекта в Docker (по желанию)

В корне проекта есть `docker-compose.yml`. Он поднимает PostgreSQL 16 с базой `ambulance` и **сам создаёт таблицы
и тестовые данные** из `sql/schema.sql` при первом запуске.

1. Откройте PowerShell в папке проекта (рядом с `pom.xml`) и запустите:
   ```powershell
   docker compose up -d --wait
   ```
2. Проверьте, что база создана:
   ```powershell
   docker compose ps
   docker compose exec db psql -U postgres -d ambulance -c "\dt"
   docker compose exec db psql -U postgres -d ambulance -c "SELECT COUNT(*) FROM calls;"
   ```
   Должны быть 5 таблиц и 10 вызовов.
3. В `src/main/resources/database.properties` укажите порт контейнера **5433**:
   ```properties
   db.url=jdbc:postgresql://localhost:5433/ambulance
   db.user=postgres
   db.password=postgres
   ```
4. Запустите приложение: `mvn javafx:run` (Docker Desktop при этом должен работать).

Управление:
```powershell
docker compose down        # остановить, данные сохраняются
docker compose up -d       # запустить снова
docker compose down -v     # остановить и УДАЛИТЬ данные: следующий запуск создаст базу заново из schema.sql
```
Данные хранятся в Docker volume `ambulance_pgdata`. Скрипт `schema.sql` выполняется только при создании
пустой базы; если вы изменили `schema.sql`, выполните `docker compose down -v` и запустите заново
(или примените изменения вручную через `docker compose exec`).

Перенос данных из локального PostgreSQL в контейнер (если нужно сохранить свои записи):
```powershell
pg_dump -U postgres -d ambulance -f backup.sql
docker compose down -v
docker compose up -d --wait
Get-Content backup.sql -Raw | docker compose exec -T db psql -U postgres -d ambulance
```
(в таком случае `backup.sql` содержит и создание таблиц; ошибки «already exists» для таблиц можно игнорировать
или сначала очистить схему: `docker compose exec db psql -U postgres -d ambulance -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"`).

### 6.3. Если Docker не запускается

| Симптом | Что делать |
|---|---|
| `error during connect` / `cannot find the file specified ... dockerDesktopLinuxEngine` | Docker Desktop не запущен или ещё стартует: запустите его и дождитесь *Engine running* |
| Docker Desktop завис на старте / «WSL 2 installation is incomplete» | PowerShell: `wsl --update`, затем перезапустите Docker Desktop; при необходимости перезагрузите ПК |
| «Virtualization is disabled» | включите виртуализацию в BIOS/UEFI (VT-x / SVM Mode) |
| `port is already allocated` / `address already in use` (8080, 55432, 5433) | порт занят: найдите процесс `netstat -ano \| findstr :8080`, закройте его или измените порт (`APP_PORT`/`POSTGRES_PORT` в `.env` архива, `DB_PORT` для нашей базы) |
| `--wait` завершился ошибкой / контейнер `unhealthy` | посмотрите `logs backend` / `logs db`; часто помогает подождать и повторить команду |
| Не удаётся войти администратором | учётная запись создаётся только при первом запуске на **пустой** базе; если контейнеры запускали раньше с другим `.env`, удалите данные демо-системы: `docker compose --env-file .env -f backend/docker-compose.yml down -v` (только для архива, не для своей базы!) и запустите заново |
| Очень медленная первая сборка | это нормально (скачиваются образы и зависимости); последующие запуски быстрые |

### Что говорить на защите про Docker
* Docker запускает PostgreSQL и backend готовой системы в контейнерах; контейнер — изолированный процесс с готовым окружением, образ — его «шаблон».
* `docker compose up -d --wait` читает `docker-compose.yml`, поднимает сервисы в фоне и ждёт готовности (healthcheck).
* Состояние проверяется так: `docker compose ps`, Docker Desktop → Containers, `http://localhost:8080/actuator/health`.
* Данные базы лежат в volume и переживают остановку (`down`); удаляет их только `down -v`.
* Порт `55432:5432` — порт на компьютере и порт внутри контейнера; поэтому JDBC с компьютера подключается к опубликованному порту.
* JavaFX-клиент в Docker не запускается: он обычное настольное приложение, которое подключается к уже работающим сервисам.

---

## 7. Частые проблемы

| Симптом | Причина и решение |
|---|---|
| «Нет подключения к базе данных» при старте | PostgreSQL не запущен, неверный порт/имя базы/логин/пароль в `database.properties` |
| `password authentication failed` | неверный `db.user` / `db.password` |
| `database "ambulance" does not exist` | база не создана: шаг 2 или поправьте имя в `db.url` |
| `relation "users" does not exist` | таблицы не созданы: выполните `sql/schema.sql` |
| `relation "patients" does not exist` / `column "patient_id" does not exist` | база старая (КР №1): выполните миграцию (раздел 2, вариант Б) или создайте базу заново (вариант А) |
| `JavaFX runtime components are missing` | запустили `AmbulanceApp` напрямую: запускайте `Launcher` или `mvn javafx:run` |
| `release version 21 not supported` | Maven/IDE используют старую JDK: поставьте JDK 21 и укажите её в `JAVA_HOME` / Project SDK |
| Maven не скачивает зависимости | проверьте интернет/прокси; в IntelliJ: Maven → Reload All Maven Projects |
| Кракозябры в Excel при открытии `.csv` | файл в UTF-8 с BOM и разделителем `;` — открывайте через *Данные → Из текста/CSV* или используйте `.xlsx` |
| Новые данные из БД не видны | нажмите «Обновить» |

---

## 8. Структура проекта

```
docker-compose.yml                 PostgreSQL проекта в Docker (раздел 6.2)
sql/schema.sql                     создание таблиц (3НФ) + тестовые данные
sql/migrate_to_3nf.sql             перенос старой базы КР №1 в 3НФ без потери данных
docs/                              er-diagram.png/.svg/.md (схема БД), 3nf.md (нормализация)
src/main/resources/
├── database.properties            параметры подключения к БД
├── fxml/  main.fxml, call_form.fxml, statistics.fxml
└── css/style.css                  оформление
src/main/java/ru/mirea/ambulance/
├── Launcher.java, AmbulanceApp.java   запуск JavaFX, сборка слоёв
├── controller/   MainController, CallFormController, StatisticsController
├── service/      CallService, UserService, ExportService, CallQuery, CallSort, StatRow
├── repository/   Repository<T>, UserRepository, CallRepository, PatientRepository   (весь SQL тут, PreparedStatement)
├── model/        User, Call, CallStatus, CallPriority
├── exception/    BusinessException, EntityNotFoundException, DatabaseException, ExportException
└── util/         DatabaseManager, ExcelExporter, CsvExporter, Dialogs
```

---

## 9. Шпаргалка к защите

| Вопрос | Где смотреть / что отвечать |
|---|---|
| Stage, Scene, Node | `AmbulanceApp`: Stage — окно, Scene — содержимое окна, Node — любой элемент внутри Scene (кнопка, таблица, панель) |
| Зачем FXML | разметка интерфейса отдельно от кода; `fxml/*.fxml` |
| Связь FXML ↔ Controller | `fx:controller` в корне FXML; поля `@FXML` связаны с `fx:id`; обработчики — `onAction="#onAdd"` ↔ метод `onAdd()` |
| Обновление TableView | таблица привязана к `ObservableList tableItems`; `tableItems.setAll(...)` сразу перерисовывает таблицу |
| Зачем ObservableList | список сообщает таблице об изменениях, поэтому ручное обновление не нужно |
| Где бизнес-логика | `CallService` (правила, валидация, фильтры, статистика) |
| Где работа с данными | `repository/*` (JDBC, `PreparedStatement`), подключение — `DatabaseManager` |
| Исключения | сервис бросает `BusinessException`/`EntityNotFoundException`, репозиторий — `DatabaseException`; контроллеры ловят и показывают `Alert`/сообщение в форме, stack trace пользователю не показывается (пишется в лог) |
| Проверка ввода | `CallService.validate(...)`, сообщения называют поле и что исправить |
| Поиск и фильтры | `CallService.filterAndSort` — Stream API по загруженному списку; параметры собираются в `CallQuery` |
| Сортировка | enum `CallSort` (Comparator'ы) + встроенная сортировка заголовков TableView |
| Статистика | `CallService.getStatistics()` → список `StatRow` → TableView в окне статистики |
| Экспорт | `ExportService` → `ExcelExporter` (Apache POI) / `CsvExporter`; путь выбирает пользователь через `FileChooser` |
| Почему БД в 3НФ | `docs/3nf.md`: данные пациента вынесены в `patients`, статусы/приоритеты — в справочники; нет частичных и транзитивных зависимостей |
| Как сохраняется вызов | `CallRepository.save`: в одной транзакции находим/создаём пациента (`PatientRepository.findOrCreate`) и вставляем вызов; при ошибке `rollback` |
| Docker | раздел 6 (там же краткий конспект «что говорить на защите») |

---

## 10. Задел на КР №3 и КР №4

Слои разделены, поэтому в КР №3 `Repository` и `Service` можно перенести в Spring Boot почти без
изменений (контроллеры JavaFX заменятся REST-контроллерами), а в КР №4 Android будет использовать
тот же REST API. Предметная область и БД не меняются.
