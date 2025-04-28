import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.controlsfx.control.textfield.TextFields;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * EnglishLessonsTrackerFX - приложение для учёта уроков английского языка.
 * Версия: v1.3.0
 * Дата: 28.04.2025
 * Изменения:
 * - Исправлена сортировка таблицы уроков с использованием SortedList.
 * - Исправлена подсветка календаря: жёлтый фон для дат с состоявшимися уроками имеет приоритет.
 * - Добавлено логирование для проверки уроков в календаре.
 * - Сохранена функциональность v1.2.0 (кнопка "Добавить урок" вместо вкладки).
 */
@SuppressWarnings("unchecked")
public class EnglishLessonsTrackerFX extends Application {
    // Перечисление для статуса урока
    public enum LessonStatus {
        PLANNED("Запланирован"),
        COMPLETED("Состоявшийся");

        private final String displayName;

        LessonStatus(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    // Класс для хранения данных об уроке
    public static class Lesson {
        private final LocalDate date;
        private final String studentName;
        private final double hourlyRate;
        private final double hours;
        private final double totalCost;
        private final LessonStatus status;

        public Lesson(LocalDate date, String studentName, double hourlyRate, double hours, LessonStatus status) {
            this.date = date;
            this.studentName = studentName;
            this.hourlyRate = hourlyRate;
            this.hours = hours;
            this.totalCost = hourlyRate * hours;
            this.status = status;
        }

        public LocalDate getDate() {
            return date;
        }

        public String getStudentName() {
            return studentName;
        }

        public double getHourlyRate() {
            return hourlyRate;
        }

        public double getHours() {
            return hours;
        }

        public double getTotalCost() {
            return totalCost;
        }

        public LessonStatus getStatus() {
            return status;
        }

        public String getFormattedDate() {
            return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        }
    }

    // Класс для итогов по ученику
    public static class StudentSummary {
        private final SimpleStringProperty studentName;
        private final SimpleDoubleProperty lessonCount;
        private final SimpleDoubleProperty totalCost;

        public StudentSummary(String studentName, double lessonCount, double totalCost) {
            this.studentName = new SimpleStringProperty(studentName);
            this.lessonCount = new SimpleDoubleProperty(lessonCount);
            this.totalCost = new SimpleDoubleProperty(totalCost);
        }

        public String getStudentName() {
            return studentName.get();
        }

        public double getLessonCount() {
            return lessonCount.get();
        }

        public double getTotalCost() {
            return totalCost.get();
        }
    }

    private final ObservableList<Lesson> lessons = FXCollections.observableArrayList();
    private final ObservableList<String> previousDates = FXCollections.observableArrayList();
    private final ObservableList<String> previousStudentNames = FXCollections.observableArrayList();
    private final ObservableList<String> previousHourlyRates = FXCollections.observableArrayList();
    private final ObservableList<String> previousHours = FXCollections.observableArrayList();
    private final ObservableList<String> previousMonths = FXCollections.observableArrayList();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private final File xmlFile = new File("lessons.xml");

    @Override
    public void start(Stage primaryStage) {
        // Загрузка данных из XML при запуске
        loadLessonsFromXML();

        // Инициализация списков автодополнения
        updateAutocompleteLists();

        primaryStage.setTitle("English Lessons Tracker");

        // Создаем вкладки
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab lessonsTab = new Tab("Список уроков", createLessonsPane());
        Tab summaryTab = new Tab("Итоги по ученикам", createSummaryPane());

        tabPane.getTabs().addAll(lessonsTab, summaryTab);

        // Основная сцена
        Scene scene = new Scene(tabPane, 1000, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Загрузка уроков из XML
    private void loadLessonsFromXML() {
        try {
            if (!xmlFile.exists()) {
                return; // Если файла нет, ничего не загружаем
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            NodeList lessonNodes = doc.getElementsByTagName("lesson");
            for (int i = 0; i < lessonNodes.getLength(); i++) {
                Element lessonElement = (Element) lessonNodes.item(i);
                LocalDate date = LocalDate.parse(lessonElement.getElementsByTagName("date").item(0).getTextContent(), dateFormatter);
                String studentName = lessonElement.getElementsByTagName("studentName").item(0).getTextContent();
                double hourlyRate = Double.parseDouble(lessonElement.getElementsByTagName("hourlyRate").item(0).getTextContent());
                double hours = Double.parseDouble(lessonElement.getElementsByTagName("hours").item(0).getTextContent());
                String statusStr = lessonElement.getElementsByTagName("status").item(0).getTextContent();
                LessonStatus status = LessonStatus.valueOf(statusStr);

                lessons.add(new Lesson(date, studentName, hourlyRate, hours, status));
            }
            System.out.println("Loaded lessons from XML: " + lessons.size());
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ошибка при загрузке данных из XML: " + e.getMessage());
            alert.showAndWait();
        }
    }

    // Сохранение уроков в XML
    private void saveLessonsToXML() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            // Корневой элемент
            Element root = doc.createElement("lessons");
            doc.appendChild(root);

            // Добавляем уроки
            for (Lesson lesson : lessons) {
                Element lessonElement = doc.createElement("lesson");

                Element dateElement = doc.createElement("date");
                dateElement.appendChild(doc.createTextNode(lesson.getFormattedDate()));
                lessonElement.appendChild(dateElement);

                Element studentElement = doc.createElement("studentName");
                studentElement.appendChild(doc.createTextNode(lesson.getStudentName()));
                lessonElement.appendChild(studentElement);

                Element hourlyRateElement = doc.createElement("hourlyRate");
                hourlyRateElement.appendChild(doc.createTextNode(String.valueOf(lesson.getHourlyRate())));
                lessonElement.appendChild(hourlyRateElement);

                Element hoursElement = doc.createElement("hours");
                hoursElement.appendChild(doc.createTextNode(String.valueOf(lesson.getHours())));
                lessonElement.appendChild(hoursElement);

                Element statusElement = doc.createElement("status");
                statusElement.appendChild(doc.createTextNode(lesson.getStatus().name()));
                lessonElement.appendChild(statusElement);

                root.appendChild(lessonElement);
            }

            // Сохраняем документ в файл
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(xmlFile);
            transformer.transform(source, result);
        } catch (Exception e) {
            System.out.println("saveLessonsToXML error: " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ошибка при сохранении данных в XML: " + e.getMessage());
            alert.showAndWait();
        }
    }

    // Обновление списков автодополнения
    private void updateAutocompleteLists() {
        previousDates.setAll(
                lessons.stream()
                        .map(Lesson::getFormattedDate)
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList())
        );
        previousStudentNames.setAll(
                lessons.stream()
                        .map(Lesson::getStudentName)
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList())
        );
        previousHourlyRates.setAll(
                lessons.stream()
                        .map(lesson -> String.valueOf(lesson.getHourlyRate()))
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList())
        );
        previousHours.setAll(
                lessons.stream()
                        .map(lesson -> String.valueOf(lesson.getHours()))
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList())
        );
    }

