package ru.mirea.ambulance.repository;

import java.util.List;
import java.util.Optional;

/**
 * Общий контракт для доступа к данным (CRUD).
 * Реализуется репозиториями каждой сущности.
 */
public interface Repository<T> {
    T save(T entity);

    Optional<T> findById(int id);

    List<T> findAll();

    void update(T entity);

    void delete(int id);
}
