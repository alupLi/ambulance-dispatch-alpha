package ru.mirea.ambulance.service;

import ru.mirea.ambulance.model.CallPriority;
import ru.mirea.ambulance.model.CallStatus;

import java.time.LocalDate;

/**
 * Параметры поиска, фильтрации и сортировки списка вызовов.
 * Любое поле, кроме sort, может быть null - тогда условие не применяется.
 */
public record CallQuery(String search,
                        CallStatus status,
                        CallPriority priority,
                        LocalDate dateFrom,
                        LocalDate dateTo,
                        CallSort sort) {
}
