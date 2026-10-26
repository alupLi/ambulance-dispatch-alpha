package ru.mirea.ambulance.service;

import ru.mirea.ambulance.exception.BusinessException;
import ru.mirea.ambulance.exception.EntityNotFoundException;
import ru.mirea.ambulance.model.Call;
import ru.mirea.ambulance.model.CallPriority;
import ru.mirea.ambulance.model.CallStatus;
import ru.mirea.ambulance.model.User;
import ru.mirea.ambulance.repository.CallRepository;
import ru.mirea.ambulance.repository.UserRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Бизнес-логика по вызовам скорой помощи: валидация, бизнес-правила,
 * поиск/фильтрация/сортировка и статистика.
 * Контроллеры JavaFX работают только через этот класс, SQL здесь нет.
 */
public class CallService {

    public static final int MAX_PATIENT_NAME = 150;
    public static final int MAX_ADDRESS = 255;
    public static final int MAX_SYMPTOMS = 500;

    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+0-9()\\-\\s]{5,20}$");
    private static final Set<CallStatus> FINAL_STATUSES = EnumSet.of(CallStatus.COMPLETED, CallStatus.CANCELLED);

    private final CallRepository callRepository;
    private final UserRepository userRepository;

    public CallService(CallRepository callRepository, UserRepository userRepository) {
        this.callRepository = callRepository;
        this.userRepository = userRepository;
    }

    // ------------------------------------------------------------------
    // CRUD + бизнес-правила
    // ------------------------------------------------------------------

    public Call create(Integer dispatcherId, String patientName, String address, String phone,
                       String symptoms, CallPriority priority) {
        validate(dispatcherId, patientName, address, phone, symptoms, priority);
        Call call = new Call(dispatcherId, patientName.trim(), address.trim(),
                blankToNull(phone), blankToNull(symptoms), priority);
        return callRepository.save(call);
    }

    public Call update(int id, Integer dispatcherId, String patientName, String address, String phone,
                       String symptoms, CallPriority priority, CallStatus status) {
        Call call = getById(id);
        validate(dispatcherId, patientName, address, phone, symptoms, priority);
        if (status == null) {
            throw new BusinessException("Поле «Статус» обязательно: выберите статус вызова.");
        }
        // Бизнес-правило: завершённый или отменённый вызов нельзя вернуть в работу
        if (FINAL_STATUSES.contains(call.getStatus()) && !FINAL_STATUSES.contains(status)) {
            throw new BusinessException("Нельзя изменить статус «" + call.getStatus()
                    + "» на «" + status + "»: завершённый или отменённый вызов нельзя вернуть в работу.");
        }
        if (status != call.getStatus() && FINAL_STATUSES.contains(status)) {
            call.setCompletedAt(LocalDateTime.now());
        }
        call.setDispatcherId(dispatcherId);
        call.setPatientName(patientName.trim());
        call.setAddress(address.trim());
        call.setPhone(blankToNull(phone));
        call.setSymptoms(blankToNull(symptoms));
        call.setPriority(priority);
        call.setStatus(status);
        callRepository.update(call);
        return call;
    }

    public void delete(int id) {
        Call call = getById(id);
        // Бизнес-правило: нельзя удалить вызов, который сейчас в работе
        if (call.getStatus() == CallStatus.IN_PROGRESS) {
            throw new BusinessException("Нельзя удалить вызов №" + id
                    + ": он находится в работе. Сначала завершите или отмените его.");
        }
        callRepository.delete(id);
    }

    public Call getById(int id) {
        return callRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Вызов №" + id + " не найден в базе данных."));
    }

    public List<Call> getAll() {
        return callRepository.findAll();
    }

    // ------------------------------------------------------------------
    // Поиск, фильтрация, сортировка (работает с уже загруженным списком)
    // ------------------------------------------------------------------

    public List<Call> filterAndSort(List<Call> source, CallQuery query) {
        if (query.dateFrom() != null && query.dateTo() != null && query.dateFrom().isAfter(query.dateTo())) {
            throw new BusinessException("Дата «с» не может быть позже даты «по».");
        }
        String needle = query.search() == null ? "" : query.search().trim().toLowerCase();
        CallSort sort = query.sort() == null ? CallSort.DATE_DESC : query.sort();

        return source.stream()
                .filter(c -> needle.isEmpty() || matchesSearch(c, needle))
                .filter(c -> query.status() == null || c.getStatus() == query.status())
                .filter(c -> query.priority() == null || c.getPriority() == query.priority())
                .filter(c -> query.dateFrom() == null
                        || !c.getCreatedAt().toLocalDate().isBefore(query.dateFrom()))
                .filter(c -> query.dateTo() == null
                        || !c.getCreatedAt().toLocalDate().isAfter(query.dateTo()))
                .sorted(sort.comparator())
                .toList();
    }

    private boolean matchesSearch(Call c, String needle) {
        return contains(c.getPatientName(), needle)
                || contains(c.getAddress(), needle)
                || contains(c.getPhone(), needle)
                || contains(c.getSymptoms(), needle);
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }

    // ------------------------------------------------------------------
    // Статистика
    // ------------------------------------------------------------------

    public List<StatRow> getStatistics() {
        List<Call> calls = callRepository.findAll();
        List<User> users = userRepository.findAll();
        int total = calls.size();

        long urgent = calls.stream()
                .filter(c -> c.getPriority() == CallPriority.HIGH || c.getPriority() == CallPriority.CRITICAL)
                .count();

        OptionalDouble avgMinutes = calls.stream()
                .filter(c -> c.getStatus() == CallStatus.COMPLETED && c.getCompletedAt() != null)
                .mapToLong(c -> Duration.between(c.getCreatedAt(), c.getCompletedAt()).toMinutes())
                .average();

        LocalDateTime dayAgo = LocalDateTime.now().minusHours(24);
        long last24h = calls.stream().filter(c -> c.getCreatedAt().isAfter(dayAgo)).count();

        Map<Integer, Long> perDispatcher = calls.stream()
                .collect(Collectors.groupingBy(Call::getDispatcherId, Collectors.counting()));
        String busiest = perDispatcher.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> users.stream()
                        .filter(u -> u.getId() == e.getKey())
                        .findFirst()
                        .map(User::getFullName)
                        .orElse("ID " + e.getKey()) + " (" + e.getValue() + ")")
                .orElse("—");

        return List.of(
                new StatRow("Всего вызовов", String.valueOf(total)),
                new StatRow("Диспетчеров в системе", String.valueOf(users.size())),
                new StatRow("Новых", withPercent(countByStatus(calls, CallStatus.NEW), total)),
                new StatRow("В работе", withPercent(countByStatus(calls, CallStatus.IN_PROGRESS), total)),
                new StatRow("Завершённых", withPercent(countByStatus(calls, CallStatus.COMPLETED), total)),
                new StatRow("Отменённых", withPercent(countByStatus(calls, CallStatus.CANCELLED), total)),
                new StatRow("Срочных (высокий и критический приоритет)", withPercent(urgent, total)),
                new StatRow("Среднее время обработки завершённых вызовов",
                        avgMinutes.isPresent() ? String.format("%.0f мин", avgMinutes.getAsDouble()) : "—"),
                new StatRow("Вызовов за последние 24 часа", String.valueOf(last24h)),
                new StatRow("Больше всего вызовов принял", busiest)
        );
    }

    private long countByStatus(List<Call> calls, CallStatus status) {
        return calls.stream().filter(c -> c.getStatus() == status).count();
    }

    private String withPercent(long part, long total) {
        double percent = total == 0 ? 0.0 : part * 100.0 / total;
        return String.format("%d (%.0f%%)", part, percent);
    }

    // ------------------------------------------------------------------
    // Валидация
    // ------------------------------------------------------------------

    private void validate(Integer dispatcherId, String patientName, String address, String phone,
                          String symptoms, CallPriority priority) {
        if (dispatcherId == null) {
            throw new BusinessException("Поле «Диспетчер» обязательно: выберите диспетчера из списка.");
        }
        requireText(patientName, "ФИО пациента", MAX_PATIENT_NAME);
        requireText(address, "Адрес", MAX_ADDRESS);
        if (phone != null && !phone.isBlank() && !PHONE_PATTERN.matcher(phone.trim()).matches()) {
            throw new BusinessException("Поле «Телефон» заполнено неверно: допустимы цифры, пробелы и "
                    + "символы + - ( ), всего от 5 до 20 символов.");
        }
        if (symptoms != null && symptoms.trim().length() > MAX_SYMPTOMS) {
            throw new BusinessException("Поле «Симптомы» слишком длинное: максимум " + MAX_SYMPTOMS + " символов.");
        }
        if (priority == null) {
            throw new BusinessException("Поле «Приоритет» обязательно: выберите приоритет вызова.");
        }
        // Бизнес-правило: диспетчер должен существовать в БД
        userRepository.findById(dispatcherId).orElseThrow(() ->
                new EntityNotFoundException("Выбранный диспетчер не найден в базе данных. Обновите список."));
    }

    private void requireText(String value, String field, int max) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("Поле «" + field + "» обязательно для заполнения.");
        }
        if (value.trim().length() > max) {
            throw new BusinessException("Поле «" + field + "» слишком длинное: максимум " + max + " символов.");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
