package View;

import Controller.ManagerController;
import Model.Item;
import Model.Supplier;
import Util.AppNavigator;
import View.LoginView;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;

public class ManagerDashboardView {
    private Stage stage;
    private ManagerController controller;
    private AppNavigator navigator;

    private TextField barcodeField;
    private TextField itemNameField, categoryField, supplierField, supplierContactField, purchasePriceField, sellingPriceField, stockField, searchField;
    private Button addItemButton, addStockButton, removeItemButton, updateItemButton, logoutButton, viewStatisticsButton;
    private TableView<Item> inventoryTable;
    private TableView<Supplier> supplierTable;

    public ManagerDashboardView(Stage stage, ManagerController controller) {
        this.stage = stage;
        this.controller = controller;
    }

    public void start(Stage stage) {
        checkOutOfStockItems();
        setupInputFields();
        setupButtons();
        setupTables();

        VBox mainLayout = createMainLayout();
        ScrollPane scroll = new ScrollPane(mainLayout);
        scroll.setFitToWidth(true); scroll.setFitToHeight(true);
        Scene scene = new Scene(scroll, 1180, 860);
        AppTheme.apply(scene, stage);
        stage.setMinWidth(1000);
        stage.setScene(scene);
        stage.setTitle("Manager Dashboard");
        stage.show();
    }

    private void setupInputFields() {
        barcodeField = createStyledTextField("Optional barcode");
        itemNameField = createStyledTextField("Item Name");
        categoryField = createStyledTextField("Category");
        supplierField = createStyledTextField("Supplier");
        supplierContactField = createStyledTextField("Supplier Contact");
        purchasePriceField = createStyledTextField("Purchase Price");
        sellingPriceField = createStyledTextField("Selling Price");
        stockField = createStyledTextField("Stock Quantity");
        searchField = createStyledTextField("Search Item");
        searchField.setOnKeyReleased(e -> filterInventory(searchField.getText()));
    }

