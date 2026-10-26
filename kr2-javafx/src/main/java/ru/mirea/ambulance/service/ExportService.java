package ru.mirea.ambulance.service;

import ru.mirea.ambulance.exception.BusinessException;
import ru.mirea.ambulance.exception.ExportException;
import ru.mirea.ambulance.model.Call;
import ru.mirea.ambulance.model.User;
import ru.mirea.ambulance.util.CsvExporter;
import ru.mirea.ambulance.util.ExcelExporter;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Экспорт набора вызовов в .xlsx или .csv (формат выбирается по расширению файла).
 */
public class ExportService {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final String[] HEADERS = {"№", "Пациент", "Адрес", "Телефон", "Симптомы",
            "Приоритет", "Статус", "Диспетчер", "Создан", "Завершён"};

    private final ExcelExporter excelExporter = new ExcelExporter();
    private final CsvExporter csvExporter = new CsvExporter();

    public void export(List<Call> calls, List<User> dispatchers, File file) {
        Map<Integer, String> names = new HashMap<>();
        for (User u : dispatchers) {
            names.put(u.getId(), u.getFullName());
        }
        List<String[]> rows = buildRows(calls, names);

        String fileName = file.getName().toLowerCase();
        try {
            if (fileName.endsWith(".xlsx")) {
                excelExporter.write(rows, file);
            } else if (fileName.endsWith(".csv")) {
                csvExporter.write(rows, file);
            } else {
                throw new BusinessException("Поддерживаются только форматы .xlsx и .csv.");
            }
        } catch (IOException e) {
            throw new ExportException("Не удалось записать файл «" + file.getName() + "»: " + e.getMessage(), e);
        }
    }

    private List<String[]> buildRows(List<Call> calls, Map<Integer, String> names) {
        List<String[]> rows = new ArrayList<>();
        rows.add(HEADERS);
        for (Call c : calls) {
            rows.add(new String[]{
                    String.valueOf(c.getId()),
                    c.getPatientName(),
                    c.getAddress(),
                    nullToEmpty(c.getPhone()),
                    nullToEmpty(c.getSymptoms()),
                    c.getPriority().getLabel(),
                    c.getStatus().getLabel(),
                    names.getOrDefault(c.getDispatcherId(), ""),
                    format(c.getCreatedAt()),
                    format(c.getCompletedAt())
            });
        }
        return rows;
    }

    private String format(LocalDateTime value) {
        return value == null ? "" : value.format(FORMAT);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
