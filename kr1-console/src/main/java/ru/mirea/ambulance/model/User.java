package ru.mirea.ambulance.model;

import java.util.Objects;

/**
 * Пользователь системы — диспетчер, принимающий вызовы.
 */
public class User {

    private int id;
    private String fullName;
    private String login;
    private String phone;

    public User() {
    }

    public User(String fullName, String login, String phone) {
        this.fullName = fullName;
        this.login = login;
        this.phone = phone;
    }

    public User(int id, String fullName, String login, String phone) {
        this.id = id;
        this.fullName = fullName;
        this.login = login;
        this.phone = phone;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return id == user.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("#%d | %-20s | login: %-10s | тел: %s", id, fullName, login, phone);
    }
}
