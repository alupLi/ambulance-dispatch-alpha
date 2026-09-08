package ru.mirea.ambulance.exception;

/**
 * Выбрасывается, когда запись с указанным ID не найдена.
 */
public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String message) {
        super(message);
    }
}
