package ru.mirea.ambulance.service;

/**
 * Одна строка таблицы статистики: название показателя и его значение.
 */
public record StatRow(String name, String value) {
}
