package ru.mirea.ambulance.ui;

import ru.mirea.ambulance.exception.BusinessException;
import ru.mirea.ambulance.exception.EntityNotFoundException;
import ru.mirea.ambulance.model.Call;
import ru.mirea.ambulance.model.CallPriority;
import ru.mirea.ambulance.model.CallStatus;
import ru.mirea.ambulance.model.User;
import ru.mirea.ambulance.service.CallService;
import ru.mirea.ambulance.service.CallStatistics;
import ru.mirea.ambulance.service.UserService;
import ru.mirea.ambulance.util.ExcelExporter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class ConsoleUI {

    private final Scanner scanner = new Scanner(System.in);
    private final UserService userService;
    private final CallService callService;
    private final ExcelExporter excelExporter = new ExcelExporter();

    public ConsoleUI(UserService userService, CallService callService) {
        this.userService = userService;
        this.callService = callService;
    }

    public void run() {
        boolean running = true;
        while (running) {
            printMainMenu();
            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> dispatchersMenu();
                    case "2" -> callsMenu();
                    case "3" -> searchMenu();
                    case "4" -> filterMenu();
                    case "5" -> printStatistics();
                    case "6" -> exportMenu();
                    case "7" -> printRawTables();
                    case "0" -> running = false;
                    default -> System.out.println("Неизвестный пункт меню.");
                }
            } catch (BusinessException | EntityNotFoundException e) {
                System.out.println("Ошибка: " + e.getMessage());
            } catch (RuntimeException e) {
                System.out.println("Ошибка базы данных: " + e.getMessage());
            }
        }
        System.out.println("Работа завершена.");
    }

    private void printMainMenu() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("СИСТЕМА ВЫЗОВА СКОРОЙ ПОМОЩИ");
        System.out.println("========================================");
        System.out.println("1. Диспетчеры");
        System.out.println("2. Вызовы");
        System.out.println("3. Поиск");
        System.out.println("4. Фильтрация");
        System.out.println("5. Статистика");
        System.out.println("6. Экспорт данных");
        System.out.println("7. Вывести таблицы базы данных");
        System.out.println("0. Выход");
        System.out.print("Выберите действие: ");
    }

    // Диспетчеры

    private void dispatchersMenu() {
        boolean back = false;
        while (!back) {
            System.out.println();
            System.out.println("--- Диспетчеры ---");
            System.out.println("1. Добавить");
            System.out.println("2. Показать всех");
            System.out.println("3. Показать по ID");
            System.out.println("4. Изменить");
            System.out.println("5. Удалить");
            System.out.println("0. Назад");
            System.out.print("Выберите действие: ");
            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> {
                        System.out.print("ФИО: ");
                        String fullName = scanner.nextLine();
                        System.out.print("Логин: ");
                        String login = scanner.nextLine();
                        System.out.print("Телефон: ");
                        String phone = scanner.nextLine();
                        User created = userService.create(fullName, login, phone);
                        System.out.println("Создан: " + created);
                    }
                    case "2" -> printUsers(userService.getAll());
                    case "3" -> {
                        int id = readInt("ID: ");
                        System.out.println(userService.getById(id));
                    }
                    case "4" -> {
                        int id = readInt("ID диспетчера для изменения: ");
                        System.out.print("Новое ФИО (пусто — не менять): ");
                        String fullName = scanner.nextLine();
                        System.out.print("Новый логин (пусто — не менять): ");
                        String login = scanner.nextLine();
                        System.out.print("Новый телефон (пусто — не менять): ");
                        String phone = scanner.nextLine();
                        userService.update(id, fullName, login, phone);
                        System.out.println("Диспетчер обновлён.");
                    }
                    case "5" -> {
                        int id = readInt("ID диспетчера для удаления: ");
                        userService.delete(id);
                        System.out.println("Диспетчер удалён.");
                    }
                    case "0" -> back = true;
                    default -> System.out.println("Неизвестный пункт меню.");
                }
            } catch (BusinessException | EntityNotFoundException e) {
                System.out.println("Ошибка: " + e.getMessage());
            } catch (RuntimeException e) {
                System.out.println("Ошибка базы данных: " + e.getMessage());
            }
        }
    }

    // Вызовы

    private void callsMenu() {
        boolean back = false;
        while (!back) {
            System.out.println();
            System.out.println("--- Вызовы ---");
            System.out.println("1. Создать вызов");
            System.out.println("2. Показать все вызовы");
            System.out.println("3. Показать по ID");
            System.out.println("4. Изменить данные вызова");
            System.out.println("5. Изменить статус вызова");
            System.out.println("6. Удалить вызов");
            System.out.println("7. Сортировка по дате создания");
            System.out.println("8. Сортировка по приоритету");
            System.out.println("0. Назад");
            System.out.print("Выберите действие: ");
            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> createCall();
                    case "2" -> printCalls(callService.getAll());
                    case "3" -> {
                        int id = readInt("ID: ");
                        System.out.println(callService.getById(id));
                    }
                    case "4" -> updateCall();
                    case "5" -> changeCallStatus();
                    case "6" -> {
                        int id = readInt("ID вызова для удаления: ");
                        callService.delete(id);
                        System.out.println("Вызов удалён.");
                    }
                    case "7" -> printCalls(callService.sortByDateCreated());
                    case "8" -> printCalls(callService.sortByPriority());
                    case "0" -> back = true;
                    default -> System.out.println("Неизвестный пункт меню.");
                }
            } catch (BusinessException | EntityNotFoundException e) {
                System.out.println("Ошибка: " + e.getMessage());
            } catch (RuntimeException e) {
                System.out.println("Ошибка базы данных: " + e.getMessage());
            }
        }
    }

    private void createCall() {
        int dispatcherId = readInt("ID диспетчера: ");
        System.out.print("ФИО пациента: ");
        String patientName = scanner.nextLine();
        System.out.print("Адрес: ");
        String address = scanner.nextLine();
        System.out.print("Телефон: ");
        String phone = scanner.nextLine();
        System.out.print("Симптомы: ");
        String symptoms = scanner.nextLine();
        CallPriority priority = readPriority();
        Call created = callService.create(dispatcherId, patientName, address, phone, symptoms, priority);
        System.out.println("Создан: " + created);
    }

    private void updateCall() {
        int id = readInt("ID вызова для изменения: ");
        System.out.print("Новое ФИО пациента (пусто — не менять): ");
        String patientName = scanner.nextLine();
        System.out.print("Новый адрес (пусто — не менять): ");
        String address = scanner.nextLine();
        System.out.print("Новый телефон (пусто — не менять): ");
        String phone = scanner.nextLine();
        System.out.print("Новые симптомы (пусто — не менять): ");
        String symptoms = scanner.nextLine();
        System.out.print("Новый приоритет (LOW/MEDIUM/HIGH/CRITICAL, пусто — не менять): ");
        String priorityRaw = scanner.nextLine().trim();
        CallPriority priority = priorityRaw.isBlank() ? null : parsePriorityOrNull(priorityRaw);
        callService.update(id, patientName, address, phone, symptoms, priority);
        System.out.println("Вызов обновлён.");
    }

    private void changeCallStatus() {
        int id = readInt("ID вызова: ");
        System.out.print("Новый статус (NEW/IN_PROGRESS/COMPLETED/CANCELLED): ");
        String statusRaw = scanner.nextLine().trim().toUpperCase();
        try {
            CallStatus status = CallStatus.valueOf(statusRaw);
            callService.changeStatus(id, status);
            System.out.println("Статус обновлён.");
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка: неизвестный статус \"" + statusRaw + "\".");
        }
    }

    // Поиск / фильтрация / статистика / экспорт

    private void searchMenu() {
        System.out.println();
        System.out.println("--- Поиск ---");
        System.out.println("1. По ФИО пациента");
        System.out.println("2. По адресу");
        System.out.print("Выберите действие: ");
        String choice = scanner.nextLine().trim();
        System.out.print("Введите строку поиска: ");
        String query = scanner.nextLine();
        List<Call> result = switch (choice) {
            case "1" -> callService.searchByPatientName(query);
            case "2" -> callService.searchByAddress(query);
            default -> List.of();
        };
        printCalls(result);
    }

    private void filterMenu() {
        System.out.println();
        System.out.println("--- Фильтрация ---");
        System.out.println("1. По статусу");
        System.out.println("2. По приоритету");
        System.out.println("3. По диапазону дат создания");
        System.out.print("Выберите действие: ");
        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1" -> {
                System.out.print("Статус (NEW/IN_PROGRESS/COMPLETED/CANCELLED): ");
                String raw = scanner.nextLine().trim().toUpperCase();
                try {
                    printCalls(callService.filterByStatus(CallStatus.valueOf(raw)));
                } catch (IllegalArgumentException e) {
                    System.out.println("Неизвестный статус.");
                }
            }
            case "2" -> {
                CallPriority priority = readPriority();
                printCalls(callService.filterByPriority(priority));
            }
            case "3" -> {
                LocalDate from = readDate("Дата начала (гггг-мм-дд): ");
                LocalDate to = readDate("Дата окончания (гггг-мм-дд): ");
                printCalls(callService.filterByDateRange(from, to));
            }
            default -> System.out.println("Неизвестный пункт меню.");
        }
    }

    private void printStatistics() {
        CallStatistics stats = callService.getStatistics();
        System.out.println();
        System.out.println("--- Статистика ---");
        System.out.println(stats);
    }

    private void exportMenu() {
        List<Call> calls = callService.getAll();
        String filePath = "calls_export.xlsx";
        excelExporter.exportCalls(calls, filePath);
        System.out.println("Экспортировано " + calls.size() + " записей в файл: " + filePath);
    }

    private void printRawTables() {
        System.out.println();
        System.out.println("=== Таблица: users ===");
        printUsers(userService.getAll());
        System.out.println();
        System.out.println("=== Таблица: calls ===");
        printCalls(callService.getAll());
    }

    // Вспомогательные методы ввода/вывода

    private void printUsers(List<User> users) {
        if (users.isEmpty()) {
            System.out.println("(нет записей)");
            return;
        }
        users.forEach(System.out::println);
    }

    private void printCalls(List<Call> calls) {
        if (calls.isEmpty()) {
            System.out.println("(нет записей)");
            return;
        }
        calls.forEach(System.out::println);
    }

    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String raw = scanner.nextLine().trim();
            try {
                return Integer.parseInt(raw);
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: ID должен быть целым числом.");
            }
        }
    }

    private LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String raw = scanner.nextLine().trim();
            try {
                return LocalDate.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (Exception e) {
                System.out.println("Ошибка: дата должна быть в формате гггг-мм-дд.");
            }
        }
    }

    private CallPriority readPriority() {
        while (true) {
            System.out.print("Приоритет (LOW/MEDIUM/HIGH/CRITICAL): ");
            String raw = scanner.nextLine().trim().toUpperCase();
            try {
                return CallPriority.valueOf(raw);
            } catch (IllegalArgumentException e) {
                System.out.println("Ошибка: неизвестный приоритет \"" + raw + "\".");
            }
        }
    }

    private CallPriority parsePriorityOrNull(String raw) {
        try {
            return CallPriority.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.out.println("Неизвестный приоритет, значение не изменено.");
            return null;
        }
    }
}
