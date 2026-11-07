package Util;
import Model.Role;
import Model.User;

public final class Session {
    private static User user;
    private Session() { }
    public static void signIn(User account) { user = java.util.Objects.requireNonNull(account); }
    public static void clear() { user = null; }
    public static String username() { return user == null ? "system" : user.getUsername(); }
    public static Role role() { return user == null ? null : user.getRole(); }
    public static void require(Role... allowed) {
        for (Role role : allowed) if (user != null && user.getRole() == role) return;
        throw new SecurityException("You do not have permission for this operation.");
    }
}
