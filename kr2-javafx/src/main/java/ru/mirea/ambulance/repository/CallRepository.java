package ru.mirea.ambulance.repository;

import ru.mirea.ambulance.exception.DatabaseException;
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

/**
 * Доступ к таблице calls.
 * ФИО и телефон пациента хранятся в patients, приоритет и статус - в справочниках,
 * поэтому чтение выполняется через JOIN, а запись - в одной транзакции вместе с пациентом.
 */
public class CallRepository implements Repository<Call> {

    private static final String SELECT_CALLS =
            "SELECT c.id, c.dispatcher_id, p.full_name AS patient_name, p.phone AS patient_phone, "
                    + "c.address, c.symptoms, pr.code AS priority_code, st.code AS status_code, "
                    + "c.created_at, c.completed_at "
                    + "FROM calls c "
                    + "JOIN patients p ON p.id = c.patient_id "
                    + "JOIN call_priorities pr ON pr.id = c.priority_id "
                    + "JOIN call_statuses st ON st.id = c.status_id ";

    private static final String INSERT_CALL =
            "INSERT INTO calls (dispatcher_id, patient_id, address, symptoms, priority_id, status_id, created_at) "
                    + "VALUES (?, ?, ?, ?, "
                    + "(SELECT id FROM call_priorities WHERE code = ?), "
                    + "(SELECT id FROM call_statuses WHERE code = ?), ?)";

    private static final String UPDATE_CALL =
            "UPDATE calls SET dispatcher_id = ?, patient_id = ?, address = ?, symptoms = ?, "
                    + "priority_id = (SELECT id FROM call_priorities WHERE code = ?), "
                    + "status_id = (SELECT id FROM call_statuses WHERE code = ?), "
                    + "completed_at = ? WHERE id = ?";

    private final DatabaseManager databaseManager;
    private final PatientRepository patientRepository = new PatientRepository();

    public CallRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public Call save(Call call) {
        try (Connection conn = databaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int patientId = patientRepository.findOrCreate(conn, call.getPatientName(), call.getPhone());
                try (PreparedStatement ps = conn.prepareStatement(INSERT_CALL, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, call.getDispatcherId());
                    ps.setInt(2, patientId);
                    ps.setString(3, call.getAddress());
                    ps.setString(4, call.getSymptoms());
                    ps.setString(5, call.getPriority().name());
                    ps.setString(6, call.getStatus().name());
                    ps.setTimestamp(7, Timestamp.valueOf(call.getCreatedAt()));
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            call.setId(keys.getInt("id"));
                        }
                    }
                }
                conn.commit();
                return call;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при сохранении вызова: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Call> findById(int id) {
        String sql = SELECT_CALLS + "WHERE c.id = ?";
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
            throw new DatabaseException("Ошибка при поиске вызова: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Call> findAll() {
        String sql = SELECT_CALLS + "ORDER BY c.id";
        List<Call> calls = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                calls.add(mapRow(rs));
            }
            return calls;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при получении списка вызовов: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Call call) {
        try (Connection conn = databaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int patientId = patientRepository.findOrCreate(conn, call.getPatientName(), call.getPhone());
                try (PreparedStatement ps = conn.prepareStatement(UPDATE_CALL)) {
                    ps.setInt(1, call.getDispatcherId());
                    ps.setInt(2, patientId);
                    ps.setString(3, call.getAddress());
                    ps.setString(4, call.getSymptoms());
                    ps.setString(5, call.getPriority().name());
                    ps.setString(6, call.getStatus().name());
                    ps.setTimestamp(7, call.getCompletedAt() != null
                            ? Timestamp.valueOf(call.getCompletedAt()) : null);
                    ps.setInt(8, call.getId());
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при обновлении вызова: " + e.getMessage(), e);
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
            throw new DatabaseException("Ошибка при удалении вызова: " + e.getMessage(), e);
        }
    }

    private Call mapRow(ResultSet rs) throws SQLException {
        Timestamp completedTs = rs.getTimestamp("completed_at");
        return new Call(
                rs.getInt("id"),
                rs.getInt("dispatcher_id"),
                rs.getString("patient_name"),
                rs.getString("address"),
                rs.getString("patient_phone"),
                rs.getString("symptoms"),
                CallPriority.valueOf(rs.getString("priority_code")),
                CallStatus.valueOf(rs.getString("status_code")),
                rs.getTimestamp("created_at").toLocalDateTime(),
                completedTs != null ? completedTs.toLocalDateTime() : null
        );
    }
}
