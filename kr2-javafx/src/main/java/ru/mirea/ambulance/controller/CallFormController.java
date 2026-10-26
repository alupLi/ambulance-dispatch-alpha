package ru.mirea.ambulance.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mirea.ambulance.exception.BusinessException;
import ru.mirea.ambulance.exception.DatabaseException;
import ru.mirea.ambulance.exception.EntityNotFoundException;
import ru.mirea.ambulance.model.Call;
import ru.mirea.ambulance.model.CallPriority;
import ru.mirea.ambulance.model.CallStatus;
import ru.mirea.ambulance.model.User;
import ru.mirea.ambulance.service.CallService;

import java.util.List;

/**
 * Контроллер формы добавления/редактирования вызова (call_form.fxml).
 * Диспетчер выбирается из списка (ComboBox), а не вводится по ID.
 * Проверку данных выполняет CallService, форма показывает понятное сообщение об ошибке.
 */
public class CallFormController {

    private static final Logger log = LoggerFactory.getLogger(CallFormController.class);

    @FXML private Label titleLabel;
    @FXML private ComboBox<User> dispatcherCombo;
    @FXML private TextField patientField;
    @FXML private TextField addressField;
    @FXML private TextField phoneField;
    @FXML private TextArea symptomsArea;
    @FXML private ComboBox<CallPriority> priorityCombo;
    @FXML private ComboBox<CallStatus> statusCombo;
    @FXML private Label errorLabel;
    @FXML private Button saveButton;

    private CallService callService;
    private Call editing;      // null — режим добавления
    private boolean saved;
    private int savedId;

    @FXML
    private void initialize() {
        priorityCombo.setItems(FXCollections.observableArrayList(CallPriority.values()));
        statusCombo.setItems(FXCollections.observableArrayList(CallStatus.values()));
        dispatcherCombo.setConverter(new StringConverter<User>() {
            @Override
            public String toString(User user) {
                return user == null ? "" : user.getFullName() + " (" + user.getLogin() + ")";
            }

            @Override
            public User fromString(String string) {
                return null;
            }
        });
        hideError();
    }

    public void init(CallService callService, List<User> dispatchers, Call editing) {
        this.callService = callService;
        this.editing = editing;
        dispatcherCombo.setItems(FXCollections.observableArrayList(dispatchers));

        if (editing == null) {
            titleLabel.setText("Новый вызов");
            priorityCombo.setValue(CallPriority.MEDIUM);
            statusCombo.setValue(CallStatus.NEW);
            statusCombo.setDisable(true); // новый вызов всегда создаётся со статусом «Новый»
        } else {
            titleLabel.setText("Вызов №" + editing.getId());
            for (User user : dispatchers) {
                if (user.getId() == editing.getDispatcherId()) {
                    dispatcherCombo.setValue(user);
                }
            }
            patientField.setText(editing.getPatientName());
            addressField.setText(editing.getAddress());
            phoneField.setText(editing.getPhone());
            symptomsArea.setText(editing.getSymptoms());
            priorityCombo.setValue(editing.getPriority());
            statusCombo.setValue(editing.getStatus());
        }
    }

    public boolean isSaved() {
        return saved;
    }

    public int getSavedId() {
        return savedId;
    }

    @FXML
    private void onSave() {
        hideError();
        User dispatcher = dispatcherCombo.getValue();
        Integer dispatcherId = dispatcher == null ? null : dispatcher.getId();
        try {
            Call result;
            if (editing == null) {
                result = callService.create(dispatcherId, patientField.getText(), addressField.getText(),
                        phoneField.getText(), symptomsArea.getText(), priorityCombo.getValue());
            } else {
                result = callService.update(editing.getId(), dispatcherId, patientField.getText(),
                        addressField.getText(), phoneField.getText(), symptomsArea.getText(),
                        priorityCombo.getValue(), statusCombo.getValue());
            }
            saved = true;
            savedId = result.getId();
            close();
        } catch (BusinessException | EntityNotFoundException e) {
            showError(e.getMessage());
        } catch (DatabaseException e) {
            log.error("Не удалось сохранить вызов", e);
            showError("Не удалось сохранить данные в базе данных. Проверьте подключение к PostgreSQL.");
        }
    }

    @FXML
    private void onCancel() {
        close();
    }

    private void close() {
        ((Stage) saveButton.getScene().getWindow()).close();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
