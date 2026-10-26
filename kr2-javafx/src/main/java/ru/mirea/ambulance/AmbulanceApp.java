package ru.mirea.ambulance;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.mirea.ambulance.controller.MainController;
import ru.mirea.ambulance.repository.CallRepository;
import ru.mirea.ambulance.repository.UserRepository;
import ru.mirea.ambulance.service.CallService;
import ru.mirea.ambulance.service.ExportService;
import ru.mirea.ambulance.service.UserService;
import ru.mirea.ambulance.util.DatabaseManager;
import ru.mirea.ambulance.util.Dialogs;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Точка входа JavaFX. Stage — окно, Scene — содержимое окна, Node — элементы внутри Scene.
 * Здесь собираются слои: DatabaseManager -> Repository -> Service -> Controller.
 */
public class AmbulanceApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        DatabaseManager databaseManager;
        try {
            databaseManager = new DatabaseManager();
            // Проверяем подключение до открытия окна
            try (Connection ignored = databaseManager.getConnection()) {
                // соединение открыто и сразу закрывается
            }
        } catch (IllegalStateException | SQLException e) {
            Dialogs.error(null, "Нет подключения к базе данных",
                    "Не удалось подключиться к PostgreSQL. Проверьте, что сервер запущен, база создана "
                            + "(sql/schema.sql), а параметры в database.properties указаны верно.\n\n"
                            + "Подробности: " + e.getMessage());
            Platform.exit();
            return;
        }

        UserRepository userRepository = new UserRepository(databaseManager);
        CallRepository callRepository = new CallRepository(databaseManager);
        UserService userService = new UserService(userRepository);
        CallService callService = new CallService(callRepository, userRepository);
        ExportService exportService = new ExportService();

        FXMLLoader loader = new FXMLLoader(AmbulanceApp.class.getResource("/fxml/main.fxml"));
        Parent root = loader.load();
        MainController controller = loader.getController();

        Scene scene = new Scene(root, 1180, 720);
        scene.getStylesheets().add(AmbulanceApp.class.getResource("/css/style.css").toExternalForm());
        stage.setTitle("Скорая помощь — диспетчерская");
        stage.setMinWidth(1000);
        stage.setMinHeight(580);
        stage.setScene(scene);
        stage.show();

        controller.init(callService, userService, exportService);
    }
}
