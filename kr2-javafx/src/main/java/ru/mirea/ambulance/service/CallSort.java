package ru.mirea.ambulance.service;

import ru.mirea.ambulance.model.Call;

import java.util.Comparator;

/**
 * Варианты сортировки списка вызовов (выбираются в ComboBox).
 */
public enum CallSort {
    DATE_DESC("Дата: сначала новые",
            Comparator.comparing(Call::getCreatedAt).reversed()),
    DATE_ASC("Дата: сначала старые",
            Comparator.comparing(Call::getCreatedAt)),
    PRIORITY_DESC("Приоритет: сначала срочные",
            Comparator.comparing(Call::getPriority).reversed().thenComparing(Call::getCreatedAt)),
    PATIENT_NAME("ФИО пациента (А–Я)",
            Comparator.comparing(Call::getPatientName, String.CASE_INSENSITIVE_ORDER));

    private final String label;
    private final Comparator<Call> comparator;

    CallSort(String label, Comparator<Call> comparator) {
        this.label = label;
        this.comparator = comparator;
    }

    public Comparator<Call> comparator() {
        return comparator;
    }

    @Override
    public String toString() {
        return label;
    }
}
