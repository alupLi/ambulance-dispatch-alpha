package ru.mirea.ambulance.util;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Запись таблицы строк в файл Excel (.xlsx). Первая строка — заголовок.
 */
public class ExcelExporter {

    public void write(List<String[]> rows, File file) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Вызовы");

            Font bold = workbook.createFont();
            bold.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(bold);

            for (int r = 0; r < rows.size(); r++) {
                Row row = sheet.createRow(r);
                String[] values = rows.get(r);
                for (int c = 0; c < values.length; c++) {
                    row.createCell(c).setCellValue(values[c]);
                    if (r == 0) {
                        row.getCell(c).setCellStyle(headerStyle);
                    }
                }
            }
            if (!rows.isEmpty()) {
                for (int c = 0; c < rows.get(0).length; c++) {
                    sheet.autoSizeColumn(c);
                }
            }
            try (FileOutputStream out = new FileOutputStream(file)) {
                workbook.write(out);
            }
        }
    }
}
