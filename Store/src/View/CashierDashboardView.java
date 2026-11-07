package View;

import Controller.CashierController;
import Model.Item;
import Model.Bill;
import Util.AppNavigator;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import java.util.Optional;

public class CashierDashboardView {
    private Stage stage;
    private CashierController controller;
    private AppNavigator navigator;
    private TextField searchField;
    private TextField quantityField;
    private Button addItemButton, finalizeBillButton, refreshBillsButton, returnItemButton;
    private Label totalLabel;
    private Label salesLabel;
    private TableView<Item> cartTable;
    private TableView<Item> inventoryTable;
    private TableView<Bill> billTable;
    private ObservableList<Item> inventoryItems;

    public CashierDashboardView(Stage stage, CashierController controller) {
        this.stage = stage;
        this.controller = controller;
        inventoryItems = FXCollections.observableArrayList(controller.getInventoryItems());
    }

    public void start() {
        // Search field
        searchField = new TextField();
        searchField.setPromptText("Search item...");
        searchField.textProperty().addListener((observable, oldValue, newValue) -> filterInventory(newValue));

        // Quantity field
        quantityField = new TextField();
        quantityField.setPromptText("Quantity");

        // Buttons
        addItemButton = createStyledButton("Add Item");
        addItemButton.setOnAction(e -> addItemToBill());

        finalizeBillButton = createStyledButton("Finalize Bill");
        finalizeBillButton.setOnAction(e -> finalizeBill());

        returnItemButton = createStyledButton("Process Return");
        returnItemButton.setOnAction(e -> showReturnDialog());

        refreshBillsButton = createStyledButton("Refresh Today's Bills");
        refreshBillsButton.setOnAction(e -> loadTodaysBills());

        totalLabel = new Label("Cart total: $0.00");
        salesLabel = new Label();

        // Setup inventory table and load data
        setupInventoryTable();
        loadInventoryData();

        // Setup bill table and load data
        setupBillTable();
        loadTodaysBills();

        Button logoutButton = createStyledButton("Logout");
        logoutButton.setOnAction(e -> { if (navigator != null) navigator.logout(); });
        AppTheme.primary(addItemButton); AppTheme.primary(finalizeBillButton);
        AppTheme.table(inventoryTable, "No products available yet.");
        AppTheme.table(billTable, "Completed sales will appear here.");
        totalLabel.getStyleClass().add("section-title"); totalLabel.setWrapText(true);
        quantityField.setPrefWidth(110);
        cartTable = new TableView<>();
        TableColumn<Item,String> cartName = new TableColumn<>("Product");
        cartName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getItemName()));
        TableColumn<Item,String> cartQuantity = new TableColumn<>("Qty");
        cartQuantity.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().getQuantity())));
        TableColumn<Item,String> cartPrice = new TableColumn<>("Unit price");
        cartPrice.setCellValueFactory(cell -> new SimpleStringProperty(Util.Money.format(cell.getValue().getSellingAmount())));
        TableColumn<Item,String> cartSubtotal = new TableColumn<>("Subtotal");
        cartSubtotal.setCellValueFactory(cell -> new SimpleStringProperty(Util.Money.format(cell.getValue().getSellingAmount().multiply(java.math.BigDecimal.valueOf(cell.getValue().getQuantity())))));
        cartTable.getColumns().addAll(cartName,cartQuantity,cartPrice,cartSubtotal);
        refreshCart();
        AppTheme.table(cartTable,"Your cart is empty. Add a product or scan a barcode.");
        Button change = createStyledButton("Change quantity"); change.setOnAction(e -> changeCartQuantity());
        Button remove = AppTheme.danger(createStyledButton("Remove")); remove.setOnAction(e -> {
            Item selected=cartTable.getSelectionModel().getSelectedItem();
            if(selected!=null) { controller.updateCartQuantity(selected.getSku(),0); refreshCart(); }
        });
        Button cancel = createStyledButton("Cancel sale"); cancel.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,"Discard this cart?",ButtonType.OK,ButtonType.CANCEL);
            AppTheme.dialog(confirm);
            if(confirm.showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK) { controller.cancelBill(); refreshCart(); }
        });
        TextField barcode = new TextField(); barcode.setPromptText("Scan barcode or enter SKU, then press Enter");
        barcode.setOnAction(e -> {
            try { controller.scanBarcode(barcode.getText().trim(),1); barcode.clear(); refreshCart(); }
            catch(RuntimeException error) { showAlert("Scan product",error.getMessage()); }
        });
        VBox products = AppTheme.card(AppTheme.title("Products"), searchField, barcode, inventoryTable,
                new HBox(10, AppTheme.field("Quantity", quantityField), addItemButton));
        VBox cart = AppTheme.card(AppTheme.title("Current sale"),cartTable,totalLabel,
                new HBox(10,change,remove),new HBox(10,finalizeBillButton,cancel));
        VBox history = AppTheme.card(AppTheme.title("Sales today"),salesLabel, billTable,
                new HBox(10,returnItemButton,refreshBillsButton));
        Tab current = new Tab("Checkout",cart); current.setClosable(false);
        Tab completed = new Tab("Completed sales",history); completed.setClosable(false);
        TabPane salesTabs = new TabPane(current,completed);
        VBox sales = new VBox(salesTabs); VBox.setVgrow(salesTabs,Priority.ALWAYS);
        VBox.setVgrow(cartTable,Priority.ALWAYS);
        VBox.setVgrow(inventoryTable, Priority.ALWAYS); VBox.setVgrow(billTable, Priority.ALWAYS);
        products.setMinWidth(0); sales.setMinWidth(0);
        SplitPane panels = new SplitPane(products, sales); panels.setDividerPositions(0.48);
        VBox layout = new VBox(24, AppTheme.header("Point of sale", "Select products, build a bill, and complete the sale.", logoutButton), panels);
        VBox.setVgrow(panels, Priority.ALWAYS);
        layout.setPadding(new Insets(28));
        Scene scene = new Scene(layout, 1200, 780);
        AppTheme.apply(scene, stage);
        stage.setMinWidth(1000);
        stage.setScene(scene);
        stage.setTitle("Cashier Dashboard");
        stage.show();
    }

    public void setNavigator(AppNavigator navigator) {
        this.navigator = navigator;
    }

    private void setupInventoryTable() {
        inventoryTable = new TableView<>();
        inventoryTable.setItems(inventoryItems);

        TableColumn<Item, String> itemNameCol = new TableColumn<>("Item Name");
        itemNameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getItemName()));

        TableColumn<Item, String> stockCol = new TableColumn<>("Stock");
        stockCol.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getStock())));

        TableColumn<Item, String> priceCol = new TableColumn<>("Selling Price");
        priceCol.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getSellingPrice())));

        TableColumn<Item,String> skuCol = new TableColumn<>("SKU");
        skuCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSku()));
        inventoryTable.getColumns().addAll(itemNameCol, stockCol, priceCol,skuCol);
    }

    private void loadInventoryData() {
        inventoryItems.setAll(controller.getInventoryItems());
        if (inventoryTable != null) { filterInventory(searchField.getText()); inventoryTable.refresh(); }
    }

    private void setupBillTable() {
        billTable = new TableView<>();

        TableColumn<Bill, String> billNumberCol = new TableColumn<>("Bill Number");
        billNumberCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getBillNumber()));

        TableColumn<Bill, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDate().toString()));

        TableColumn<Bill, Double> totalAmountCol = new TableColumn<>("Total Amount");
        totalAmountCol.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getTotalAmount()).asObject());

        TableColumn<Bill, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cellData -> {
            Bill bill = cellData.getValue();
            String status = bill.isFullyRefunded() ? "Refunded"
                    : bill.getRefundedAmount() > 0 ? "Partial Return" : "Completed";
            return new SimpleStringProperty(status);
        });

        billTable.getColumns().addAll(billNumberCol, dateCol, totalAmountCol, statusCol);
    }

    private void loadTodaysBills() {
        ObservableList<Bill> todaysBills = FXCollections.observableArrayList(controller.getTodaysBills());
        billTable.setItems(todaysBills);

        double totalSales = controller.getTotalOfTodaysBills();
        salesLabel.setText("Today's net sales: " + Util.Money.format(Util.Money.of(totalSales)));
    }

    // Search function for filtering inventory table
    private void filterInventory(String query) {
        ObservableList<Item> filteredList = FXCollections.observableArrayList();
        for (Item item : controller.getInventoryItems()) {
            if (item.getItemName().toLowerCase().contains(query.toLowerCase()) || item.getSku().contains(query) || item.getBarcode().contains(query)) {
                filteredList.add(item);
            }
        }
        inventoryTable.setItems(filteredList);
    }

    private void addItemToBill() {
        Item selectedItem = inventoryTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showAlert("Error", "Please select an item.");
            return;
        }
        try {
            int quantity = Integer.parseInt(quantityField.getText());

            if (quantity > selectedItem.getStock()) {
                showAlert("Error", "Not enough stock available.");
                return;
            }

            controller.addItemToBill(selectedItem.getItemName(), quantity);
            refreshCart();

            loadInventoryData(); // Reload inventory to reflect updated stock
        } catch (IllegalArgumentException e) {
            showAlert("Error", e.getMessage());
        }
    }

    private void finalizeBill() {
        try {
            controller.finalizeBill();
        } catch (RuntimeException e) {
            showAlert("Error", e.getMessage());
            return;
        }
        refreshCart();
        showAlert("Sale completed", "Payment recorded and stock updated.");
        loadInventoryData();  // Reload inventory to reflect stock update
        loadTodaysBills();  // Reload bills to show new finalized bill
    }

    private void refreshCart() {
        cartTable.setItems(FXCollections.observableArrayList(controller.getCartItems()));
        totalLabel.setText("Cart total: " + Util.Money.format(Util.Money.of(controller.getTotalAmount())));
    }
    private void changeCartQuantity() {
        Item selected=cartTable.getSelectionModel().getSelectedItem();
        if(selected==null) { showAlert("Cart", "Select a cart item first."); return; }
        TextInputDialog dialog = new TextInputDialog(String.valueOf(selected.getQuantity()));
        dialog.setTitle("Change quantity"); dialog.setHeaderText(selected.getItemName()); AppTheme.dialog(dialog);
        dialog.showAndWait().ifPresent(value -> {
            try { controller.updateCartQuantity(selected.getSku(),Integer.parseInt(value)); refreshCart(); }
            catch(RuntimeException e) { showAlert("Quantity",e.getMessage()); }
        });
    }

    private void showReturnDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Process Return");
        dialog.setHeaderText("Return item from a completed bill");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField billNumberField = new TextField();
        billNumberField.setPromptText("Bill Number");
        TextField returnItemNameField = new TextField();
        returnItemNameField.setPromptText("Item Name");
        TextField returnQuantityField = new TextField();
        returnQuantityField.setPromptText("Quantity");

        grid.add(new Label("Bill Number:"), 0, 0);
        grid.add(billNumberField, 1, 0);
        grid.add(new Label("Item Name:"), 0, 1);
        grid.add(returnItemNameField, 1, 1);
        grid.add(new Label("Quantity:"), 0, 2);
        grid.add(returnQuantityField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        AppTheme.dialog(dialog);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                String billNumber = billNumberField.getText().trim();
                String itemName = returnItemNameField.getText().trim();
                int quantity = Integer.parseInt(returnQuantityField.getText().trim());

                if (billNumber.isEmpty() || itemName.isEmpty() || quantity <= 0) {
                    showAlert("Error", "Please provide valid return details.");
                    return;
                }

                if (controller.returnItemFromBill(billNumber, itemName, quantity)) {
                    showAlert("Success", "Return processed successfully.");
                    loadInventoryData();
                    loadTodaysBills();
                    salesLabel.setText("Today's net sales: " + Util.Money.format(Util.Money.of(controller.getTotalOfTodaysBills())));
                } else {
                    showAlert("Error", "Unable to process return. Check the bill number, item name, and quantity.");
                }
            } catch (RuntimeException e) {
                showAlert("Error", e.getMessage());
            }
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        AppTheme.dialog(alert);
        alert.showAndWait();
    }

    private Button createStyledButton(String text) {
        Button button = new Button(text);
        return button;
    }

    private Label createStyledLabel(String text) {
        Label label = new Label(text);
        return label;
    }
}
