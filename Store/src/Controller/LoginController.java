package Controller;
import Model.User;
import Util.*;
import View.LoginView;

public class LoginController {
    private final LoginView view;
    private AppNavigator navigator;
    public LoginController(LoginView view) { this(view, null); }
    public LoginController(LoginView view, AppNavigator navigator) { this.view=view; this.navigator=navigator; }
    public void setNavigator(AppNavigator navigator) { this.navigator=navigator; }
    public void loginAction() {
        try {
            User user = AuthService.authenticate(view.getUsername().trim(), view.getPassword());
            SharedContext context = SharedContext.getInstance();
            context.setCurrentUsername(user.getUsername());
            String role = user.getRole().name().substring(0,1) + user.getRole().name().substring(1).toLowerCase();
            context.setCurrentUserRole(role);
            if (navigator == null) navigator = new AppNavigator((javafx.stage.Stage)view.getLoginButton().getScene().getWindow());
            switch (user.getRole()) {
                case ADMIN -> navigator.navigateToAdminDashboard();
                case MANAGER -> navigator.navigateToManagerDashboard(user.getUsername(), "", user.getEmail(), user.getPhoneNumber());
                case CASHIER -> navigator.navigateToCashierDashboard(user.getUsername(), "", user.getEmail(), user.getPhoneNumber());
            }
        } catch (RuntimeException e) { view.setStatus(e.getMessage()); }
    }
    public void logoutAction() { if (navigator != null) navigator.logout(); }
}