    private void setupInventoryTable() {
        TableColumn<Item, String> itemNameCol = new TableColumn<>("Item Name");
        itemNameCol.setCellValueFactory(cellData -> cellData.getValue().itemNameProperty());

        TableColumn<Item, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(cellData -> cellData.getValue().categoryProperty());

        TableColumn<Item, Double> purchasePriceCol = new TableColumn<>("Purchase Price");
        purchasePriceCol.setCellValueFactory(cellData -> cellData.getValue().purchasePriceProperty().asObject());

        TableColumn<Item, Integer> stockCol = new TableColumn<>("Stock");
        stockCol.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getStock()).asObject());

        TableColumn<Item,String> sku = new TableColumn<>("SKU");
        sku.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSku()));
        TableColumn<Item,String> barcode = new TableColumn<>("Barcode");
        barcode.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getBarcode()));
        inventoryTable.getColumns().addAll(itemNameCol, categoryCol, purchasePriceCol, stockCol,sku,barcode);
        inventoryTable.setItems(FXCollections.observableArrayList(controller.getManager().getInventory().getItems()));
    }

    private void setupSupplierTable() {
        TableColumn<Supplier, String> supplierNameCol = new TableColumn<>("Supplier");
        supplierNameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getSupplierName()));

        TableColumn<Supplier, String> contactCol = new TableColumn<>("Contact Details");
        contactCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getContactDetails()));

        TableColumn<Supplier, String> productCol = new TableColumn<>("Product");
        productCol.setCellValueFactory(cellData -> {
            Supplier supplier = cellData.getValue();
            return new SimpleStringProperty(
                    supplier.getSuppliedItems().isEmpty() ? "No Products" : supplier.getSuppliedItems().get(0).getItemName()
            );
        });

        supplierTable.getColumns().addAll(supplierNameCol, contactCol, productCol);
        supplierTable.setItems(FXCollections.observableArrayList(controller.getAllSuppliers()));
    }


    private void setupButtons() {
        addItemButton = createStyledButton("Add Item", e -> addItemToInventory());
        addStockButton = createStyledButton("Add Stock", e -> showAddStockDialog());
        removeItemButton = createStyledButton("Remove Item", e -> removeItemFromInventory());
        updateItemButton = createStyledButton("Update Item", e -> updateItemInInventory());
        viewStatisticsButton = createStyledButton("View Statistics", e -> openStatisticsView());
        logoutButton = createStyledButton("Logout", e -> logout());
    }

    private void setupTables() {
        inventoryTable = new TableView<>();
        inventoryTable.getSelectionModel().selectedItemProperty().addListener((obs, previous, selected) -> {
            if (selected == null) return;
            itemNameField.setText(selected.getItemName());
            categoryField.setText(selected.getCategory());
            supplierField.setText(selected.getSupplier());
            purchasePriceField.setText(String.valueOf(selected.getPurchasePrice()));
            sellingPriceField.setText(String.valueOf(selected.getSellingPrice()));
            stockField.setText(String.valueOf(selected.getStock()));
            barcodeField.setText(selected.getBarcode());
        });
        setupInventoryTable();

        supplierTable = new TableView<>();
        setupSupplierTable();
    }

    public void setNavigator(AppNavigator navigator) {
        this.navigator = navigator;
    }

    private VBox createMainLayout() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        AppTheme.primary(addItemButton); AppTheme.danger(removeItemButton);
        AppTheme.table(inventoryTable, "No products yet. Add one above.");
        AppTheme.table(supplierTable, "Suppliers appear when you add inventory.");
        Tab inventoryTab = new Tab("Inventory", inventoryTable);
        Tab suppliersTab = new Tab("Suppliers", supplierTable);
        inventoryTab.setClosable(false); suppliersTab.setClosable(false);
        TabPane records = new TabPane(inventoryTab, suppliersTab);
        records.setMinHeight(260); VBox.setVgrow(records, Priority.ALWAYS);
        layout.getChildren().addAll(
            AppTheme.header("Stockroom", "Products & the people who supply them", viewStatisticsButton, logoutButton),
            AppTheme.card(createInputGrid(), createButtonBox()), searchField, records);
        return layout;
    }

    private GridPane createInputGrid() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(10);
        grid.setVgap(10);

        Control[] fields = {itemNameField, categoryField, supplierField, supplierContactField,
                purchasePriceField, sellingPriceField, stockField, barcodeField};
        String[] labels = {"Item name", "Category", "Supplier", "Supplier contact", "Purchase price", "Selling price", "Stock quantity", "Barcode"};
        for (int i = 0; i < fields.length; i++) {
            VBox field = AppTheme.field(labels[i], fields[i]);
            grid.add(field, i % 2, i / 2); GridPane.setHgrow(field, Priority.ALWAYS);
            fields[i].setMaxWidth(Double.MAX_VALUE);
        }
        for (int i = 0; i < 2; i++) { ColumnConstraints c = new ColumnConstraints(); c.setPercentWidth(50); grid.getColumnConstraints().add(c); }
        return grid;
    }

    private void addGridRow(GridPane grid, int row, String label, Control field) {
        grid.addRow(row, createStyledLabel(label), field);
    }

    private HBox createButtonBox() {
        return new HBox(10, addItemButton, updateItemButton, addStockButton, removeItemButton);
    }

    private void openStatisticsView() {
        new StatisticsView(stage, controller, () -> start(stage)).show();
    }

    private void logout() {
        if (navigator != null) {
            navigator.logout();
        } else {
            stage.close();
            Stage loginStage = new Stage();
            new LoginView().start(loginStage);
        }
    }

    private void showAddStockDialog() {
        Item selectedItem = inventoryTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showAlert("Error", "Please select an item to add stock.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Stock");
        dialog.setHeaderText("Updating Stock for: " + selectedItem.getItemName());
        dialog.setContentText("Enter quantity to add:");

        AppTheme.dialog(dialog);
        dialog.showAndWait().ifPresent(input -> {
            try {
                int addedStock = Integer.parseInt(input);
                if (addedStock <= 0) {
                    showAlert("Error", "Stock quantity must be a positive number.");
                    return;
                }

                controller.updateItem(selectedItem.getItemName(), selectedItem.getPurchasePrice(), selectedItem.getSellingPrice(), selectedItem.getStock() + addedStock);
                refreshInventoryTable();
                showAlert("Success", "Stock updated successfully!");
            } catch (RuntimeException e) {
                showAlert("Error", e.getMessage());
            }
        });
    }

    private void checkOutOfStockItems() {
        List<Item> outOfStockItems = controller.getManager().getInventory().getItems().stream()
                .filter(item -> item.getStock() <= 0)
                .toList();

        if (!outOfStockItems.isEmpty()) {
            showAlert("Stock Alert", "The following items are out of stock:\n" + outOfStockItems.stream()
                    .map(Item::getItemName)
                    .reduce("", (a, b) -> a + "- " + b + "\n"));
        }
    }

    private void addItemToInventory() {
        if (!validateInput()) return;

        try {
            Item item = new Item(itemNameField.getText().trim(), categoryField.getText().trim(), supplierField.getText().trim(),
                    Util.Money.of(purchasePriceField.getText()).doubleValue(), Util.Money.of(sellingPriceField.getText()).doubleValue(),
                    Integer.parseInt(stockField.getText()));
            item.setBarcode(barcodeField.getText().trim());
            controller.addItemToInventory(item, supplierContactField.getText().trim());
            refreshInventoryTable();
            refreshSupplierTable();
            showAlert("Success", "Item and supplier added successfully!");
        } catch (RuntimeException e) { showAlert("Error", e.getMessage()); }
    }

    private void removeItemFromInventory() {
        Item selectedItem = inventoryTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showAlert("Error", "Please select an item to remove.");
            return;
        }

        controller.removeItem(selectedItem.getItemName());
        refreshInventoryTable();
        showAlert("Success", "Item removed successfully!");
    }

    private void updateItemInInventory() {
        Item selected = inventoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Error", "Select an item to update."); return; }
        try {
            controller.getManager().updateProduct(selected.getSku(), Util.Money.of(purchasePriceField.getText()).doubleValue(),
                    Util.Money.of(sellingPriceField.getText()).doubleValue(), Integer.parseInt(stockField.getText()),barcodeField.getText().trim());
            refreshInventoryTable();
            showAlert("Success", "Item updated successfully!");
        } catch (RuntimeException e) { showAlert("Error", e.getMessage()); }
    }

    private void refreshInventoryTable() {
        inventoryTable.setItems(FXCollections.observableArrayList(controller.getManager().getInventory().getItems()));
    }

    private void refreshSupplierTable() {
        supplierTable.setItems(FXCollections.observableArrayList(controller.getAllSuppliers()));
    }

    private void filterInventory(String query) {
        inventoryTable.setItems(FXCollections.observableArrayList(
                controller.getManager().getInventory().getItems().stream()
                        .filter(item -> item.getItemName().toLowerCase().contains(query.toLowerCase()))
                        .toList()));
    }

    private TextField createStyledTextField(String prompt) {
        TextField textField = new TextField();
        textField.setPromptText(prompt);
        textField.setMaxWidth(300);
        return textField;
    }

    private Button createStyledButton(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button button = new Button(text);
        button.setOnAction(handler);
        return button;
    }

    private Label createStyledLabel(String text) {
        Label label = new Label(text);
        return label;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        AppTheme.dialog(alert);
        alert.showAndWait();
    }

    private boolean validateInput() {
        if (!Util.InputValidator.isValidItemName(itemNameField.getText().trim())
                || !Util.InputValidator.isValidCategory(categoryField.getText().trim())
                || supplierField.getText().isBlank()) {
            showAlert("Error", "Enter a valid item name, category, and supplier.");
            return false;
        }
        return true;
    }
}
