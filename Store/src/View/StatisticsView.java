package View;

import Controller.ManagerController;
import Model.*;
import Util.*;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import java.time.*;
import java.util.*;

public class StatisticsView {
    private final Stage stage;
    private final ManagerController controller;
    private final Runnable onBack;
    private DatePicker start, end;
    private VBox metrics;
    private TableView<List<String>> sales, products, lowStock, movements;
    private TabPane tabs;
    private List<List<String>> salesRows, productRows, stockRows, movementRows;
    public StatisticsView(Stage stage, ManagerController controller) {
        this(stage,controller,() -> new ManagerDashboardView(stage,controller).start(stage));
    }
    public StatisticsView(Stage stage, ManagerController controller, Runnable onBack) {
        this.stage=stage; this.controller=controller; this.onBack=onBack;
    }
    public void show() {
        Session.require(Role.MANAGER,Role.ADMIN);
        start=new DatePicker(LocalDate.now().withDayOfMonth(1)); end=new DatePicker(LocalDate.now());
        Button apply=new Button("Apply dates"); apply.setOnAction(e -> refresh());
        Button export=AppTheme.primary(new Button("Export selected report")); export.setOnAction(e -> export());
        Button back=new Button("Back to Dashboard"); back.setOnAction(e -> onBack.run());
        HBox filters=new HBox(12,AppTheme.field("From",start),AppTheme.field("Through",end),apply,export);
        filters.setAlignment(javafx.geometry.Pos.BOTTOM_LEFT);
        metrics=new VBox();
        sales=table("Bill","Date","Net units","Net revenue","Net profit","Refunded");
        products=table("SKU","Product","Net units","Net revenue","Net profit");
        lowStock=table("SKU","Product","Stock","Supplier");
        movements=table("When","SKU","Product","Change","Reason","User");
        tabs=new TabPane(tab("Sales",sales),tab("Best sellers",products),tab("Low stock (current)",lowStock),tab("Stock history",movements));
        VBox layout=new VBox(22,AppTheme.header("The shop ledger","Sales & stock for your chosen dates. Low stock always shows today?s quantities.",back),filters,metrics,tabs);
        layout.setPadding(new javafx.geometry.Insets(28)); VBox.setVgrow(tabs,Priority.ALWAYS);
        Scene scene=new Scene(layout,1200,820); AppTheme.apply(scene,stage); stage.setMinWidth(1050);
        stage.setScene(scene); stage.setTitle("Manager Statistics"); refresh(); stage.show();
    }
    private Tab tab(String text,TableView<?> table) { Tab tab=new Tab(text,table); tab.setClosable(false); return tab; }
    private TableView<List<String>> table(String... titles) {
        TableView<List<String>> table=new TableView<>();
        for(int i=0;i<titles.length;i++) {
            final int index=i;
            TableColumn<List<String>,String> column=new TableColumn<>(titles[i]);
            column.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().get(index)));
            table.getColumns().add(column);
        }
        AppTheme.table(table,"No records for this report."); return table;
    }
    private List<List<String>> rows(String... heading) { List<List<String>> rows=new ArrayList<>(); rows.add(List.of(heading)); return rows; }
    private void refresh() {
        try {
            List<Bill> bills=Reports.inPeriod(controller.getAllBills(),start.getValue(),end.getValue());
            Reports.Summary summary=Reports.summarize(bills);
            HBox cards=new HBox(16);
            String[] labels={"Bills","Net units","Net revenue","Net profit"};
            String[] values={""+summary.bills(),""+summary.units(),Money.format(summary.revenue()),Money.format(summary.profit())};
            for(int i=0;i<labels.length;i++) {
                Label value=new Label(values[i]); value.getStyleClass().add("metric");
                VBox card=new VBox(8,AppTheme.muted(labels[i]),value); card.getStyleClass().add("report-metric"); HBox.setHgrow(card,Priority.ALWAYS); card.setMaxWidth(Double.MAX_VALUE); cards.getChildren().add(card);
            }
            metrics.getChildren().setAll(cards);
            salesRows=rows("Bill","Date","Net units","Net revenue","Net profit","Refunded");
            for(Bill b:bills) salesRows.add(List.of(b.getBillNumber(),b.getDate().toInstant().toString(),""+b.getTotalItemsSold(),b.getNetAmount().toPlainString(),b.getProfit().toPlainString(),Money.of(b.getRefundedAmount()).toPlainString()));
            productRows=rows("SKU","Product","Net units","Net revenue","Net profit");
            for(Reports.Product product:Reports.bestSellers(bills)) productRows.add(List.of(product.sku(),product.name(),""+product.units(),product.revenue().toPlainString(),product.profit().toPlainString()));
            stockRows=rows("SKU","Product","Stock","Supplier");
            for(Item item:controller.getAllItems()) if(item.getStock()<=5) stockRows.add(List.of(item.getSku(),item.getItemName(),""+item.getStock(),item.getSupplier()));
            movementRows=rows("When","SKU","Product","Change","Reason","User");
            for(List<String> row:DAO.StoreDatabase.movements()) {
                LocalDate date=Instant.parse(row.get(0)).atZone(ZoneId.systemDefault()).toLocalDate();
                if(!date.isBefore(start.getValue())&&!date.isAfter(end.getValue())) movementRows.add(row);
            }
            sales.setItems(FXCollections.observableArrayList(salesRows.subList(1,salesRows.size())));
            products.setItems(FXCollections.observableArrayList(productRows.subList(1,productRows.size())));
            lowStock.setItems(FXCollections.observableArrayList(stockRows.subList(1,stockRows.size())));
            movements.setItems(FXCollections.observableArrayList(movementRows.subList(1,movementRows.size())));
        } catch(RuntimeException e) { alert(e.getMessage()); }
    }
    private void export() {
        if(salesRows==null)return;
        FileChooser chooser=new FileChooser(); chooser.setTitle("Export report"); chooser.setInitialFileName("store-report.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files","*.csv"));
        java.io.File destination=chooser.showSaveDialog(stage); if(destination==null)return;
        try { Reports.export(destination.toPath(),switch(tabs.getSelectionModel().getSelectedIndex()) {
            case 1 -> productRows; case 2 -> stockRows; case 3 -> movementRows; default -> salesRows;
        }); alert("Report exported to " + destination); } catch(Exception e) { alert(e.getMessage()); }
    }
    private void alert(String message) { Alert alert=new Alert(Alert.AlertType.INFORMATION,message); AppTheme.dialog(alert); alert.showAndWait(); }
}
