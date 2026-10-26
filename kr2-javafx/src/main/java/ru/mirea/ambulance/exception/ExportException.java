package ru.mirea.ambulance.exception;

/**
 * Ошибка при экспорте данных в файл.
 */
public class ExportException extends RuntimeException {
    public ExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
