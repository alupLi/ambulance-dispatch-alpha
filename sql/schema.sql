-- Выполнять уже подключившись к базе данных "ambulance".
-- Если базы ещё нет, сначала создать её:
--   CREATE DATABASE ambulance;

DROP TABLE IF EXISTS calls;
DROP TABLE IF EXISTS users;

-- Диспетчеры (пользователи системы)
CREATE TABLE users (
    id         SERIAL PRIMARY KEY,
    full_name  VARCHAR(150) NOT NULL,
    login      VARCHAR(50)  NOT NULL UNIQUE,
    phone      VARCHAR(20)
);

-- Вызовы скорой помощи (основная сущность)
CREATE TABLE calls (
    id            SERIAL PRIMARY KEY,
    dispatcher_id INTEGER NOT NULL REFERENCES users (id),
    patient_name  VARCHAR(150) NOT NULL,
    address       VARCHAR(255) NOT NULL,
    phone         VARCHAR(20),
    symptoms      VARCHAR(500),
    priority      VARCHAR(20) NOT NULL CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    status        VARCHAR(20) NOT NULL CHECK (status IN ('NEW', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at  TIMESTAMP
);

-- ==========================================================
-- Тестовые данные
-- ==========================================================

INSERT INTO users (full_name, login, phone) VALUES
    ('Иванова Мария Сергеевна', 'ivanova', '+7-900-100-10-01'),
    ('Петров Алексей Викторович', 'petrov', '+7-900-100-10-02'),
    ('Сидорова Анна Павловна', 'sidorova', '+7-900-100-10-03'),
    ('Кузнецов Дмитрий Игоревич', 'kuznetsov', '+7-900-100-10-04'),
    ('Смирнова Ольга Николаевна', 'smirnova', '+7-900-100-10-05');

INSERT INTO calls (dispatcher_id, patient_name, address, phone, symptoms, priority, status, created_at, completed_at) VALUES
    (1, 'Волков Иван Петрович',      'ул. Ленина, д. 5, кв. 12',   '+7-900-200-01-01', 'Боль в груди',        'CRITICAL', 'IN_PROGRESS', NOW() - INTERVAL '2 hours', NULL),
    (1, 'Морозова Елена Викторовна', 'пр. Мира, д. 10, кв. 3',     '+7-900-200-01-02', 'Высокая температура', 'MEDIUM',   'NEW',         NOW() - INTERVAL '1 hour',  NULL),
    (2, 'Лебедев Сергей Николаевич', 'ул. Гагарина, д. 22',        '+7-900-200-01-03', 'Перелом ноги',        'HIGH',     'COMPLETED',   NOW() - INTERVAL '1 day',   NOW() - INTERVAL '23 hours'),
    (2, 'Новикова Дарья Андреевна',  'ул. Пушкина, д. 7, кв. 45',  '+7-900-200-01-04', 'Аллергическая реакция','HIGH',    'NEW',         NOW() - INTERVAL '30 minutes', NULL),
    (3, 'Козлов Артём Русланович',   'ул. Советская, д. 3',        '+7-900-200-01-05', 'Головокружение',      'LOW',      'CANCELLED',   NOW() - INTERVAL '3 days',  NOW() - INTERVAL '3 days'),
    (3, 'Соколова Наталья Юрьевна',  'пр. Победы, д. 18, кв. 9',   '+7-900-200-01-06', 'Ожог руки',           'MEDIUM',   'IN_PROGRESS', NOW() - INTERVAL '40 minutes', NULL),
    (4, 'Егоров Максим Олегович',    'ул. Кирова, д. 14',          '+7-900-200-01-07', 'Отравление',          'HIGH',     'NEW',         NOW() - INTERVAL '10 minutes', NULL),
    (4, 'Павлова Виктория Ивановна', 'ул. Гоголя, д. 2, кв. 8',    '+7-900-200-01-08', 'Судороги',            'CRITICAL', 'COMPLETED',   NOW() - INTERVAL '2 days',  NOW() - INTERVAL '2 days'),
    (5, 'Романов Илья Дмитриевич',   'ул. Чехова, д. 9',           '+7-900-200-01-09', 'Ушиб при падении',    'LOW',      'NEW',         NOW() - INTERVAL '5 minutes', NULL),
    (5, 'Тарасова Юлия Сергеевна',   'пр. Строителей, д. 30, кв. 1','+7-900-200-01-10','Приступ астмы',       'CRITICAL', 'IN_PROGRESS', NOW() - INTERVAL '15 minutes', NULL);
