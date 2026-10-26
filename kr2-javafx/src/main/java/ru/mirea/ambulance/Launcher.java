package ru.mirea.ambulance;

import javafx.application.Application;

/**
 * Запускающий класс без наследования от Application.
 * Нужен, чтобы приложение стартовало из IDE и из jar без настройки module path.
 */
public class Launcher {
    public static void main(String[] args) {
        Application.launch(AmbulanceApp.class, args);
    }
}
