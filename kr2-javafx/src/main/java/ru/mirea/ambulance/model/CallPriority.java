package ru.mirea.ambulance.model;

/**
 * Приоритет (срочность) вызова.
 * Порядок констант важен: от наименее к наиболее срочному.
 */
public enum CallPriority {
    LOW("Низкий"),
    MEDIUM("Средний"),
    HIGH("Высокий"),
    CRITICAL("Критический");

    private final String label;

    CallPriority(String label) {
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
