package ru.mirea.ambulance.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.mirea.ambulance.model.Call;
import ru.mirea.ambulance.model.User;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Экспорт списка вызовов в Excel (.xlsx).
 */
public class ExcelExporter {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public void exportCalls(List<Call> calls, String filePath) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Вызовы");

            String[] headers = {"ID", "Диспетчер ID", "Пациент", "Адрес", "Телефон",
                    "Симптомы", "Приоритет", "Статус", "Создан", "Завершён"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            int rowIndex = 1;
            for (Call call : calls) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(call.getId());
                row.createCell(1).setCellValue(call.getDispatcherId());
                row.createCell(2).setCellValue(call.getPatientName());
                row.createCell(3).setCellValue(call.getAddress());
                row.createCell(4).setCellValue(call.getPhone() != null ? call.getPhone() : "");
                row.createCell(5).setCellValue(call.getSymptoms() != null ? call.getSymptoms() : "");
                row.createCell(6).setCellValue(call.getPriority().name());
                row.createCell(7).setCellValue(call.getStatus().name());
                row.createCell(8).setCellValue(call.getCreatedAt().format(FORMATTER));
                row.createCell(9).setCellValue(call.getCompletedAt() != null
                        ? call.getCompletedAt().format(FORMATTER) : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream out = new FileOutputStream(filePath)) {
                workbook.write(out);
            }
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при экспорте в Excel: " + e.getMessage(), e);
        }
    }

    public void exportUsers(List<User> users, String filePath) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Пользователи");

            String[] headers = {"ID", "ФИО", "Логин", "Телефон"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            int rowIndex = 1;
            for (User user : users) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(user.getId());
                row.createCell(1).setCellValue(user.getFullName() != null ? user.getFullName() : "");
                row.createCell(2).setCellValue(user.getLogin() != null ? user.getLogin() : "");
                row.createCell(3).setCellValue(user.getPhone() != null ? user.getPhone() : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream out = new FileOutputStream(filePath)) {
                workbook.write(out);
            }
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при экспорте в Excel: " + e.getMessage(), e);
        }
    }
}
