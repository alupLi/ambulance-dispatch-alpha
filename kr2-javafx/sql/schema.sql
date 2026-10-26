DROP TABLE IF EXISTS calls          CASCADE;
DROP TABLE IF EXISTS patients       CASCADE;
DROP TABLE IF EXISTS call_statuses  CASCADE;
DROP TABLE IF EXISTS call_priorities CASCADE;
DROP TABLE IF EXISTS users          CASCADE;

-- Диспетчеры (пользователи системы)
CREATE TABLE users (
    id        SERIAL       PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    login     VARCHAR(50)  NOT NULL UNIQUE,
    phone     VARCHAR(20)
);

-- Пациенты: ФИО и телефон принадлежат пациенту, а не вызову
CREATE TABLE patients (
    id        SERIAL       PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    phone     VARCHAR(20)
);
-- Один и тот же пациент (ФИО + телефон) хранится один раз
CREATE UNIQUE INDEX ux_patients_name_phone ON patients (full_name, COALESCE(phone, ''));

-- Справочник статусов вызова (соответствует enum CallStatus)
CREATE TABLE call_statuses (
    id    SMALLINT    PRIMARY KEY,
    code  VARCHAR(20) NOT NULL UNIQUE,
    title VARCHAR(50) NOT NULL
);

-- Справочник приоритетов вызова (соответствует enum CallPriority)
CREATE TABLE call_priorities (
    id    SMALLINT    PRIMARY KEY,
    code  VARCHAR(20) NOT NULL UNIQUE,
    title VARCHAR(50) NOT NULL
);

-- Вызовы скорой помощи (основная сущность)
CREATE TABLE calls (
    id            SERIAL       PRIMARY KEY,
    dispatcher_id INTEGER      NOT NULL REFERENCES users (id),
    patient_id    INTEGER      NOT NULL REFERENCES patients (id),
    address       VARCHAR(255) NOT NULL,          -- адрес, куда выехать (атрибут вызова)
    symptoms      VARCHAR(500),
    priority_id   SMALLINT     NOT NULL REFERENCES call_priorities (id),
    status_id     SMALLINT     NOT NULL REFERENCES call_statuses (id),
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    completed_at  TIMESTAMP,
    CONSTRAINT ck_calls_completed_after_created
        CHECK (completed_at IS NULL OR completed_at >= created_at)
);

CREATE INDEX ix_calls_dispatcher ON calls (dispatcher_id);
CREATE INDEX ix_calls_patient    ON calls (patient_id);
CREATE INDEX ix_calls_status     ON calls (status_id);
CREATE INDEX ix_calls_priority   ON calls (priority_id);

-- Справочники

INSERT INTO call_statuses (id, code, title) VALUES
    (1, 'NEW',         'Новый'),
    (2, 'IN_PROGRESS', 'В работе'),
    (3, 'COMPLETED',   'Завершён'),
    (4, 'CANCELLED',   'Отменён');

INSERT INTO call_priorities (id, code, title) VALUES
    (1, 'LOW',      'Низкий'),
    (2, 'MEDIUM',   'Средний'),
    (3, 'HIGH',     'Высокий'),
    (4, 'CRITICAL', 'Критический');

-- Тестовые данные: 5 диспетчеров, 9 пациентов, 10 вызовов
-- (пациент «Волков Иван Петрович» звонил дважды — одна запись в patients)

INSERT INTO users (full_name, login, phone) VALUES
    ('Иванова Мария Сергеевна',   'ivanova',   '+7-900-100-10-01'),
    ('Петров Алексей Викторович', 'petrov',    '+7-900-100-10-02'),
    ('Сидорова Анна Павловна',    'sidorova',  '+7-900-100-10-03'),
    ('Кузнецов Дмитрий Игоревич', 'kuznetsov', '+7-900-100-10-04'),
    ('Смирнова Ольга Николаевна', 'smirnova',  '+7-900-100-10-05');

