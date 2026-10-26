package ru.mirea.ambulance.model;

/**
 * Статус вызова скорой помощи.
 * В БД хранится name(), в интерфейсе показывается русская подпись.
 */
public enum CallStatus {
    NEW("Новый"),
    IN_PROGRESS("В работе"),
    COMPLETED("Завершён"),
    CANCELLED("Отменён");

    private final String label;

    CallStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
