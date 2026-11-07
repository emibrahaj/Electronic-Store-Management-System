package Util;
import Controller.AdminController;
import DAO.PasswordManager;
import Model.User;

public final class AuthService {
    private AuthService() { }
    public static boolean needsSetup() { return new AdminController().getEmployees().isEmpty(); }
    public static User authenticate(String username, String password) {
        Session.clear();
        User user = new AdminController().findEmployee(username);
        boolean matches = false;
        try {
            if (user != null) matches = user.getPassword().contains(":")
                    ? PasswordManager.verifyPassword(password, user.getPassword()) : user.getPassword().equals(password);
        } catch (Exception ignored) { }
        if (!matches) throw new IllegalArgumentException("Incorrect username or password.");
        Session.signIn(user); return user;
    }
}
