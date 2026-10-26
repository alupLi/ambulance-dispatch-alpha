package ru.mirea.ambulance.model;

/**
 * Статус вызова скорой помощи.
 */
public enum CallStatus {
    NEW,          // вызов принят, ещё не обработан
    IN_PROGRESS,  // бригада выехала / работает по вызову
    COMPLETED,    // вызов завершён
    CANCELLED     // вызов отменён
}
