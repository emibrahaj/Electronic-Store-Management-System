package View;
import Controller.AdminController;
import Controller.LoginController;
import Util.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class LoginView {
    private TextField usernameField;
    private PasswordField passwordField;
    private Button loginButton;
    private Label statusLabel;
    private LoginController controller;
    private AppNavigator navigator;
    public void start(Stage stage) {
        boolean setup = AuthService.needsSetup();
        usernameField = new TextField(); usernameField.setPromptText("Username");
        passwordField = new PasswordField(); passwordField.setPromptText("Password");
        statusLabel = AppTheme.muted(setup ? "Create the administrator account for this store." : "Enter your details to open the shop.");
        loginButton = AppTheme.primary(new Button(setup ? "Create administrator" : "Sign in"));
        loginButton.setMaxWidth(Double.MAX_VALUE); loginButton.setDefaultButton(true);
        Label brand = new Label("storepos."); brand.getStyleClass().add("login-brand");
        VBox card = AppTheme.card(AppTheme.title(setup ? "Set up your store" : "Hello again."), statusLabel,
                AppTheme.field("Username", usernameField), AppTheme.field("Password", passwordField));
        if (setup) {
            TextField name = new TextField(); TextField email = new TextField(); TextField phone = new TextField();
            PasswordField confirm = new PasswordField();
            card.getChildren().addAll(AppTheme.field("Confirm password", confirm), AppTheme.field("Full name", name),
                    AppTheme.field("Email", email), AppTheme.field("Phone (10?15 digits)", phone));
            loginButton.setOnAction(e -> {
                try {
                    if (!passwordField.getText().equals(confirm.getText())) throw new IllegalArgumentException("Passwords do not match.");
                    AdminController.createFirstAdmin(getUsername().trim(),getPassword(),name.getText().trim(),email.getText().trim(),phone.getText().trim());
                    start(stage);
                } catch (RuntimeException failure) { setStatus(failure.getMessage()); }
            });
        } else {
            controller = new LoginController(this,navigator);
            loginButton.setOnAction(e -> controller.loginAction());
        }
        card.getStyleClass().add("login-card");
        card.getChildren().add(loginButton); card.setMaxWidth(420);
        VBox layout = new VBox(20,brand,card,AppTheme.muted("Staff access"));
        layout.setAlignment(javafx.geometry.Pos.CENTER); layout.setPadding(new javafx.geometry.Insets(32));
        ScrollPane scroll = new ScrollPane(layout); scroll.setFitToWidth(true); scroll.setFitToHeight(true);
        Scene scene = new Scene(scroll,820,setup ? 850 : 680); AppTheme.apply(scene,stage);
        stage.setScene(scene); stage.setTitle("Login"); stage.show();
    }
    public void display(Stage stage) { start(stage); }
    public void setNavigator(AppNavigator navigator) { this.navigator=navigator; if(controller!=null)controller.setNavigator(navigator); }
    public String getUsername() { return usernameField.getText(); }
    public String getPassword() { return passwordField.getText(); }
    public void setStatus(String status) { statusLabel.setText(status); }
    public Button getLoginButton() { return loginButton; }
}
