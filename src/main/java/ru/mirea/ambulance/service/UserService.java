package ru.mirea.ambulance.service;

import ru.mirea.ambulance.exception.BusinessException;
import ru.mirea.ambulance.exception.EntityNotFoundException;
import ru.mirea.ambulance.model.User;
import ru.mirea.ambulance.repository.UserRepository;

import java.util.List;

public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User create(String fullName, String login, String phone) {
        if (fullName == null || fullName.isBlank()) {
            throw new BusinessException("ФИО диспетчера не может быть пустым.");
        }
        if (login == null || login.isBlank()) {
            throw new BusinessException("Логин диспетчера не может быть пустым.");
        }
        return userRepository.save(new User(fullName.trim(), login.trim(), phone));
    }

    public User getById(int id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Диспетчер с ID " + id + " не найден."));
    }

    public List<User> getAll() {
        return userRepository.findAll();
    }

    public void update(int id, String fullName, String login, String phone) {
        User user = getById(id);
        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName.trim());
        }
        if (login != null && !login.isBlank()) {
            user.setLogin(login.trim());
        }
        if (phone != null && !phone.isBlank()) {
            user.setPhone(phone.trim());
        }
        userRepository.update(user);
    }

    public void delete(int id) {
        getById(id); // бросит EntityNotFoundException, если диспетчера нет
        userRepository.delete(id);
    }
}
