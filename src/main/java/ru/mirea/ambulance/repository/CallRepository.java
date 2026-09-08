package ru.mirea.ambulance.repository;

import ru.mirea.ambulance.model.Call;
import ru.mirea.ambulance.model.CallPriority;
import ru.mirea.ambulance.model.CallStatus;
import ru.mirea.ambulance.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CallRepository implements Repository<Call> {

    private final DatabaseManager databaseManager;

    public CallRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public Call save(Call call) {
        String sql = "INSERT INTO calls (dispatcher_id, patient_name, address, phone, symptoms, " +
                "priority, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, call.getDispatcherId());
            ps.setString(2, call.getPatientName());
            ps.setString(3, call.getAddress());
            ps.setString(4, call.getPhone());
            ps.setString(5, call.getSymptoms());
            ps.setString(6, call.getPriority().name());
            ps.setString(7, call.getStatus().name());
            ps.setTimestamp(8, Timestamp.valueOf(call.getCreatedAt()));
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    call.setId(keys.getInt(1));
                }
            }
            return call;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при сохранении вызова: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Call> findById(int id) {
        String sql = "SELECT * FROM calls WHERE id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при поиске вызова: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Call> findAll() {
        String sql = "SELECT * FROM calls ORDER BY id";
        List<Call> calls = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                calls.add(mapRow(rs));
            }
            return calls;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при получении списка вызовов: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Call call) {
        String sql = "UPDATE calls SET dispatcher_id = ?, patient_name = ?, address = ?, phone = ?, " +
                "symptoms = ?, priority = ?, status = ?, completed_at = ? WHERE id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, call.getDispatcherId());
            ps.setString(2, call.getPatientName());
            ps.setString(3, call.getAddress());
            ps.setString(4, call.getPhone());
            ps.setString(5, call.getSymptoms());
            ps.setString(6, call.getPriority().name());
            ps.setString(7, call.getStatus().name());
            ps.setTimestamp(8, call.getCompletedAt() != null ? Timestamp.valueOf(call.getCompletedAt()) : null);
            ps.setInt(9, call.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при обновлении вызова: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM calls WHERE id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при удалении вызова: " + e.getMessage(), e);
        }
    }

    private Call mapRow(ResultSet rs) throws SQLException {
        Timestamp completedTs = rs.getTimestamp("completed_at");
        return new Call(
                rs.getInt("id"),
                rs.getInt("dispatcher_id"),
                rs.getString("patient_name"),
                rs.getString("address"),
                rs.getString("phone"),
                rs.getString("symptoms"),
                CallPriority.valueOf(rs.getString("priority")),
                CallStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toLocalDateTime(),
                completedTs != null ? completedTs.toLocalDateTime() : null
        );
    }
}
