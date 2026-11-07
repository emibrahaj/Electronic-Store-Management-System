package Controller;

import DAO.AppPaths;
import DAO.PasswordManager;
import Util.InputValidator;
import Model.Employee;
import Model.Role;
import Model.User;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdminController {
    private static final Path PASSWORD_FILE = AppPaths.daoFile("passwords.txt");
    private final List<User> employees;

    public AdminController() {
        employees = new ArrayList<>();
        loadEmployees();
    }

    public List<User> getEmployees() {
        return Collections.unmodifiableList(employees);
    }

    public void addEmployee(String username, String password, String name, String email,
                            String phoneNumber, Role role, String accessLevel) {
        Util.Session.require(Role.ADMIN);
        validateEmployee(username, password, email, phoneNumber, role);

        if (findEmployee(username) != null) {
            throw new IllegalArgumentException("An employee with this username already exists.");
        }

        validateTextFields(name, accessLevel);

        // Hash password before storing
        String hashedPassword;
        try {
            hashedPassword = PasswordManager.hashPassword(password);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash password: " + e.getMessage(), e);
        }

        User added=new Employee(username, hashedPassword, name, email, phoneNumber, role, accessLevel);
        employees.add(added);
        try { saveEmployees(); } catch(RuntimeException e) { employees.remove(added); throw e; }
    }

    public void updateEmployee(String originalUsername, String username, String password, String name,
                               String email, String phoneNumber, Role role, String accessLevel) {
        Util.Session.require(Role.ADMIN);
        validateEmployee(username, password.isEmpty() ? "unchanged" : password, email, phoneNumber, role);

        validateTextFields(name, accessLevel);
        User existing = findEmployee(originalUsername);
        if (existing == null) {
            throw new IllegalArgumentException("Please select an employee to update.");
        }

        User duplicate = findEmployee(username);
        if (duplicate != null && !duplicate.getUsername().equalsIgnoreCase(originalUsername)) {
            throw new IllegalArgumentException("Another employee already uses this username.");
        }

        if (existing.getRole() == Role.ADMIN && role != Role.ADMIN && getEmployeeCountByRole(Role.ADMIN) <= 1)
            throw new IllegalArgumentException("Keep at least one administrator.");
        if (existing.getUsername().equalsIgnoreCase(Util.Session.username()) &&
                (!username.equalsIgnoreCase(existing.getUsername()) || role != Role.ADMIN))
            throw new IllegalArgumentException("You cannot rename or demote your active administrator account.");
        User original=new Employee(existing.getUsername(),existing.getPassword(),existing.getName(),existing.getEmail(),existing.getPhoneNumber(),existing.getRole(),existing.getAccessLevel());
        existing.setUsername(username);
        try {
            if (!password.isEmpty()) existing.setPassword(PasswordManager.hashPassword(password));
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash password: " + e.getMessage(), e);
        }
        existing.setName(name);
        existing.setEmail(email);
        existing.setPhoneNumber(phoneNumber);
        existing.setRole(role);
        existing.setAccessLevel(accessLevel);
        try { saveEmployees(); } catch(RuntimeException e) { employees.set(employees.indexOf(existing),original); throw e; }
    }

    public void deleteEmployee(User employee) {
        Util.Session.require(Role.ADMIN);
        if (employee == null) {
            throw new IllegalArgumentException("Please select an employee to delete.");
        }

        if (employee.getUsername().equalsIgnoreCase(Util.Session.username()))
            throw new IllegalArgumentException("You cannot delete your active account.");
        if (employee.getRole() == Role.ADMIN && getEmployeeCountByRole(Role.ADMIN) <= 1)
            throw new IllegalArgumentException("Keep at least one administrator.");
        List<User> original=new ArrayList<>(employees);
        employees.removeIf(user -> user.getUsername().equalsIgnoreCase(employee.getUsername()));
        try { saveEmployees(); } catch(RuntimeException e) { employees.clear(); employees.addAll(original); throw e; }
    }

    public User findEmployee(String username) {
        if (username == null) {
            return null;
        }

        return employees.stream()
                .filter(user -> user.getUsername().equalsIgnoreCase(username))
                .findFirst()
                .orElse(null);
    }

    public int getEmployeeCountByRole(Role role) {
        return (int) employees.stream()
                .filter(user -> role.equals(user.getRole()))
                .count();
    }

    private void loadEmployees() {
        employees.clear();

        if (!Files.exists(PASSWORD_FILE)) return;

        try (BufferedReader reader = Files.newBufferedReader(PASSWORD_FILE)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",", -1);
                if (line.isBlank()) continue;
                if (fields.length < 5) throw new IllegalStateException("Malformed credential record; restore the credentials backup.");
                if (fields.length >= 5) {
                    Role role = parseRole(fields[4]);
                    employees.add(new Employee(fields[0], fields[1], fields.length > 5 ? fields[5] : fields[0], fields[2],
                            fields[3], role, fields.length > 6 ? fields[6] : defaultAccessLevel(role)));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not load employees: " + e.getMessage(), e);
        }


    }

    private void saveEmployees() {
        try {
            Files.createDirectories(PASSWORD_FILE.getParent());
            Path temporary = Files.createTempFile(PASSWORD_FILE.getParent(), "employees-", ".tmp");
            try {
            try (BufferedWriter writer = Files.newBufferedWriter(temporary)) {
                for (User employee : employees) {
                    writer.write(String.join(",",
                            employee.getUsername(),
                            employee.getPassword(),
                            employee.getEmail(),
                            employee.getPhoneNumber(),
                            roleToFileValue(employee.getRole()), employee.getName(), employee.getAccessLevel()));
                    writer.newLine();
                }
            }
            DAO.AtomicFiles.replace(temporary, PASSWORD_FILE);
            } finally { Files.deleteIfExists(temporary); }
        } catch (IOException e) {
            throw new IllegalStateException("Could not save employees: " + e.getMessage(), e);
        }
    }

    public static synchronized void createFirstAdmin(String username, String password, String name, String email, String phone) {
        AdminController controller = new AdminController();
        if (!controller.employees.isEmpty()) throw new IllegalStateException("An administrator is already configured.");
        controller.validateEmployee(username, password, email, phone, Role.ADMIN);
        controller.validateTextFields(name, "Full Access");
        try {
            controller.employees.add(new Employee(username, PasswordManager.hashPassword(password), name, email, phone, Role.ADMIN, "Full Access"));
            controller.saveEmployees();
        } catch (Exception e) { throw new IllegalStateException("Could not create administrator.", e); }
    }

    private void validateEmployee(String username, String password, String email, String phoneNumber, Role role) {
        if (!InputValidator.isValidUsername(username)) {
            throw new IllegalArgumentException(InputValidator.getValidationErrorMessage("Username", "username"));
        }
        if (!InputValidator.isValidPassword(password)) {
            throw new IllegalArgumentException(InputValidator.getValidationErrorMessage("Password", "password"));
        }
        if (!InputValidator.isValidEmail(email)) {
            throw new IllegalArgumentException(InputValidator.getValidationErrorMessage("Email", "email"));
        }
        if (!InputValidator.isValidPhoneNumber(phoneNumber)) {
            throw new IllegalArgumentException(InputValidator.getValidationErrorMessage("Phone Number", "phone"));
        }
        if (role == null) {
            throw new IllegalArgumentException("Role is required.");
        }
    }

    private void validateTextFields(String... values) {
        for (String value : values) {
            if (value == null || value.contains(",") || value.contains("\n") || value.contains("\r"))
                throw new IllegalArgumentException("Name and access level cannot contain commas or line breaks.");
        }
    }

    private Role parseRole(String role) {
        return Role.valueOf(role.trim().toUpperCase());
    }

    private String roleToFileValue(Role role) {
        String value = role.name().toLowerCase();
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private String defaultAccessLevel(Role role) {
        switch (role) {
            case ADMIN:
                return "Full Access";
            case MANAGER:
                return "Inventory and reports";
            case CASHIER:
                return "Sales";
            default:
                return "Standard";
        }
    }

}