INSERT INTO patients (full_name, phone) VALUES
    ('Волков Иван Петрович',      '+7-900-200-01-01'),
    ('Морозова Елена Викторовна', '+7-900-200-01-02'),
    ('Лебедев Сергей Николаевич', '+7-900-200-01-03'),
    ('Новикова Дарья Андреевна',  '+7-900-200-01-04'),
    ('Козлов Артём Русланович',   '+7-900-200-01-05'),
    ('Соколова Наталья Юрьевна',  '+7-900-200-01-06'),
    ('Егоров Максим Олегович',    '+7-900-200-01-07'),
    ('Павлова Виктория Ивановна', '+7-900-200-01-08'),
    ('Романов Илья Дмитриевич',   '+7-900-200-01-09');

INSERT INTO calls (dispatcher_id, patient_id, address, symptoms, priority_id, status_id, created_at, completed_at)
SELECT v.dispatcher_id, p.id, v.address, v.symptoms, pr.id, st.id,
       v.created_at::timestamp, v.completed_at::timestamp
FROM (VALUES
    (1,  1, 'Волков Иван Петрович',      'ул. Ленина, д. 5, кв. 12',      'Боль в груди',         'CRITICAL', 'IN_PROGRESS', NOW() - INTERVAL '2 hours',      NULL::timestamptz),
    (2,  1, 'Морозова Елена Викторовна', 'пр. Мира, д. 10, кв. 3',        'Высокая температура',  'MEDIUM',   'NEW',         NOW() - INTERVAL '1 hour',       NULL::timestamptz),
    (3,  2, 'Лебедев Сергей Николаевич', 'ул. Гагарина, д. 22',           'Перелом ноги',         'HIGH',     'COMPLETED',   NOW() - INTERVAL '1 day',        NOW() - INTERVAL '23 hours'),
    (4,  2, 'Новикова Дарья Андреевна',  'ул. Пушкина, д. 7, кв. 45',     'Аллергическая реакция','HIGH',     'NEW',         NOW() - INTERVAL '30 minutes',   NULL::timestamptz),
    (5,  3, 'Козлов Артём Русланович',   'ул. Советская, д. 3',           'Головокружение',       'LOW',      'CANCELLED',   NOW() - INTERVAL '3 days',       NOW() - INTERVAL '3 days'),
    (6,  3, 'Соколова Наталья Юрьевна',  'пр. Победы, д. 18, кв. 9',      'Ожог руки',            'MEDIUM',   'IN_PROGRESS', NOW() - INTERVAL '40 minutes',   NULL::timestamptz),
    (7,  4, 'Егоров Максим Олегович',    'ул. Кирова, д. 14',             'Отравление',           'HIGH',     'NEW',         NOW() - INTERVAL '10 minutes',   NULL::timestamptz),
    (8,  4, 'Павлова Виктория Ивановна', 'ул. Гоголя, д. 2, кв. 8',       'Судороги',             'CRITICAL', 'COMPLETED',   NOW() - INTERVAL '2 days',       NOW() - INTERVAL '2 days' + INTERVAL '45 minutes'),
    (9,  5, 'Романов Илья Дмитриевич',   'ул. Чехова, д. 9',              'Ушиб при падении',     'LOW',      'NEW',         NOW() - INTERVAL '5 minutes',    NULL::timestamptz),
    (10, 5, 'Волков Иван Петрович',      'пр. Строителей, д. 30, кв. 1',  'Приступ астмы',        'CRITICAL', 'IN_PROGRESS', NOW() - INTERVAL '15 minutes',   NULL::timestamptz)
) AS v(n, dispatcher_id, patient_name, address, symptoms, priority, status, created_at, completed_at)
JOIN patients        p  ON p.full_name = v.patient_name
JOIN call_priorities pr ON pr.code     = v.priority
JOIN call_statuses   st ON st.code     = v.status
ORDER BY v.n;