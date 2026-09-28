package ru.mirea.ambulance.exception;

/**
 * Выбрасывается при нарушении бизнес-правил предметной области.
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) { super(message); }
}
