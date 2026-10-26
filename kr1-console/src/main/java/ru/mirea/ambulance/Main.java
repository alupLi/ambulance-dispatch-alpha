package ru.mirea.ambulance;

import ru.mirea.ambulance.repository.CallRepository;
import ru.mirea.ambulance.repository.UserRepository;
import ru.mirea.ambulance.service.CallService;
import ru.mirea.ambulance.service.UserService;
import ru.mirea.ambulance.ui.ConsoleUI;
import ru.mirea.ambulance.util.DatabaseManager;

public class Main {
    public static void main(String[] args) {
        DatabaseManager databaseManager = new DatabaseManager();

        UserRepository userRepository = new UserRepository(databaseManager);
        CallRepository callRepository = new CallRepository(databaseManager);

        UserService userService = new UserService(userRepository);
        CallService callService = new CallService(callRepository, userRepository);

        ConsoleUI ui = new ConsoleUI(userService, callService);
        ui.run();
    }
}