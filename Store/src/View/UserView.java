package View;

import Model.User;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.List;

public class UserView {
    private final TableView<User> tableView;

    public UserView() {
        tableView = new TableView<>();
        setupColumns();
    }

    public TableView<User> getTableView() {
        return tableView;
    }

    public User getSelectedUser() {
        return tableView.getSelectionModel().getSelectedItem();
    }

    public void setUsers(List<User> users) {
        tableView.setItems(FXCollections.observableArrayList(users));
    }

    private void setupColumns() {
        TableColumn<User, String> usernameColumn = createColumn("Username", User::getUsername);
        TableColumn<User, String> nameColumn = createColumn("Name", User::getName);
        TableColumn<User, String> roleColumn = createColumn("Role", user -> user.getRole() == null ? "" : user.getRole().name());
        TableColumn<User, String> emailColumn = createColumn("Email", User::getEmail);
        TableColumn<User, String> phoneColumn = createColumn("Phone", User::getPhoneNumber);
        TableColumn<User, String> accessColumn = createColumn("Access", User::getAccessLevel);

        tableView.getColumns().addAll(usernameColumn, nameColumn, roleColumn, emailColumn, phoneColumn, accessColumn);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private TableColumn<User, String> createColumn(String title, java.util.function.Function<User, String> mapper) {
        TableColumn<User, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(nullToEmpty(mapper.apply(cellData.getValue()))));
        return column;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
