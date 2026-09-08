package ru.mirea.ambulance.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Отвечает за подключение к базе данных PostgreSQL.
 * Параметры читаются из src/main/resources/db.properties,
 * при отсутствии файла используются значения по умолчанию.
 */
public class DatabaseManager {

    private final String url;
    private final String user;
    private final String password;

    public DatabaseManager() {
        Properties props = new Properties();
        try (InputStream in = DatabaseManager.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            System.out.println("Не удалось прочитать db.properties, использую значения по умолчанию.");
        }

        this.url = props.getProperty("db.url", "jdbc:postgresql://localhost:5432/ambulance");
        this.user = props.getProperty("db.user", "postgres");
        this.password = props.getProperty("db.password", "postgres");
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
