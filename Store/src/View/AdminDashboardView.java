package View;

import Controller.AdminController;
import Model.Role;
import Model.User;
import Util.AppNavigator;
import javafx.geometry.Insets;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.Optional;

public class AdminDashboardView {
    private AdminController controller;
    private UserView userView;
    private VBox contentArea;
    private HBox quickActions;
    private AppNavigator navigator;
    private TextField usernameField;
    private PasswordField passwordField;
    private TextField nameField;
    private TextField emailField;
    private TextField phoneField;
    private ComboBox<Role> roleComboBox;
    private TextField accessField;

    public void show(Stage primaryStage) {
        controller = new AdminController();
        userView = new UserView();
        userView.setUsers(controller.getEmployees());

        final BorderPane mainLayout = new BorderPane();

    //set style for admin dashboard display
        HBox header = new HBox();
        Label welcomeLabel = new Label("Welcome, Admin");
        welcomeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));

        HBox heading = AppTheme.header("The shop desk", "Staff, accounts & permissions");
        HBox.setHgrow(heading,Priority.ALWAYS);
        header.getChildren().add(heading);
        header.setPadding(new Insets(24));

        VBox sidebar = new VBox(20);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(24, 16, 24, 16));
        sidebar.setPrefWidth(210);
        sidebar.getChildren().add(new Label("storepos.  /  staff"));
        //create buttons on the left side
        Button overviewButton = createSidebarButton("Overview");
        overviewButton.setId("admin-overview");
        Button manageEmployeesButton = createSidebarButton("Staff");
        manageEmployeesButton.setId("admin-staff");
        Button viewReportsButton = createSidebarButton("Reports");
        Button accessLevelButton = createSidebarButton("Permissions");
        Button logoutButton = createSidebarButton("Logout");

        overviewButton.getStyleClass().add("selected");
        java.util.List<Button> navigation = java.util.List.of(overviewButton, manageEmployeesButton, viewReportsButton, accessLevelButton);
        for (Button item : navigation) item.addEventHandler(javafx.event.ActionEvent.ACTION, event -> {
            navigation.forEach(button -> button.getStyleClass().remove("selected"));
            item.getStyleClass().add("selected");
        });
        overviewButton.setOnAction(e -> showOverview(manageEmployeesButton, viewReportsButton));
        manageEmployeesButton.setOnAction(e -> showEmployeeManagement());
        viewReportsButton.setOnAction(e -> showReports());
        accessLevelButton.setOnAction(e -> showAccessLevels());
        logoutButton.setOnAction(e -> {
            if (navigator != null) {
                navigator.logout();
            } else {
                primaryStage.close();
                new LoginView().start(new Stage());
            }
        });

        sidebar.getChildren().addAll(overviewButton, manageEmployeesButton, viewReportsButton, accessLevelButton, logoutButton);

        contentArea = new VBox(20);
        contentArea.setPadding(new Insets(20));

        quickActions = new HBox(20);
        quickActions.getStyleClass().add("toolbar");
        quickActions.setPadding(new Insets(16, 24, 16, 24));
        //create action buttons on the bottom
        Button addEmployeeButton = createActionButton("Add Employee");
        Button deleteEmployeeButton = createActionButton("Delete Employee");
        Button updateDetailsButton = createActionButton("Update Details");
        Button clearButton = createActionButton("Clear Form");

        addEmployeeButton.setOnAction(e -> addEmployee());
        deleteEmployeeButton.setOnAction(e -> deleteSelectedEmployee());
        updateDetailsButton.setOnAction(e -> updateSelectedEmployee());
        clearButton.setOnAction(e -> clearForm());

        AppTheme.primary(addEmployeeButton);
        AppTheme.danger(deleteEmployeeButton);
        quickActions.getChildren().addAll(addEmployeeButton, deleteEmployeeButton, updateDetailsButton, clearButton);

        showOverview(manageEmployeesButton, viewReportsButton);

        mainLayout.setTop(header);
        mainLayout.setLeft(sidebar);
        mainLayout.setCenter(contentArea);
        mainLayout.setBottom(quickActions);

        Scene scene = new Scene(mainLayout, 1200, 800);
        AppTheme.apply(scene, primaryStage);
        primaryStage.setMinWidth(1040);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Admin Dashboard");
        primaryStage.show();
    }

    private Button createSidebarButton(String text) {
        Button button = new Button(text);
        button.setPrefWidth(178);

        return button;
    }

    private void showOverview(Button staff, Button reports) {
        setStaffActions(false);
        Button team = AppTheme.primary(new Button("Manage staff"));
        team.setOnAction(e -> staff.fire());
        Button sales = new Button("Sales & inventory reports");
        sales.setOnAction(e -> reports.fire());
        Util.SharedContext context = Util.SharedContext.getInstance();
        ScrollPane scroll = new ScrollPane(StoreOverview.create(context.getBills(), context.getInventory().getItems(), team, sales));
        scroll.setFitToWidth(true);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        contentArea.getChildren().setAll(scroll);
    }

    private void setStaffActions(boolean visible) {
        quickActions.setVisible(visible); quickActions.setManaged(visible);
    }

    private void showEmployeeManagement() {
        setStaffActions(true);
        contentArea.getChildren().clear();

        Label contentTitle = new Label("The team");
        contentTitle.getStyleClass().add("section-title");

        GridPane form = createEmployeeForm();
        userView.setUsers(controller.getEmployees());
        userView.getTableView().setOnMouseClicked(e -> populateForm(userView.getSelectedUser()));

        AppTheme.table(userView.getTableView(), "No employees to display.");
        VBox.setVgrow(userView.getTableView(), Priority.ALWAYS);
        contentArea.getChildren().addAll(contentTitle, AppTheme.card(form), userView.getTableView());
    }

    private GridPane createEmployeeForm() {
        usernameField = createTextField("Username");
        passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        nameField = createTextField("Name");
        emailField = createTextField("Email");
        phoneField = createTextField("Phone Number");
        roleComboBox = new ComboBox<>();
        roleComboBox.getItems().addAll(Role.ADMIN, Role.MANAGER, Role.CASHIER);
        roleComboBox.setPromptText("Role");
        roleComboBox.setMaxWidth(Double.MAX_VALUE);
        accessField = createTextField("Access Level");

        GridPane form = new GridPane();
        form.setHgap(16);
        for (int i = 0; i < 4; i++) {
            ColumnConstraints column = new ColumnConstraints();
            if (i % 2 == 1) { column.setHgrow(Priority.ALWAYS); column.setPercentWidth(34); }
            form.getColumnConstraints().add(column);
        }
        form.setVgap(10);
        form.addRow(0, new Label("Username:"), usernameField, new Label("Password:"), passwordField);
        form.addRow(1, new Label("Name:"), nameField, new Label("Email:"), emailField);
        form.addRow(2, new Label("Phone:"), phoneField, new Label("Role:"), roleComboBox);
        form.addRow(3, new Label("Access:"), accessField);
        return form;
    }

    private TextField createTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        return field;
    }

    private void addEmployee() {
        try {
            controller.addEmployee(usernameField.getText(), passwordField.getText(), nameField.getText(),
                    emailField.getText(), phoneField.getText(), roleComboBox.getValue(), accessField.getText());
            refreshEmployeeTable();
            clearForm();
            showAlert("Success", "Employee added successfully.");
        } catch (RuntimeException e) {
            showAlert("Error", e.getMessage());
        }
    }

    private void updateSelectedEmployee() {
        User selectedUser = userView.getSelectedUser();
        if (selectedUser == null) {
            showAlert("Error", "Please select an employee to update.");
            return;
        }

        try {
            controller.updateEmployee(selectedUser.getUsername(), usernameField.getText(), passwordField.getText(),
                    nameField.getText(), emailField.getText(), phoneField.getText(), roleComboBox.getValue(),
                    accessField.getText());
            refreshEmployeeTable();
            showAlert("Success", "Employee updated successfully.");
        } catch (RuntimeException e) {
            showAlert("Error", e.getMessage());
        }
    }

    private void deleteSelectedEmployee() {
        User selectedUser = userView.getSelectedUser();
        if (selectedUser == null) {
            showAlert("Error", "Please select an employee to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Employee");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete " + selectedUser.getUsername() + "?");
        AppTheme.dialog(confirm);
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            controller.deleteEmployee(selectedUser);
            refreshEmployeeTable();
            clearForm();
            showAlert("Success", "Employee deleted successfully.");
        }
    }

    private void populateForm(User user) {
        if (user == null) {
            return;
        }

        usernameField.setText(user.getUsername());
        passwordField.clear();
        passwordField.setPromptText("Leave blank to keep password");
        nameField.setText(user.getName());
        emailField.setText(user.getEmail());
        phoneField.setText(user.getPhoneNumber());
        roleComboBox.setValue(user.getRole());
        accessField.setText(user.getAccessLevel());
    }

    private void refreshEmployeeTable() {
        userView.setUsers(controller.getEmployees());
    }

    private void clearForm() {
        usernameField.clear();
        passwordField.clear();
        nameField.clear();
        emailField.clear();
        phoneField.clear();
        roleComboBox.setValue(null);
        accessField.clear();
        userView.getTableView().getSelectionModel().clearSelection();
    }

    private void showReports() {
        setStaffActions(false);
        contentArea.getChildren().clear();

        Label title = new Label("Admin Reports");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        Label total = new Label("Total Employees: " + controller.getEmployees().size());
        Label admins = new Label("Admins: " + controller.getEmployeeCountByRole(Role.ADMIN));
        Label managers = new Label("Managers: " + controller.getEmployeeCountByRole(Role.MANAGER));
        Label cashiers = new Label("Cashiers: " + controller.getEmployeeCountByRole(Role.CASHIER));

        title.getStyleClass().add("page-title");
        Label[] values = {total, admins, managers, cashiers};
        javafx.scene.layout.TilePane cards = new javafx.scene.layout.TilePane(16, 16);
        cards.setPrefColumns(2);
        for (Label value : values) {
            value.getStyleClass().add("section-title");
            VBox card = AppTheme.card(value); card.setPrefWidth(300); cards.getChildren().add(card);
        }
        Button salesReports=AppTheme.primary(new Button("Open sales & inventory reports"));
        salesReports.setOnAction(e -> {
            javafx.stage.Stage stage=(javafx.stage.Stage)contentArea.getScene().getWindow();
            Model.Manager manager=new Model.Manager("reporting","","Reporting","","");
            new StatisticsView(stage,new Controller.ManagerController(manager),() -> show(stage)).show();
        });
        contentArea.getChildren().addAll(title, AppTheme.muted("Current staff accounts"), cards,salesReports);
    }

    public void setNavigator(AppNavigator navigator) {
        this.navigator = navigator;
    }

    private void showAccessLevels() {
        setStaffActions(false);
        contentArea.getChildren().clear();

        Label title = new Label("Access Levels");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        Label admin = new Label("Admin: manage employees and access levels.");
        Label manager = new Label("Manager: manage inventory, suppliers, stock, and statistics.");
        Label cashier = new Label("Cashier: create bills, finalize sales, and view daily sales.");

        title.getStyleClass().add("page-title");
        admin.setWrapText(true); manager.setWrapText(true); cashier.setWrapText(true);
        contentArea.getChildren().addAll(title, AppTheme.muted("Responsibilities for each store role."),
                AppTheme.card(admin), AppTheme.card(manager), AppTheme.card(cashier));
    }

    private Button createActionButton(String text) {
        Button button = new Button(text);
        button.setMinWidth(120);

        return button;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(title.equals("Error") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        AppTheme.dialog(alert);
        alert.showAndWait();
    }

}
