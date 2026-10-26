package ru.mirea.ambulance.exception;

/**
 * Ошибка при работе с базой данных (подключение, SQL-запрос).
 */
public class DatabaseException extends RuntimeException {
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