    // Вкладка для списка уроков с календарем и кнопкой "Добавить урок"
    private Pane createLessonsPane() {
        HBox mainPane = new HBox(10);
        mainPane.setPadding(new Insets(10));

        // Объявляем filteredLessons и selectedDate в начале метода
        FilteredList<Lesson> filteredLessons = new FilteredList<>(lessons, p -> true);
        LocalDate[] selectedDate = {null}; // Для хранения выбранной даты

        // Логирование для диагностики
        System.out.println("createLessonsPane: filteredLessons initialized: " + (filteredLessons != null));

        // Календарь и кнопка
        VBox calendarPane = new VBox(10);
        calendarPane.setMinWidth(300);
        calendarPane.setPrefWidth(300);
        TextField monthField = new TextField(YearMonth.now().format(DateTimeFormatter.ofPattern("MM.yyyy")));
        monthField.setPromptText("Месяц и год (мм.гггг, например 04.2025)");
        TextFields.bindAutoCompletion(monthField, previousMonths);

        // Сохранение введенных месяцев
        monthField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.isEmpty() && newValue.matches("\\d{2}\\.\\d{4}") && !previousMonths.contains(newValue)) {
                previousMonths.add(newValue);
                previousMonths.sort(String::compareTo);
            }
        });

        Button showCalendarButton = new Button("Показать календарь");
        Button showAllLessonsButton = new Button("Все уроки");
        GridPane calendarGrid = new GridPane();
        calendarGrid.setHgap(10);
        calendarGrid.setVgap(10);

        // Фильтр по ученику
        ComboBox<String> studentFilter = new ComboBox<>();
        studentFilter.getItems().add("Все");
        studentFilter.getItems().addAll(
                lessons.stream()
                        .map(Lesson::getStudentName)
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList())
        );
        studentFilter.setValue("Все");

        // Фильтр по статусу
        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("Все", "Запланированные", "Состоявшиеся");
        statusFilter.setValue("Все");

        // Кнопка "Добавить урок"
        Button addLessonButton = new Button("Добавить урок");

        // Обновление списка учеников при изменении lessons
        lessons.addListener((ListChangeListener<Lesson>) change -> {
            Platform.runLater(() -> {
                System.out.println("lessons.addListener: Updating studentFilter and filters");
                String currentSelection = studentFilter.getValue();
                studentFilter.getItems().clear();
                studentFilter.getItems().add("Все");
                studentFilter.getItems().addAll(
                        lessons.stream()
                                .map(Lesson::getStudentName)
                                .distinct()
                                .sorted()
                                .collect(Collectors.toList())
                );
                if (currentSelection != null && studentFilter.getItems().contains(currentSelection)) {
                    studentFilter.setValue(currentSelection);
                } else {
                    studentFilter.setValue("Все");
                }
                updateAutocompleteLists(); // Обновляем списки автодополнения
                System.out.println("lessons.addListener: Calling updateFilter with filteredLessons: " + (filteredLessons != null));
                updateFilter(filteredLessons, selectedDate[0], statusFilter.getValue(), studentFilter.getValue());
            });
        });

        // Добавляем элементы в calendarPane
        calendarPane.getChildren().addAll(
                new Label("Введите месяц для календаря:"), monthField,
                showCalendarButton, showAllLessonsButton, calendarGrid
        );

        // Таблица уроков
        TableView<Lesson> table = new TableView<>();
        // Используем SortedList для сортировки
        SortedList<Lesson> sortedLessons = new SortedList<>(filteredLessons);
        sortedLessons.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedLessons);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setSortPolicy(param -> true); // Включаем сортировку

        TableColumn<Lesson, String> dateColumn = new TableColumn<>("Дата");
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("formattedDate"));
        dateColumn.setSortable(true);

        TableColumn<Lesson, String> studentColumn = new TableColumn<>("Имя ученика");
        studentColumn.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        studentColumn.setSortable(true);

        TableColumn<Lesson, Double> hourlyRateColumn = new TableColumn<>("Цена/час (руб)");
        hourlyRateColumn.setCellValueFactory(new PropertyValueFactory<>("hourlyRate"));
        hourlyRateColumn.setSortable(true);

        TableColumn<Lesson, Double> hoursColumn = new TableColumn<>("Часы");
        hoursColumn.setCellValueFactory(new PropertyValueFactory<>("hours"));
        hoursColumn.setSortable(true);

        TableColumn<Lesson, Double> totalCostColumn = new TableColumn<>("Стоимость (руб)");
        totalCostColumn.setCellValueFactory(new PropertyValueFactory<>("totalCost"));
        totalCostColumn.setSortable(true);

        TableColumn<Lesson, LessonStatus> statusColumn = new TableColumn<>("Статус");
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setSortable(true);

        // Колонка для кнопки редактирования
        TableColumn<Lesson, Void> editColumn = new TableColumn<>("Редактировать");
        editColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("Редактировать");

            {
                editButton.setOnAction(event -> {
                    Lesson lesson = getTableView().getItems().get(getIndex());
                    editLesson(lesson, lessons.indexOf(lesson));
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(editButton);
                }
            }
        });
        editColumn.setSortable(false);

        // Колонка для кнопки удаления
        TableColumn<Lesson, Void> deleteColumn = new TableColumn<>("Удалить");
        deleteColumn.setCellFactory(param -> new TableCell<>() {
            private final Button deleteButton = new Button("Удалить");

            {
                deleteButton.setOnAction(event -> {
                    Lesson lesson = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Подтверждение удаления");
                    confirm.setHeaderText("Удалить урок?");
                    confirm.setContentText("Урок для " + lesson.getStudentName() + " на " + lesson.getFormattedDate() + " будет удален.");
                    Optional<ButtonType> result = confirm.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        lessons.remove(lesson);
                        saveLessonsToXML();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(deleteButton);
                }
            }
        });
        deleteColumn.setSortable(false);

        table.getColumns().addAll(dateColumn, studentColumn, hourlyRateColumn, hoursColumn, totalCostColumn, statusColumn, editColumn, deleteColumn);

        // Логика фильтрации
        statusFilter.setOnAction(e -> updateFilter(filteredLessons, selectedDate[0], statusFilter.getValue(), studentFilter.getValue()));
        studentFilter.setOnAction(e -> updateFilter(filteredLessons, selectedDate[0], statusFilter.getValue(), studentFilter.getValue()));

        // Логика добавления урока через диалоговое окно
        addLessonButton.setOnAction(e -> {
            Dialog<Lesson> dialog = new Dialog<>();
            dialog.setTitle("Добавить урок");
            dialog.setHeaderText("Введите данные урока");

            // Кнопки
            ButtonType addButtonType = new ButtonType("Добавить", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

            // Поля ввода с автодополнением
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            TextField dateField = new TextField();
            dateField.setPromptText("Дата (дд.мм.гггг)");
            TextFields.bindAutoCompletion(dateField, previousDates);

            TextField studentField = new TextField();
            studentField.setPromptText("Имя ученика");
            TextFields.bindAutoCompletion(studentField, previousStudentNames);

            TextField hourlyRateField = new TextField();
            hourlyRateField.setPromptText("Цена за 1 час (руб)");
            TextFields.bindAutoCompletion(hourlyRateField, previousHourlyRates);

            TextField hoursField = new TextField();
            hoursField.setPromptText("Количество часов");
            TextFields.bindAutoCompletion(hoursField, previousHours);

            ComboBox<LessonStatus> statusComboBox = new ComboBox<>();
            statusComboBox.getItems().addAll(LessonStatus.PLANNED, LessonStatus.COMPLETED);
            statusComboBox.setValue(LessonStatus.PLANNED);

            grid.add(new Label("Дата урока:"), 0, 0);
            grid.add(dateField, 1, 0);
            grid.add(new Label("Имя ученика:"), 0, 1);
            grid.add(studentField, 1, 1);
            grid.add(new Label("Цена за 1 час:"), 0, 2);
            grid.add(hourlyRateField, 1, 2);
            grid.add(new Label("Количество часов:"), 0, 3);
            grid.add(hoursField, 1, 3);
            grid.add(new Label("Статус урока:"), 0, 4);
            grid.add(statusComboBox, 1, 4);

            dialog.getDialogPane().setContent(grid);

            // Преобразование результата диалога в объект Lesson
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == addButtonType) {
                    try {
                        LocalDate date = LocalDate.parse(dateField.getText(), dateFormatter);
                        String studentName = studentField.getText();
                        double hourlyRate = Double.parseDouble(hourlyRateField.getText());
                        double hours = Double.parseDouble(hoursField.getText());
                        LessonStatus status = statusComboBox.getValue();

                        if (studentName.isEmpty()) {
                            throw new IllegalArgumentException("Имя ученика не может быть пустым");
                        }
                        if (status == null) {
                            throw new IllegalArgumentException("Статус урока должен быть выбран");
                        }

                        return new Lesson(date, studentName, hourlyRate, hours, status);
                    } catch (Exception ex) {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Ошибка: Проверьте введенные данные. " + ex.getMessage());
                        alert.showAndWait();
                        return null;
                    }
                }
                return null;
            });

            // Обработка результата
            Optional<Lesson> result = dialog.showAndWait();
            result.ifPresent(newLesson -> {
                lessons.add(newLesson);
                System.out.println("Добавлен урок: " + newLesson.getFormattedDate() + ", " + newLesson.getStudentName());
                saveLessonsToXML();
                updateAutocompleteLists();
            });
        });

        // Логика календаря
        Runnable showCalendar = () -> {
            try {
                YearMonth yearMonth = YearMonth.parse(monthField.getText(), DateTimeFormatter.ofPattern("MM.yyyy"));
                calendarGrid.getChildren().clear();

                // Заголовки дней недели
                String[] days = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
                for (int i = 0; i < 7; i++) {
                    calendarGrid.add(new Label(days[i]), i, 0);
                }

                LocalDate startDate = yearMonth.atDay(1);
                int firstDayOfWeek = startDate.getDayOfWeek().getValue(); // 1 = Пн, 7 = Вс
                int daysInMonth = yearMonth.lengthOfMonth();
                LocalDate today = LocalDate.now();

                int row = 1;
                int col = firstDayOfWeek - 1;
                for (int day = 1; day <= daysInMonth; day++) {
                    LocalDate date = yearMonth.atDay(day);
                    Label dayLabel = new Label(String.valueOf(day));
                    dayLabel.setPadding(new Insets(5));
                    dayLabel.setStyle("-fx-border-color: black; -fx-border-width: 1;");

                    // Проверяем наличие уроков
                    List<Lesson> lessonsOnDate = lessons.stream()
                            .filter(lesson -> lesson.getDate().equals(date))
                            .toList();
                    boolean hasPlanned = lessonsOnDate.stream().anyMatch(lesson -> lesson.getStatus() == LessonStatus.PLANNED);
                    boolean hasCompleted = lessonsOnDate.stream().anyMatch(lesson -> lesson.getStatus() == LessonStatus.COMPLETED);

                    // Логирование для диагностики
                    if (day == 20 || day == 28) {
                        System.out.println("Calendar date: " + date + ", hasCompleted: " + hasCompleted + ", hasPlanned: " + hasPlanned);
                        lessonsOnDate.forEach(lesson -> System.out.println("Lesson: " + lesson.getFormattedDate() + ", Status: " + lesson.getStatus()));
                    }

                    // Устанавливаем стиль с приоритетом для COMPLETED
                    String baseStyle = "-fx-border-color: black; -fx-border-width: 1;";
                    if (hasCompleted) {
                        dayLabel.setStyle("-fx-background-color: yellow; " + baseStyle);
                    } else if (hasPlanned) {
                        dayLabel.setStyle("-fx-background-color: lightblue; " + baseStyle);
                    } else if (date.equals(today)) {
                        dayLabel.setStyle("-fx-background-color: lightgray; -fx-font-weight: bold; " + baseStyle);
                    } else {
                        dayLabel.setStyle(baseStyle);
                    }

                    // Hover-эффект
                    String hoverStyle = hasCompleted ? "-fx-background-color: gold;" :
                            hasPlanned ? "-fx-background-color: skyblue;" : "-fx-background-color: #f0f0f0;";
                    dayLabel.setOnMouseEntered(event -> {
                        String currentStyle = dayLabel.getStyle();
                        dayLabel.setStyle(hoverStyle + " " + currentStyle);
                    });
                    dayLabel.setOnMouseExited(event -> {
                        String currentStyle = dayLabel.getStyle().replace(hoverStyle, "");
                        dayLabel.setStyle(currentStyle);
                    });

                    // Обработка клика по дате
                    dayLabel.setOnMouseClicked(event -> {
                        selectedDate[0] = date;
                        updateFilter(filteredLessons, selectedDate[0], statusFilter.getValue(), studentFilter.getValue());
                    });

                    calendarGrid.add(dayLabel, col, row);
                    col++;
                    if (col == 7) {
                        col = 0;
                        row++;
                    }
                }
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Ошибка: Проверьте формат даты (мм.гггг).");
                alert.showAndWait();
            }
        };

        // Автоматический показ календаря при загрузке
        if (!monthField.getText().isEmpty()) {
            showCalendar.run();
        }

        // Обновление календаря по кнопке
        showCalendarButton.setOnAction(e -> showCalendar.run());

        // Кнопка "Все уроки"
        showAllLessonsButton.setOnAction(e -> {
            selectedDate[0] = null;
            updateFilter(filteredLessons, null, statusFilter.getValue(), studentFilter.getValue());
        });

        VBox tablePane = new VBox(10);
        tablePane.getChildren().addAll(
                new Label("Фильтр по статусу:"), statusFilter,
                new Label("Фильтр по ученику:"), studentFilter,
                addLessonButton,
                table
        );

        // Настройка HBox для динамического распределения пространства
        HBox.setHgrow(tablePane, Priority.ALWAYS);
        mainPane.getChildren().addAll(calendarPane, tablePane);
        return mainPane;
    }

    // Метод для обновления фильтра
    private void updateFilter(FilteredList<Lesson> filteredLessons, LocalDate selectedDate, String statusFilter, String studentFilter) {
        System.out.println("updateFilter: selectedDate=" + selectedDate + ", statusFilter=" + statusFilter + ", studentFilter=" + studentFilter);
        filteredLessons.setPredicate(lesson -> {
            boolean dateMatch = selectedDate == null || lesson.getDate().equals(selectedDate);
            boolean statusMatch = statusFilter == null || statusFilter.equals("Все") ||
                    (statusFilter.equals("Запланированные") && lesson.getStatus() == LessonStatus.PLANNED) ||
                    (statusFilter.equals("Состоявшиеся") && lesson.getStatus() == LessonStatus.COMPLETED);
            boolean studentMatch = studentFilter == null || studentFilter.equals("Все") ||
                    lesson.getStudentName().equals(studentFilter);
            System.out.println("Lesson: " + lesson.getFormattedDate() + ", " + lesson.getStudentName() +
                    " -> dateMatch=" + dateMatch + ", statusMatch=" + statusMatch + ", studentMatch=" + studentMatch);
            return dateMatch && statusMatch && studentMatch;
        });
    }

    // Метод для редактирования урока
    private void editLesson(Lesson lesson, int index) {
        Dialog<Lesson> dialog = new Dialog<>();
        dialog.setTitle("Редактировать урок");
        dialog.setHeaderText("Измените данные урока");

        // Кнопки
        ButtonType saveButtonType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Поля ввода с автодополнением
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField dateField = new TextField(lesson.getFormattedDate());
        dateField.setPromptText("Дата (дд.мм.гггг)");
        TextFields.bindAutoCompletion(dateField, previousDates);

        TextField studentField = new TextField(lesson.getStudentName());
        studentField.setPromptText("Имя ученика");
        TextFields.bindAutoCompletion(studentField, previousStudentNames);

        TextField HourlyRateField = new TextField(String.valueOf(lesson.getHourlyRate()));
        HourlyRateField.setPromptText("Цена за 1 час (руб)");
        TextFields.bindAutoCompletion(HourlyRateField, previousHourlyRates);

        TextField hoursField = new TextField(String.valueOf(lesson.getHours()));
        hoursField.setPromptText("Количество часов");
        TextFields.bindAutoCompletion(hoursField, previousHours);

        ComboBox<LessonStatus> statusComboBox = new ComboBox<>();
        statusComboBox.getItems().addAll(LessonStatus.PLANNED, LessonStatus.COMPLETED);
        statusComboBox.setValue(lesson.getStatus());

        grid.add(new Label("Дата урока:"), 0, 0);
        grid.add(dateField, 1, 0);
        grid.add(new Label("Имя ученика:"), 0, 1);
        grid.add(studentField, 1, 1);
        grid.add(new Label("Цена за 1 час:"), 0, 2);
        grid.add(HourlyRateField, 1, 2);
        grid.add(new Label("Количество часов:"), 0, 3);
        grid.add(hoursField, 1, 3);
        grid.add(new Label("Статус урока:"), 0, 4);
        grid.add(statusComboBox, 1, 4);

        dialog.getDialogPane().setContent(grid);

        // Преобразование результата диалога в объект Lesson
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    LocalDate date = LocalDate.parse(dateField.getText(), dateFormatter);
                    String studentName = studentField.getText();
                    double hourlyRate = Double.parseDouble(HourlyRateField.getText());
                    double hours = Double.parseDouble(hoursField.getText());
                    LessonStatus status = statusComboBox.getValue();

                    if (studentName.isEmpty()) {
                        throw new IllegalArgumentException("Имя ученика не может быть пустым");
                    }
                    if (status == null) {
                        throw new IllegalArgumentException("Статус урока должен быть выбран");
                    }

                    return new Lesson(date, studentName, hourlyRate, hours, status);
                } catch (Exception e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Ошибка: Проверьте введенные данные. " + e.getMessage());
                    alert.showAndWait();
                    return null;
                }
            }
            return null;
        });

        // Обработка результата
        Optional<Lesson> result = dialog.showAndWait();
        result.ifPresent(newLesson -> {
            lessons.set(index, newLesson);
            saveLessonsToXML();
            updateAutocompleteLists(); // Обновляем списки автодополнения
        });
    }

    // Вкладка для итогов по ученикам
    private Pane createSummaryPane() {
        VBox pane = new VBox(10);
        pane.setPadding(new Insets(10));

        TextField monthField = new TextField(YearMonth.now().format(DateTimeFormatter.ofPattern("MM.yyyy")));
        monthField.setPromptText("Месяц и год (мм.гггг, например 04.2025)");
        TextFields.bindAutoCompletion(monthField, previousMonths);

        // Сохранение введенных месяцев
        monthField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.isEmpty() && newValue.matches("\\d{2}\\.\\d{4}") && !previousMonths.contains(newValue)) {
                previousMonths.add(newValue);
                previousMonths.sort(String::compareTo);
            }
        });

        ComboBox<String> studentFilter = new ComboBox<>();
        studentFilter.getItems().add("Все");
        studentFilter.getItems().addAll(
                lessons.stream()
                        .map(Lesson::getStudentName)
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList())
        );
        studentFilter.setValue("Все");

        // Обновление списка учеников при изменении lessons
        lessons.addListener((ListChangeListener<Lesson>) change -> {
            Platform.runLater(() -> {
                String currentSelection = studentFilter.getValue();
                studentFilter.getItems().clear();
                studentFilter.getItems().add("Все");
                studentFilter.getItems().addAll(
                        lessons.stream()
                                .map(Lesson::getStudentName)
                                .distinct()
                                .sorted()
                                .collect(Collectors.toList())
                );
                if (currentSelection != null && studentFilter.getItems().contains(currentSelection)) {
                    studentFilter.setValue(currentSelection);
                } else {
                    studentFilter.setValue("Все");
                }
            });
        });

        Button showSummaryButton = new Button("Показать итоги");

        TableView<StudentSummary> table = new TableView<>();
        table.setSortPolicy(param -> {
            // Сортировка, но "ИТОГО" остается внизу
            ObservableList<StudentSummary> items = table.getItems();
            if (items.isEmpty()) return true;
            StudentSummary totalRow = items.stream()
                    .filter(s -> "ИТОГО".equals(s.getStudentName()))
                    .findFirst()
                    .orElse(null);
            if (totalRow != null) {
                items.remove(totalRow);
                table.sort();
                items.add(totalRow);
            } else {
                table.sort();
            }
            return true;
        });

        // Стилизация строки "ИТОГО"
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(StudentSummary item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else if ("ИТОГО".equals(item.getStudentName())) {
                    setStyle("-fx-font-weight: bold; -fx-background-color: lightgray;");
                } else {
                    setStyle("");
                }
            }
        });

        TableColumn<StudentSummary, String> studentColumn = new TableColumn<>("Имя ученика");
        studentColumn.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        studentColumn.setSortable(true);

        TableColumn<StudentSummary, Double> hoursColumn = new TableColumn<>("Часы");
        hoursColumn.setCellValueFactory(new PropertyValueFactory<>("lessonCount"));
        hoursColumn.setSortable(true);

        TableColumn<StudentSummary, Double> totalCostColumn = new TableColumn<>("Общая стоимость (руб)");
        totalCostColumn.setCellValueFactory(new PropertyValueFactory<>("totalCost"));
        totalCostColumn.setSortable(true);

        table.getColumns().addAll(studentColumn, hoursColumn, totalCostColumn);

        // Логика отображения итогов
        Runnable showSummary = () -> {
            try {
                YearMonth yearMonth = YearMonth.parse(monthField.getText(), DateTimeFormatter.ofPattern("MM.yyyy"));
                LocalDate startDate = yearMonth.atDay(1);
                LocalDate endDate = yearMonth.atEndOfMonth();
                String selectedStudent = studentFilter.getValue();

                ObservableList<StudentSummary> summaries = FXCollections.observableArrayList();
                var filteredLessons = lessons.stream()
                        .filter(lesson -> !lesson.getDate().isBefore(startDate) && !lesson.getDate().isAfter(endDate))
                        .filter(lesson -> lesson.getStatus() == LessonStatus.COMPLETED);

                // Применяем фильтр по ученику
                if (selectedStudent != null && !"Все".equals(selectedStudent)) {
                    filteredLessons = filteredLessons.filter(lesson -> lesson.getStudentName().equals(selectedStudent));
                }

                var studentGroups = filteredLessons.collect(Collectors.groupingBy(Lesson::getStudentName));

                // Добавляем итоги по каждому ученику
                studentGroups.forEach((student, lessonList) -> {
                    double totalHours = lessonList.stream().mapToDouble(Lesson::getHours).sum();
                    double totalCost = lessonList.stream().mapToDouble(Lesson::getTotalCost).sum();
                    summaries.add(new StudentSummary(student, totalHours, totalCost));
                });

                // Добавляем строку "ИТОГО"
                double grandTotalHours = studentGroups.values().stream()
                        .flatMap(List::stream)
                        .mapToDouble(Lesson::getHours)
                        .sum();
                double grandTotalCost = studentGroups.values().stream()
                        .flatMap(List::stream)
                        .mapToDouble(Lesson::getTotalCost)
                        .sum();
                if (!summaries.isEmpty()) {
                    summaries.add(new StudentSummary("ИТОГО", grandTotalHours, grandTotalCost));
                }

                table.setItems(summaries);
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Ошибка: Проверьте формат даты (мм.гггг).");
                alert.showAndWait();
            }
        };

        // Автоматический показ итогов при загрузке
        showSummary.run();

        // Обновление итогов по кнопке
        showSummaryButton.setOnAction(e -> showSummary.run());

        // Обновление итогов при изменении фильтра по ученику
        studentFilter.setOnAction(e -> showSummary.run());

        pane.getChildren().addAll(
                new Label("Введите месяц для итогов:"), monthField,
                new Label("Фильтр по ученику:"), studentFilter,
                showSummaryButton, table
        );
        return pane;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
