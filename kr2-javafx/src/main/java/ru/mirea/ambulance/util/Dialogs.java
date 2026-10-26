package ru.mirea.ambulance.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;
import javafx.stage.Window;

/**
 * Стандартные диалоги JavaFX: ошибка, информация, подтверждение.
 */
public final class Dialogs {

    private Dialogs() {
    }

    public static void error(Window owner, String title, String message) {
        show(Alert.AlertType.ERROR, owner, title, message);
    }

    public static void info(Window owner, String title, String message) {
        show(Alert.AlertType.INFORMATION, owner, title, message);
    }

    public static boolean confirm(Window owner, String title, String message, String confirmText) {
        ButtonType yes = new ButtonType(confirmText, ButtonBar.ButtonData.OK_DONE);
        ButtonType no = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, yes, no);
        prepare(alert, owner, title);
        return alert.showAndWait().filter(button -> button == yes).isPresent();
    }

    private static void show(Alert.AlertType type, Window owner, String title, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        prepare(alert, owner, title);
        alert.showAndWait();
    }

    private static void prepare(Alert alert, Window owner, String title) {
        alert.setTitle(title);
        alert.setHeaderText(null);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
    }
}
