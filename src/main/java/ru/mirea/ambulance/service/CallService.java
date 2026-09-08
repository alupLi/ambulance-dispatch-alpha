package ru.mirea.ambulance.service;

import ru.mirea.ambulance.exception.BusinessException;
import ru.mirea.ambulance.exception.EntityNotFoundException;
import ru.mirea.ambulance.model.Call;
import ru.mirea.ambulance.model.CallPriority;
import ru.mirea.ambulance.model.CallStatus;
import ru.mirea.ambulance.repository.CallRepository;
import ru.mirea.ambulance.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Бизнес-логика по вызовам скорой помощи.
 * Проверки выполняются здесь, а не в консольном меню.
 */
public class CallService {

    private static final Set<CallStatus> FINAL_STATUSES = EnumSet.of(CallStatus.COMPLETED, CallStatus.CANCELLED);

    private final CallRepository callRepository;
    private final UserRepository userRepository;

    public CallService(CallRepository callRepository, UserRepository userRepository) {
        this.callRepository = callRepository;
        this.userRepository = userRepository;
    }

    public Call create(int dispatcherId, String patientName, String address, String phone,
                        String symptoms, CallPriority priority) {
        // Бизнес-правило 1: адрес и ФИО пациента обязательны
        if (address == null || address.isBlank()) {
            throw new BusinessException("Нельзя создать вызов без адреса.");
        }
        if (patientName == null || patientName.isBlank()) {
            throw new BusinessException("Нельзя создать вызов без ФИО пациента.");
        }
        // Бизнес-правило 2: диспетчер должен существовать
        userRepository.findById(dispatcherId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Диспетчер с ID " + dispatcherId + " не найден, вызов не создан."));
        // Бизнес-правило 3: приоритет обязателен и должен быть корректным значением enum
        if (priority == null) {
            throw new BusinessException("Не указан корректный приоритет вызова.");
        }

        return callRepository.save(new Call(dispatcherId, patientName.trim(), address.trim(),
                phone, symptoms, priority));
    }

    public Call getById(int id) {
        return callRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Вызов с ID " + id + " не найден."));
    }

    public List<Call> getAll() {
        return callRepository.findAll();
    }

    public void changeStatus(int id, CallStatus newStatus) {
        Call call = getById(id);
        // Бизнес-правило 4: нельзя вернуть завершённый/отменённый вызов в работу
        if (FINAL_STATUSES.contains(call.getStatus()) && !FINAL_STATUSES.contains(newStatus)) {
            throw new BusinessException("Нельзя изменить статус завершённого или отменённого вызова.");
        }
        call.setStatus(newStatus);
        if (FINAL_STATUSES.contains(newStatus)) {
            call.setCompletedAt(LocalDateTime.now());
        }
        callRepository.update(call);
    }

    public void update(int id, String patientName, String address, String phone,
                        String symptoms, CallPriority priority) {
        Call call = getById(id);
        if (patientName != null && !patientName.isBlank()) {
            call.setPatientName(patientName.trim());
        }
        if (address != null && !address.isBlank()) {
            call.setAddress(address.trim());
        }
        if (phone != null && !phone.isBlank()) {
            call.setPhone(phone.trim());
        }
        if (symptoms != null) {
            call.setSymptoms(symptoms);
        }
        if (priority != null) {
            call.setPriority(priority);
        }
        callRepository.update(call);
    }

    public void delete(int id) {
        Call call = getById(id);
        // Бизнес-правило 5: нельзя удалить вызов, который сейчас в работе
        if (call.getStatus() == CallStatus.IN_PROGRESS) {
            throw new BusinessException("Нельзя удалить вызов, находящийся в работе (IN_PROGRESS).");
        }
        callRepository.delete(id);
    }

    // ---------- Поиск ----------

    public List<Call> searchByPatientName(String query) {
        String needle = query.toLowerCase();
        return callRepository.findAll().stream()
                .filter(c -> c.getPatientName().toLowerCase().contains(needle))
                .toList();
    }

    public List<Call> searchByAddress(String query) {
        String needle = query.toLowerCase();
        return callRepository.findAll().stream()
                .filter(c -> c.getAddress().toLowerCase().contains(needle))
                .toList();
    }

    // ---------- Фильтрация ----------

    public List<Call> filterByStatus(CallStatus status) {
        return callRepository.findAll().stream()
                .filter(c -> c.getStatus() == status)
                .toList();
    }

    public List<Call> filterByPriority(CallPriority priority) {
        return callRepository.findAll().stream()
                .filter(c -> c.getPriority() == priority)
                .toList();
    }

    public List<Call> filterByDateRange(LocalDate from, LocalDate to) {
        return callRepository.findAll().stream()
                .filter(c -> {
                    LocalDate created = c.getCreatedAt().toLocalDate();
                    return !created.isBefore(from) && !created.isAfter(to);
                })
                .toList();
    }

    // ---------- Сортировка ----------

    public List<Call> sortByDateCreated() {
        return callRepository.findAll().stream()
                .sorted(Comparator.comparing(Call::getCreatedAt))
                .toList();
    }

    public List<Call> sortByPriority() {
        return callRepository.findAll().stream()
                .sorted(Comparator.comparing(Call::getPriority))
                .toList();
    }

    // ---------- Статистика ----------

    public CallStatistics getStatistics() {
        List<Call> all = callRepository.findAll();
        long totalDispatchers = userRepository.findAll().size();
        long newCalls = all.stream().filter(c -> c.getStatus() == CallStatus.NEW).count();
        long inProgress = all.stream().filter(c -> c.getStatus() == CallStatus.IN_PROGRESS).count();
        long completed = all.stream().filter(c -> c.getStatus() == CallStatus.COMPLETED).count();
        long cancelled = all.stream().filter(c -> c.getStatus() == CallStatus.CANCELLED).count();
        long highOrCritical = all.stream()
                .filter(c -> c.getPriority() == CallPriority.HIGH || c.getPriority() == CallPriority.CRITICAL)
                .count();

        return new CallStatistics(totalDispatchers, all.size(), newCalls, inProgress, completed,
                cancelled, highOrCritical);
    }
}
