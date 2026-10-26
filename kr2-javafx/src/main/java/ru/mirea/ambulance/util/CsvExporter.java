package ru.mirea.ambulance.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Запись таблицы строк в CSV (UTF-8 с BOM, разделитель «;» — так файл корректно открывается в Excel).
 */
public class CsvExporter {

    private static final char SEPARATOR = (char) 59; // точка с запятой

    public void write(List<String[]> rows, File file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write("\uFEFF");
            for (String[] row : rows) {
                StringBuilder line = new StringBuilder();
                for (int i = 0; i < row.length; i++) {
                    if (i > 0) {
                        line.append(SEPARATOR);
                    }
                    line.append(escape(row[i]));
                }
                writer.write(line.toString());
                writer.write("\r\n");
            }
        }
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuotes = value.indexOf(SEPARATOR) >= 0 || value.contains("\"")
                || value.contains("\n") || value.contains("\r");
        if (!needsQuotes) {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
