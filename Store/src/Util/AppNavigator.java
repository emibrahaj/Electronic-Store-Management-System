package Util;

import Controller.AdminController;
import Controller.CashierController;
import Controller.LoginController;
import Controller.ManagerController;
import Model.Cashier;
import Model.Inventory;
import Model.Manager;
import View.AdminDashboardView;
import View.CashierDashboardView;
import View.LoginView;
import View.ManagerDashboardView;
import javafx.stage.Stage;

/**
 * Central navigation controller for the application.
 * Manages all screen transitions and dependency injection.
 * Decouples Views from Controllers and eliminates tight coupling.
 */
public class AppNavigator {
    private Stage primaryStage;
    private SharedContext context;

    /**
     * Creates AppNavigator with the primary stage.
     *
     * @param primaryStage the main application stage
     */
    public AppNavigator(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.context = SharedContext.getInstance();
    }

    /**
     * Navigates to login screen.
     */
    public void navigateToLogin() {
        Logger.info("AppNavigator", "Navigating to Login screen");
        try {
            LoginView loginView = new LoginView();
            loginView.setNavigator(this);
            loginView.display(primaryStage);
        } catch (Exception e) {
            Logger.error("AppNavigator", "Failed to navigate to login", e);
            showError("Navigation Error", "Could not open login screen: " + e.getMessage());
        }
    }

    /**
     * Navigates to admin dashboard.
     */
    public void navigateToAdminDashboard() {
        Session.require(Model.Role.ADMIN);
        Logger.info("AppNavigator", "Navigating to Admin Dashboard");
        try {
            AdminDashboardView adminView = new AdminDashboardView();
            adminView.setNavigator(this);
            adminView.show(primaryStage);
        } catch (Exception e) {
            Logger.error("AppNavigator", "Failed to navigate to admin dashboard", e);
            showError("Navigation Error", "Could not open admin dashboard: " + e.getMessage());
        }
    }

    /**
     * Navigates to manager dashboard.
     *
     * @param managerUsername the username of the logged-in manager
     * @param password manager's password
     * @param email manager's email
     * @param phone manager's phone
     */
    public void navigateToManagerDashboard(String managerUsername, String password, String email, String phone) {
        Session.require(Model.Role.MANAGER, Model.Role.ADMIN);
        Logger.info("AppNavigator", "Navigating to Manager Dashboard for: " + managerUsername);
        try {
            Manager manager = new Manager(managerUsername, password, managerUsername, email, phone);
            ManagerController managerController = new ManagerController(manager);
            ManagerDashboardView managerView = new ManagerDashboardView(primaryStage, managerController);
            managerView.setNavigator(this);
            managerView.start(primaryStage);
        } catch (Exception e) {
            Logger.error("AppNavigator", "Failed to navigate to manager dashboard", e);
            showError("Navigation Error", "Could not open manager dashboard: " + e.getMessage());
        }
    }

    /**
     * Navigates to cashier dashboard.
     *
     * @param cashierUsername the username of the logged-in cashier
     * @param password cashier's password
     * @param email cashier's email
     * @param phone cashier's phone
     */
    public void navigateToCashierDashboard(String cashierUsername, String password, String email, String phone) {
        Session.require(Model.Role.CASHIER, Model.Role.ADMIN);
        Logger.info("AppNavigator", "Navigating to Cashier Dashboard for: " + cashierUsername);
        try {
            Inventory sharedInventory = context.getInventory();
            Cashier cashier = new Cashier(cashierUsername, password, cashierUsername, email, phone, "Electronics", sharedInventory);
            CashierController cashierController = new CashierController(cashier, sharedInventory);
            CashierDashboardView cashierView = new CashierDashboardView(primaryStage, cashierController);
            cashierView.setNavigator(this);
            cashierView.start();
        } catch (Exception e) {
            Logger.error("AppNavigator", "Failed to navigate to cashier dashboard", e);
            showError("Navigation Error", "Could not open cashier dashboard: " + e.getMessage());
        }
    }

    /**
     * Logout and return to login screen.
     */
    public void logout() {
        Logger.info("AppNavigator", "User " + context.getCurrentUsername() + " logging out");
        Session.clear();
        context.clearUserContext();
        navigateToLogin();
    }

    /**
     * Shows an error dialog.
     *
     * @param title the dialog title
     * @param message the error message
     */
    public void showError(String title, String message) {
        Logger.warn("AppNavigator", title + ": " + message);
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        View.AppTheme.dialog(alert);
        alert.showAndWait();
    }

    /**
     * Gets the current shared context.
     *
     * @return the SharedContext instance
     */
    public SharedContext getContext() {
        return context;
    }

    /**
     * Gets the primary stage.
     *
     * @return the primary Stage
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }
}
