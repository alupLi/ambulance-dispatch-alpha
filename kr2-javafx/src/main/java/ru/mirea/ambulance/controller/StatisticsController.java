package ru.mirea.ambulance.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import ru.mirea.ambulance.service.StatRow;

import java.util.List;

/**
 * Контроллер окна статистики (statistics.fxml): таблица «показатель — значение».
 */
public class StatisticsController {

    @FXML private TableView<StatRow> statsTable;
    @FXML private TableColumn<StatRow, String> nameColumn;
    @FXML private TableColumn<StatRow, String> valueColumn;

    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().name()));
        valueColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().value()));
    }

    public void init(List<StatRow> rows) {
        statsTable.setItems(FXCollections.observableArrayList(rows));
    }

    @FXML
    private void onClose() {
        ((Stage) statsTable.getScene().getWindow()).close();
    }
}
