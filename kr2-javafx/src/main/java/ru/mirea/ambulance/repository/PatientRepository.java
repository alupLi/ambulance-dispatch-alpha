package ru.mirea.ambulance.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Доступ к таблице patients.
 * Пациент хранится один раз (ФИО + телефон), а вызовы ссылаются на него по patient_id.
 * Методы принимают уже открытое соединение, чтобы работать в одной транзакции с CallRepository.
 */
public class PatientRepository {

    /** Возвращает id пациента с таким ФИО и телефоном; если его нет — создаёт запись. */
    public int findOrCreate(Connection conn, String fullName, String phone) throws SQLException {
        String selectSql = "SELECT id FROM patients WHERE full_name = ? AND COALESCE(phone, '') = ?";
        try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
            ps.setString(1, fullName);
            ps.setString(2, phone == null ? "" : phone);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        String insertSql = "INSERT INTO patients (full_name, phone) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, fullName);
            ps.setString(2, phone);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Не удалось получить id созданного пациента.");
    }
}
