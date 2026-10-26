package ru.mirea.ambulance.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Вызов скорой помощи — основная сущность предметной области.
 */
public class Call {

    private int id;
    private int dispatcherId;       // FK -> users.id
    private String patientName;
    private String address;
    private String phone;
    private String symptoms;
    private CallPriority priority;
    private CallStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt; // может быть null

    public Call() {
    }

    public Call(int dispatcherId, String patientName, String address, String phone,
                String symptoms, CallPriority priority) {
        this.dispatcherId = dispatcherId;
        this.patientName = patientName;
        this.address = address;
        this.phone = phone;
        this.symptoms = symptoms;
        this.priority = priority;
        this.status = CallStatus.NEW;
        this.createdAt = LocalDateTime.now();
    }

    public Call(int id, int dispatcherId, String patientName, String address, String phone,
                String symptoms, CallPriority priority, CallStatus status,
                LocalDateTime createdAt, LocalDateTime completedAt) {
        this.id = id;
        this.dispatcherId = dispatcherId;
        this.patientName = patientName;
        this.address = address;
        this.phone = phone;
        this.symptoms = symptoms;
        this.priority = priority;
        this.status = status;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDispatcherId() {
        return dispatcherId;
    }

    public void setDispatcherId(int dispatcherId) {
        this.dispatcherId = dispatcherId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(String symptoms) {
        this.symptoms = symptoms;
    }

    public CallPriority getPriority() {
        return priority;
    }

    public void setPriority(CallPriority priority) {
        this.priority = priority;
    }

    public CallStatus getStatus() {
        return status;
    }

    public void setStatus(CallStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Call)) return false;
        Call call = (Call) o;
        return id == call.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format(
                "#%d | %-15s | %-20s | тел: %-12s | приоритет: %-8s | статус: %-11s | создан: %s",
                id, patientName, address, phone, priority, status, createdAt);
    }
}
