package View;

import Controller.CashierController;
import Model.Bill;
import Model.Item;
import Util.Money;
import Util.AppNavigator;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.math.BigDecimal;
import java.util.*;

/** Checkout keeps frequent actions inline; history and returns stay one click away. */
public class CashierDashboardView {
    private final Stage stage;
    private final CashierController controller;
    private AppNavigator navigator;
    private TextField searchField;
    private TilePane productGrid;
    private Label productCount, totalLabel, itemCount, salesLabel, status;
    private Button completeButton, cancelButton;
    private ListView<Item> cart;
    private final ObservableList<Item> cartItems = FXCollections.observableArrayList();
    private TableView<Bill> billTable;
    private String category;
    private final PauseTransition noticeTimeout = new PauseTransition(Duration.seconds(7));

    public CashierDashboardView(Stage stage, CashierController controller) {
        this.stage = stage; this.controller = controller;
    }
    public void setNavigator(AppNavigator navigator) { this.navigator = navigator; }

    public void start() {
        BorderPane page = new BorderPane();
        page.setPadding(new Insets(24)); page.getStyleClass().add("checkout-page");
        Button logout = new Button("Log out");
        logout.getStyleClass().add("quiet-button");
        logout.setOnAction(e -> logout());
        ToggleButton checkoutTab = new ToggleButton("Checkout"); checkoutTab.setId("checkout-tab");
        ToggleButton historyTab = new ToggleButton("Sales today"); historyTab.setId("history-tab");
        ToggleGroup navigation = new ToggleGroup();
        checkoutTab.setToggleGroup(navigation); historyTab.setToggleGroup(navigation); checkoutTab.setSelected(true);
        checkoutTab.getStyleClass().add("nav-button"); historyTab.getStyleClass().add("nav-button");
        HBox tabs = new HBox(4,checkoutTab,historyTab); tabs.getStyleClass().add("segmented-nav");
        Label brand = label("storepos", "checkout-brand");
        Label user = AppTheme.muted(Util.Session.username());
        Region spacer = new Region(); HBox.setHgrow(spacer,Priority.ALWAYS);
        HBox header = new HBox(20,brand,tabs,spacer,user,logout); header.setAlignment(Pos.CENTER_LEFT);
        status = new Label(); status.setWrapText(true); status.setMaxWidth(Double.MAX_VALUE);
        status.setVisible(false); status.setManaged(false); status.setId("checkout-notice");
        VBox top = new VBox(16,header,status); top.setPadding(new Insets(0,0,24,0)); page.setTop(top);
        noticeTimeout.setOnFinished(e -> { status.setVisible(false); status.setManaged(false); });

        VBox products = productsPanel();
        VBox order = orderPanel();
        HBox checkout = new HBox(24,products,order);
        HBox.setHgrow(products,Priority.ALWAYS); products.setMinWidth(0);
        order.setPrefWidth(410); order.setMinWidth(380); order.setMaxWidth(450);
        VBox history = historyPanel();
        page.setCenter(checkout);
        navigation.selectedToggleProperty().addListener((obs,previous,selected) -> {
            if(selected == null) { navigation.selectToggle(previous); return; }
            if(selected == historyTab) { loadTodaysBills(); page.setCenter(history); }
            else { page.setCenter(checkout); searchField.requestFocus(); }
        });
        Scene scene = new Scene(page,1240,800);
        AppTheme.apply(scene,stage); stage.setMinWidth(1020); stage.setMinHeight(690);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F,KeyCombination.CONTROL_DOWN),() -> {
            checkoutTab.setSelected(true); searchField.requestFocus(); searchField.selectAll();
        });
        stage.setScene(scene); stage.setTitle("StorePOS | Checkout");
        refreshOrder(); refreshProducts(); loadTodaysBills(); stage.show();
        javafx.application.Platform.runLater(searchField::requestFocus);
    }

    private VBox productsPanel() {
        Label title = label("Products", "page-title");
        productCount = AppTheme.muted("");
        Region gap = new Region(); HBox.setHgrow(gap,Priority.ALWAYS);
        HBox heading = new HBox(title,gap,productCount); heading.setAlignment(Pos.CENTER_LEFT);
        searchField = new TextField(); searchField.setId("product-search");
        searchField.setPromptText("Search products or scan a barcode"); searchField.getStyleClass().add("product-search");
        searchField.textProperty().addListener((obs,before,after) -> refreshProducts());
        searchField.setOnAction(e -> addSearchResult());
        searchField.setOnKeyPressed(e -> { if(e.getCode()==KeyCode.ESCAPE) searchField.clear(); });
        FlowPane categories = new FlowPane(8,8);
        ToggleGroup filters = new ToggleGroup();
        ToggleButton all = categoryButton("All products",null,filters); all.setSelected(true); categories.getChildren().add(all);
        controller.getInventoryItems().stream().map(Item::getCategory).filter(value -> value!=null&&!value.isBlank())
                .distinct().sorted(String.CASE_INSENSITIVE_ORDER).forEach(value -> categories.getChildren().add(categoryButton(value,value,filters)));
        productGrid = new TilePane(12,12); productGrid.setId("product-grid");
        productGrid.setPrefColumns(2); productGrid.setPadding(new Insets(2,4,12,2));
        ScrollPane catalog = new ScrollPane(productGrid); catalog.setFitToWidth(true); catalog.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        catalog.getStyleClass().add("catalog-scroll"); VBox.setVgrow(catalog,Priority.ALWAYS);
        catalog.viewportBoundsProperty().addListener((obs,before,bounds) -> {
            int columns = Math.max(1,(int)(bounds.getWidth()/210));
            productGrid.setPrefTileWidth(Math.max(160,(bounds.getWidth()-6-(columns-1)*12)/columns));
        });
        Label hint = AppTheme.muted("Click to add one  /  Enter to add a search result  /  Ctrl+F to search");
        VBox panel = new VBox(16,heading,searchField,categories,catalog,hint);
        return panel;
    }
    private ToggleButton categoryButton(String text,String value,ToggleGroup filters) {
        ToggleButton button = new ToggleButton(text); button.getStyleClass().add("category-chip"); button.setToggleGroup(filters);
        button.setOnAction(e -> { button.setSelected(true); category=value; refreshProducts(); }); return button;
    }
    private List<Item> matches() {
        String query = searchField == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        return controller.getInventoryItems().stream()
                .filter(item -> category==null || category.equals(item.getCategory()))
                .filter(item -> item.getItemName().toLowerCase(Locale.ROOT).contains(query)
                        || item.getCategory().toLowerCase(Locale.ROOT).contains(query)
                        || item.getBarcode().toLowerCase(Locale.ROOT).contains(query)
                        || item.getSku().toLowerCase(Locale.ROOT).contains(query))
                .sorted(Comparator.comparing(Item::getItemName,String.CASE_INSENSITIVE_ORDER)).toList();
    }
    private int available(Item item) {
        int inCart = controller.getCartItems().stream().filter(line -> line.getSku().equals(item.getSku())).mapToInt(Item::getQuantity).sum();
        return Math.max(0,item.getStock()-inCart);
    }
    private void refreshProducts() {
        if(productGrid==null)return;
        List<Item> results=matches(); productGrid.getChildren().clear();
        productCount.setText(results.size()+ (results.size()==1 ? " product" : " products"));
        if(results.isEmpty()) {
            VBox empty = new VBox(8,label("No products found", "section-title"),AppTheme.muted("Try another search or category."));
            empty.setPadding(new Insets(24)); productGrid.getChildren().add(empty); return;
        }
        for(Item item:results) {
            int remaining=available(item);
            Label badge = label(item.getStock()==0 ? "Out of stock" : remaining==0 ? "All in order" : remaining<=5 ? "Only "+remaining+" left" : remaining+" available", "stock-badge");
            if(remaining<=5 && remaining>0)badge.getStyleClass().add("stock-low");
            if(remaining==0)badge.getStyleClass().add("stock-empty");
            StackPane icon = new StackPane(productIcon(item)); icon.getStyleClass().addAll("product-icon","tint-"+Math.floorMod(item.getCategory().hashCode(),3));
            icon.setMinSize(46,46); icon.setPrefSize(46,46); icon.setMaxSize(46,46);
            Region spacer = new Region(); HBox.setHgrow(spacer,Priority.ALWAYS);
            HBox top = new HBox(8,icon,spacer,badge); top.setAlignment(Pos.TOP_LEFT);
            Label name = label(item.getItemName(),"product-name"); name.setWrapText(true); name.setMinHeight(42);
            Label price = label(Money.format(item.getSellingAmount()),"product-price");
            VBox content = new VBox(10,top,name,AppTheme.muted(item.getCategory()),price);
            content.setMaxWidth(Double.MAX_VALUE);
            Button tile = new Button(); tile.setId("product-"+item.getSku()); tile.setGraphic(content);
            tile.setMaxWidth(Double.MAX_VALUE); tile.setPrefHeight(202); tile.getStyleClass().add("product-tile");
            tile.setDisable(remaining==0);
            tile.setAccessibleText("Add "+item.getItemName()+", "+Money.format(item.getSellingAmount())+", "+badge.getText());
            Tooltip.install(tile,new Tooltip(item.getItemName()+"\n"+badge.getText()));
            tile.setOnAction(e -> addProduct(item)); productGrid.getChildren().add(tile);
        }
    }
    private Node productIcon(Item item) {
        String name=(item.getItemName()+" "+item.getCategory()).toLowerCase(Locale.ROOT);
        String path;
        if(name.contains("headphone")) path="M3 15 V11 A9 9 0 0 1 21 11 V15 M3 13 H6 V21 H3 Z M18 13 H21 V21 H18 Z";
        else if(name.contains("speaker")) path="M5 2 H19 V22 H5 Z M15 7 A3 3 0 1 1 9 7 A3 3 0 1 1 15 7 M16 16 A4 4 0 1 1 8 16 A4 4 0 1 1 16 16";
        else if(name.contains("mouse")) path="M6 11 A6 6 0 0 1 18 11 V17 A6 6 0 0 1 6 17 Z M12 5 V12 M6 12 H18";
        else if(name.contains("phone")) path="M7 2 H17 V22 H7 Z M10 18 H14";
        else if(name.contains("wash") || name.contains("appliance")) path="M3 2 H21 V22 H3 Z M3 6 H21 M6 4 H7 M10 4 H11 M17 14 A5 5 0 1 1 7 14 A5 5 0 1 1 17 14";
        else if(name.contains("laptop") || name.contains("computer")) path="M4 3 H20 V16 H4 Z M1 20 L4 16 H20 L23 20 Z";
        else path="M3 6 L12 2 L21 6 V18 L12 22 L3 18 Z M3 6 L12 10 L21 6 M12 10 V22";
        SVGPath icon = new SVGPath(); icon.setContent(path); icon.getStyleClass().add("product-glyph"); return icon;
    }
    private void addSearchResult() {
        String text=searchField.getText().trim(); if(text.isEmpty())return;
        Item exact=controller.getInventoryItems().stream().filter(item -> item.getBarcode().equalsIgnoreCase(text)
                || item.getSku().equalsIgnoreCase(text) || item.getItemName().equalsIgnoreCase(text)).findFirst().orElse(null);
        List<Item> results=matches();
        if(exact==null && results.size()==1)exact=results.get(0);
        if(exact==null) { notice("Choose a product below, or enter its exact barcode.",true); return; }
        if(addProduct(exact))searchField.clear();
    }
    private boolean addProduct(Item item) {
        try {
            controller.addItemToBill(item.getItemName(),1); refreshOrder(); refreshProducts(); return true;
        } catch(RuntimeException e) { notice(e.getMessage(),true); return false; }
    }

    private VBox orderPanel() {
        itemCount = AppTheme.muted("0 items");
        Region space = new Region(); HBox.setHgrow(space,Priority.ALWAYS);
        HBox heading = new HBox(label("Current order","section-title"),space,itemCount); heading.setAlignment(Pos.CENTER_LEFT);
        cart = new ListView<>(cartItems); cart.setId("order-list"); cart.getStyleClass().add("order-list");
        VBox empty = new VBox(12,label("Your order starts here","section-title"),AppTheme.muted("Add a product from the left.\nScan a barcode for a faster checkout."));
        empty.setAlignment(Pos.CENTER); empty.setPadding(new Insets(24)); cart.setPlaceholder(empty);
        cart.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(Item item,boolean empty) {
                super.updateItem(item,empty); setText(null); setGraphic(null);
                if(empty||item==null)return;
                Label name=label(item.getItemName(),"order-name"); name.setWrapText(true); name.setMaxWidth(Double.MAX_VALUE);
                Label subtotal=label(Money.format(item.getSellingAmount().multiply(BigDecimal.valueOf(item.getQuantity()))),"order-subtotal");
                VBox description=new VBox(5,name,AppTheme.muted(Money.format(item.getSellingAmount())+" each")); description.setMinWidth(0); HBox.setHgrow(description,Priority.ALWAYS);
                HBox first=new HBox(12,description,subtotal); first.setAlignment(Pos.TOP_LEFT);
                Button minus=quantityButton("-","Decrease quantity of "+item.getItemName()); minus.setId("decrease-"+item.getSku());
                Button plus=quantityButton("+","Increase quantity of "+item.getItemName()); plus.setId("increase-"+item.getSku());
                Item stock=controller.getInventoryItems().stream().filter(product -> product.getSku().equals(item.getSku())).findFirst().orElse(null);
                plus.setDisable(stock==null||item.getQuantity()>=stock.getStock());
                minus.setOnAction(e -> changeQuantity(item,item.getQuantity()-1)); plus.setOnAction(e -> changeQuantity(item,item.getQuantity()+1));
                Label quantity=label(String.valueOf(item.getQuantity()),"quantity-value"); quantity.setMinWidth(30); quantity.setAlignment(Pos.CENTER);
                HBox stepper=new HBox(0,minus,quantity,plus); stepper.setAlignment(Pos.CENTER_LEFT); stepper.getStyleClass().add("quantity-stepper");
                Region gap=new Region(); HBox.setHgrow(gap,Priority.ALWAYS);
                Button remove=new Button("Remove"); remove.getStyleClass().add("remove-line"); remove.setId("remove-"+item.getSku());
                remove.setAccessibleText("Remove "+item.getItemName()+" from order"); remove.setOnAction(e -> changeQuantity(item,0));
                HBox controls=new HBox(stepper,gap,remove); controls.setAlignment(Pos.CENTER_LEFT);
                VBox line=new VBox(14,first,controls); line.setPadding(new Insets(16,4,16,4)); line.prefWidthProperty().bind(cart.widthProperty().subtract(32));
                setGraphic(line);
            }
        });
        VBox.setVgrow(cart,Priority.ALWAYS);
        totalLabel=label("$0.00","order-total");
        Region fill=new Region(); HBox.setHgrow(fill,Priority.ALWAYS);
        HBox total=new HBox(label("Total","section-title"),fill,totalLabel); total.setAlignment(Pos.CENTER_LEFT);
        completeButton=AppTheme.primary(new Button()); completeButton.setId("complete-sale"); completeButton.getStyleClass().add("complete-sale");
        completeButton.setMaxWidth(Double.MAX_VALUE); completeButton.setOnAction(e -> finalizeBill());
        cancelButton=new Button("Cancel order"); cancelButton.getStyleClass().add("quiet-button"); cancelButton.setOnAction(e -> cancelOrder());
        VBox footer=new VBox(14,total,completeButton,cancelButton); footer.getStyleClass().add("order-footer"); footer.setAlignment(Pos.CENTER);
        VBox panel=new VBox(12,heading,cart,footer); panel.getStyleClass().add("order-panel"); panel.setPadding(new Insets(22)); return panel;
    }
    private Button quantityButton(String text,String accessible) {
        Button button=new Button(text); button.getStyleClass().add("quantity-button"); button.setAccessibleText(accessible); return button;
    }
    private void changeQuantity(Item item,int quantity) {
        try { controller.updateCartQuantity(item.getSku(),quantity); refreshOrder(); refreshProducts(); }
        catch(RuntimeException e) { notice(e.getMessage(),true); }
    }
    private void refreshOrder() {
        cartItems.setAll(controller.getCartItems()); cart.refresh();
        int count=controller.getCartItems().stream().mapToInt(Item::getQuantity).sum();
        itemCount.setText(count+(count==1 ? " item" : " items"));
        String amount=Money.format(Money.of(controller.getTotalAmount())); totalLabel.setText(amount);
        completeButton.setText("Complete sale  /  "+amount); completeButton.setDisable(count==0); cancelButton.setDisable(count==0);
    }
    private void finalizeBill() {
        String amount=Money.format(Money.of(controller.getTotalAmount()));
        try {
            controller.finalizeBill(); searchField.clear(); refreshOrder(); refreshProducts(); loadTodaysBills();
            notice("Sale completed for "+amount+". Ready for the next order.",false); searchField.requestFocus();
        } catch(RuntimeException e) { notice(e.getMessage(),true); }
    }
    private boolean confirmDiscard() {
        Alert confirm=new Alert(Alert.AlertType.CONFIRMATION,"Discard the current order?",ButtonType.OK,ButtonType.CANCEL);
        confirm.setHeaderText("Cancel order"); AppTheme.dialog(confirm); return confirm.showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK;
    }
    private void cancelOrder() {
        if(confirmDiscard()) { controller.cancelBill(); refreshOrder(); refreshProducts(); searchField.requestFocus(); }
    }
    private void logout() {
        if(!controller.getCartItems().isEmpty()&&!confirmDiscard())return;
        controller.cancelBill();
        if(navigator!=null)navigator.logout();
        else { Util.Session.clear(); new AppNavigator(stage).navigateToLogin(); }
    }
    private void notice(String text,boolean error) {
        status.setText(text==null ? "Unable to complete this action." : text);
        status.getStyleClass().setAll("checkout-notice",error ? "notice-error" : "notice-success");
        status.setVisible(true); status.setManaged(true); noticeTimeout.playFromStart();
    }
    private Label label(String text,String style) { Label label=new Label(text); label.getStyleClass().add(style); return label; }

    private VBox historyPanel() {
        salesLabel=label("","section-title"); billTable=new TableView<>(); billTable.setId("sales-history");
        String[] headings={"Bill","Time","Total","Status"};
        for(int i=0;i<headings.length;i++) {
            final int index=i; TableColumn<Bill,String> column=new TableColumn<>(headings[i]);
            column.setCellValueFactory(cell -> {
                Bill b=cell.getValue();
                return new SimpleStringProperty(switch(index) {
                    case 0 -> b.getBillNumber();
                    case 1 -> new java.text.SimpleDateFormat("HH:mm").format(b.getDate());
                    case 2 -> Money.format(b.getNetAmount());
                    default -> b.isFullyRefunded() ? "Refunded" : b.getRefundedAmount()>0 ? "Partial return" : "Completed";
                });
            }); billTable.getColumns().add(column);
        }
        AppTheme.table(billTable,"No completed sales today."); VBox.setVgrow(billTable,Priority.ALWAYS);
        Button returns=new Button("Process return"); returns.setOnAction(e -> showReturnDialog());
        Button refresh=new Button("Refresh"); refresh.setOnAction(e -> loadTodaysBills());
        HBox actions=new HBox(10,returns,refresh);
        VBox panel=new VBox(18,AppTheme.title("Sales today"),salesLabel,billTable,actions); return panel;
    }
    private void loadTodaysBills() {
        if(billTable==null)return;
        billTable.setItems(FXCollections.observableArrayList(controller.getTodaysBills()));
        salesLabel.setText("Net sales: "+Money.format(Money.of(controller.getTotalOfTodaysBills())));
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
                    notice("Return processed. Stock and sales have been updated.", false);
                    refreshProducts();
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

    private void showAlert(String title,String message) { notice(message,!title.equals("Success")); }
}
