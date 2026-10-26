package ru.mirea.ambulance.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Подключение к PostgreSQL через JDBC.
 * Параметры (url, пользователь, пароль) читаются из src/main/resources/database.properties
 */
public class DatabaseManager {

    private final String url;
    private final String user;
    private final String password;

    public DatabaseManager() {
        Properties props = new Properties();
        try (InputStream in = DatabaseManager.class.getClassLoader().getResourceAsStream("database.properties")) {
            if (in == null) {
                throw new IllegalStateException(
                        "Не найден файл database.properties (src/main/resources/database.properties).");
            }
            props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось прочитать database.properties: " + e.getMessage(), e);
        }

        this.url = require(props, "db.url");
        this.user = require(props, "db.user");
        this.password = require(props, "db.password");
    }

    private static String require(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null) {
            throw new IllegalStateException("В database.properties не задан параметр " + key + ".");
        }
        return value.trim();
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
