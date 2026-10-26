package ru.mirea.ambulance.controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mirea.ambulance.exception.BusinessException;
import ru.mirea.ambulance.exception.DatabaseException;
import ru.mirea.ambulance.exception.EntityNotFoundException;
import ru.mirea.ambulance.exception.ExportException;
import ru.mirea.ambulance.model.Call;
import ru.mirea.ambulance.model.CallPriority;
import ru.mirea.ambulance.model.CallStatus;
import ru.mirea.ambulance.model.User;
import ru.mirea.ambulance.service.CallQuery;
import ru.mirea.ambulance.service.CallService;
import ru.mirea.ambulance.service.CallSort;
import ru.mirea.ambulance.service.ExportService;
import ru.mirea.ambulance.service.StatRow;
import ru.mirea.ambulance.service.UserService;
import ru.mirea.ambulance.util.Dialogs;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Контроллер главного окна (main.fxml).
 * Принимает действия пользователя и передаёт их сервисам; SQL и бизнес-правил здесь нет.
 */
public class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    // --- элементы интерфейса из main.fxml (связь по fx:id) ---
    @FXML private TextField searchField;
    @FXML private ComboBox<CallStatus> statusFilter;
    @FXML private ComboBox<CallPriority> priorityFilter;
    @FXML private DatePicker dateFromPicker;
    @FXML private DatePicker dateToPicker;
    @FXML private ComboBox<CallSort> sortCombo;
    @FXML private Button editButton;
    @FXML private Button deleteButton;

    @FXML private TableView<Call> callsTable;
    @FXML private TableColumn<Call, Integer> idColumn;
    @FXML private TableColumn<Call, String> patientColumn;
    @FXML private TableColumn<Call, String> addressColumn;
    @FXML private TableColumn<Call, String> phoneColumn;
    @FXML private TableColumn<Call, CallPriority> priorityColumn;
    @FXML private TableColumn<Call, CallStatus> statusColumn;
    @FXML private TableColumn<Call, String> dispatcherColumn;
    @FXML private TableColumn<Call, LocalDateTime> createdColumn;

    @FXML private Label detailsLabel;
    @FXML private Label statusBarLabel;

    // --- данные и сервисы ---
    private final ObservableList<Call> tableItems = FXCollections.observableArrayList();
    private final Map<Integer, User> dispatchersById = new HashMap<>();
    private List<Call> loaded = new ArrayList<>();

    private CallService callService;
    private UserService userService;
    private ExportService exportService;

    /** Вызывается FXMLLoader после создания элементов интерфейса. */
    @FXML
    private void initialize() {
        setupColumns();
        callsTable.setItems(tableItems);
        callsTable.setPlaceholder(new Label("Вызовов по заданным условиям нет"));
        callsTable.setRowFactory(tv -> createRow());
        callsTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, selected) -> showDetails(selected));
        editButton.disableProperty().bind(callsTable.getSelectionModel().selectedItemProperty().isNull());
        deleteButton.disableProperty().bind(callsTable.getSelectionModel().selectedItemProperty().isNull());

        statusFilter.setItems(withAllOption(Arrays.asList(CallStatus.values())));
        configureNullableCombo(statusFilter, "Все статусы");
        priorityFilter.setItems(withAllOption(Arrays.asList(CallPriority.values())));
        configureNullableCombo(priorityFilter, "Все приоритеты");
        sortCombo.setItems(FXCollections.observableArrayList(CallSort.values()));
        sortCombo.setValue(CallSort.DATE_DESC);
        showDetails(null);
    }

    /** Передаёт контроллеру сервисы и загружает данные. Вызывается из AmbulanceApp. */
    public void init(CallService callService, UserService userService, ExportService exportService) {
        this.callService = callService;
        this.userService = userService;
        this.exportService = exportService;

        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
        statusFilter.valueProperty().addListener((obs, o, n) -> applyFilters());
        priorityFilter.valueProperty().addListener((obs, o, n) -> applyFilters());
        dateFromPicker.valueProperty().addListener((obs, o, n) -> applyFilters());
        dateToPicker.valueProperty().addListener((obs, o, n) -> applyFilters());
        sortCombo.valueProperty().addListener((obs, o, n) -> {
            callsTable.getSortOrder().clear(); // выбранная сортировка важнее клика по заголовку
            applyFilters();
        });

        refresh();
    }

    // Обработчики кнопок (onAction в main.fxml)

    @FXML
    private void onAdd() {
        openForm(null);
    }

    @FXML
    private void onEdit() {
        Call selected = callsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openForm(selected);
        }
    }

    @FXML
    private void onDelete() {
        Call selected = callsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        boolean confirmed = Dialogs.confirm(owner(), "Удаление вызова",
                "Удалить вызов №" + selected.getId() + " (пациент: " + selected.getPatientName() + ")?\n"
                        + "Это действие нельзя отменить.", "Удалить");
        if (!confirmed) {
            return;
        }
        try {
            callService.delete(selected.getId());
            refresh();
        } catch (BusinessException | EntityNotFoundException e) {
            Dialogs.error(owner(), "Удаление невозможно", e.getMessage());
            refresh();
        } catch (DatabaseException e) {
            showDatabaseError(e);
        }
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    @FXML
    private void onResetFilters() {
        searchField.clear();
        statusFilter.setValue(null);
        priorityFilter.setValue(null);
        dateFromPicker.setValue(null);
        dateToPicker.setValue(null);
        sortCombo.setValue(CallSort.DATE_DESC);
        callsTable.getSortOrder().clear();
        applyFilters();
    }

    @FXML
    private void onStatistics() {
        try {
            List<StatRow> rows = callService.getStatistics();
            showModal("/fxml/statistics.fxml", "Статистика",
                    (StatisticsController controller) -> controller.init(rows));
        } catch (DatabaseException e) {
            showDatabaseError(e);
        } catch (IOException e) {
            showWindowError(e);
        }
    }

    @FXML
    private void onExport() {
        if (tableItems.isEmpty()) {
            Dialogs.info(owner(), "Экспорт данных", "В таблице нет данных для экспорта.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Экспорт вызовов");
        FileChooser.ExtensionFilter xlsx = new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx");
        FileChooser.ExtensionFilter csv = new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv");
        chooser.getExtensionFilters().addAll(xlsx, csv);
        chooser.setSelectedExtensionFilter(xlsx);
        chooser.setInitialFileName("calls_export");

        File file = chooser.showSaveDialog(owner());
        if (file == null) {
            return; // пользователь закрыл окно выбора файла
        }
        String lower = file.getName().toLowerCase();
        if (!lower.endsWith(".xlsx") && !lower.endsWith(".csv")) {
            String extension = chooser.getSelectedExtensionFilter() == csv ? ".csv" : ".xlsx";
            file = new File(file.getParentFile(), file.getName() + extension);
        }

        try {
            // экспортируется то, что сейчас показано в таблице (с учётом поиска, фильтров и сортировки)
            exportService.export(new ArrayList<>(tableItems), new ArrayList<>(dispatchersById.values()), file);
            Dialogs.info(owner(), "Экспорт данных",
                    "Экспортировано записей: " + tableItems.size() + "\nФайл: " + file.getAbsolutePath());
        } catch (BusinessException | ExportException e) {
            Dialogs.error(owner(), "Ошибка экспорта", e.getMessage());
        }
    }

    // Загрузка данных, фильтры, отображение

    private void refresh() {
        Integer keepId = selectedId();
        try {
            loaded = callService.getAll();
            dispatchersById.clear();
            for (User user : userService.getAll()) {
                dispatchersById.put(user.getId(), user);
            }
        } catch (DatabaseException e) {
            showDatabaseError(e);
            return;
        }
        applyFilters();
        if (keepId != null) {
            selectById(keepId);
        }
    }

    private void applyFilters() {
        CallQuery query = new CallQuery(searchField.getText(), statusFilter.getValue(),
                priorityFilter.getValue(), dateFromPicker.getValue(), dateToPicker.getValue(),
                sortCombo.getValue());
        try {
            List<Call> result = callService.filterAndSort(loaded, query);
            tableItems.setAll(result);
            setStatusBar("Показано вызовов: " + result.size() + " из " + loaded.size(), false);
        } catch (BusinessException e) {
            setStatusBar("Ошибка в условиях фильтра: " + e.getMessage(), true);
        }
    }

    private void setStatusBar(String text, boolean error) {
        statusBarLabel.setText(text);
        statusBarLabel.getStyleClass().remove("error-text");
        if (error) {
            statusBarLabel.getStyleClass().add("error-text");
        }
    }

    private void showDetails(Call call) {
        if (call == null) {
            detailsLabel.setText("Выберите вызов в таблице, чтобы увидеть подробные сведения.");
            return;
        }
        User dispatcher = dispatchersById.get(call.getDispatcherId());
        String dispatcherText = dispatcher == null
                ? "не найден"
                : dispatcher.getFullName() + " (логин: " + dispatcher.getLogin()
                + ", тел.: " + dash(dispatcher.getPhone()) + ")";
        detailsLabel.setText(String.format(
                "Вызов №%d  •  статус: %s  •  приоритет: %s%n"
                        + "Пациент: %s  •  телефон: %s%n"
                        + "Адрес: %s%n"
                        + "Симптомы: %s%n"
                        + "Принял диспетчер: %s%n"
                        + "Создан: %s  •  завершён: %s",
                call.getId(), call.getStatus(), call.getPriority(),
                call.getPatientName(), dash(call.getPhone()),
                call.getAddress(),
                dash(call.getSymptoms()),
                dispatcherText,
                call.getCreatedAt().format(DATE_TIME),
                call.getCompletedAt() == null ? "—" : call.getCompletedAt().format(DATE_TIME)));
    }

    // Форма добавления / редактирования

    private void openForm(Call existing) {
        try {
            List<User> dispatchers = userService.getAll();
            if (dispatchers.isEmpty()) {
                Dialogs.error(owner(), "Нет диспетчеров",
                        "В базе данных нет ни одного диспетчера, поэтому вызов создать нельзя.");
                return;
            }
            String title = existing == null ? "Новый вызов" : "Редактирование вызова №" + existing.getId();
            CallFormController form = showModal("/fxml/call_form.fxml", title,
                    (CallFormController controller) -> controller.init(callService, dispatchers, existing));
            if (form.isSaved()) {
                refresh();
                selectById(form.getSavedId());
            }
        } catch (DatabaseException e) {
            showDatabaseError(e);
        } catch (IOException e) {
            showWindowError(e);
        }
    }

    /** Открывает модальное окно из FXML, настраивает его контроллер и ждёт закрытия. */
    private <C> C showModal(String fxmlPath, String title, Consumer<C> setup) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent root = loader.load();
        C controller = loader.getController();
        setup.accept(controller);

        Stage stage = new Stage();
        stage.setTitle(title);
        stage.initOwner(owner());
        stage.initModality(Modality.WINDOW_MODAL);
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        stage.setScene(scene);
        stage.showAndWait();
        return controller;
    }

    // Настройка таблицы и ComboBox

    private void setupColumns() {
        idColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getId()));
        patientColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getPatientName()));
        addressColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getAddress()));
        phoneColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(dash(c.getValue().getPhone())));
        priorityColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getPriority()));
        statusColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getStatus()));
        dispatcherColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(dispatcherName(c.getValue())));
        createdColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getCreatedAt()));
        createdColumn.setCellFactory(column -> new TableCell<Call, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(DATE_TIME));
            }
        });
    }

    /** Строка таблицы: двойной клик открывает редактирование, цвет зависит от приоритета и статуса. */
    private TableRow<Call> createRow() {
        TableRow<Call> row = new TableRow<Call>() {
            @Override
            protected void updateItem(Call call, boolean empty) {
                super.updateItem(call, empty);
                getStyleClass().removeAll("row-critical", "row-high", "row-closed");
                if (empty || call == null) {
                    return;
                }
                boolean closed = call.getStatus() == CallStatus.COMPLETED
                        || call.getStatus() == CallStatus.CANCELLED;
                if (closed) {
                    getStyleClass().add("row-closed");
                } else if (call.getPriority() == CallPriority.CRITICAL) {
                    getStyleClass().add("row-critical");
                } else if (call.getPriority() == CallPriority.HIGH) {
                    getStyleClass().add("row-high");
                }
            }
        };
        row.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && !row.isEmpty()) {
                openForm(row.getItem());
            }
        });
        return row;
    }

    private <T> ObservableList<T> withAllOption(List<T> values) {
        ObservableList<T> items = FXCollections.observableArrayList();
        items.add(null); // null означает «Все»
        items.addAll(values);
        return items;
    }

    private <T> void configureNullableCombo(ComboBox<T> combo, String allLabel) {
        combo.setCellFactory(listView -> new ListCell<T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : (item == null ? allLabel : item.toString()));
            }
        });
        combo.setButtonCell(new ListCell<T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null ? allLabel : item.toString());
            }
        });
    }

    // Вспомогательные методы

    private String dispatcherName(Call call) {
        User dispatcher = dispatchersById.get(call.getDispatcherId());
        return dispatcher == null ? "—" : dispatcher.getFullName();
    }

    private Integer selectedId() {
        Call selected = callsTable.getSelectionModel().getSelectedItem();
        return selected == null ? null : selected.getId();
    }

    private void selectById(int id) {
        for (Call call : tableItems) {
            if (call.getId() == id) {
                callsTable.getSelectionModel().select(call);
                callsTable.scrollTo(call);
                return;
            }
        }
    }

    private Window owner() {
        return callsTable.getScene() == null ? null : callsTable.getScene().getWindow();
    }

    private String dash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private void showDatabaseError(DatabaseException e) {
        log.error("Ошибка базы данных", e);
        Dialogs.error(owner(), "Ошибка базы данных",
                "Не удалось выполнить операцию с базой данных. Проверьте, что PostgreSQL запущен, "
                        + "а параметры в database.properties указаны верно.\n\nПодробности: " + e.getMessage());
    }

    private void showWindowError(IOException e) {
        log.error("Не удалось открыть окно", e);
        Dialogs.error(owner(), "Ошибка интерфейса", "Не удалось открыть окно: " + e.getMessage());
    }
}
