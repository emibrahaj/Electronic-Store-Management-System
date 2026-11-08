package View;

import Model.Bill;
import Model.Item;
import Util.Money;
import Util.Reports;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/** Shared, read-only overview of the store's current sales and inventory. */
public final class StoreOverview {
    private StoreOverview() { }

    public static VBox create(List<Bill> bills, List<Item> items, Button... actions) {
        LocalDate today = LocalDate.now();
        Reports.Summary summary = Reports.summarize(Reports.inPeriod(bills, today, today));
        List<Item> lowStock = items.stream().filter(item -> item.getStock() <= 5)
                .sorted(Comparator.comparingInt(Item::getStock).thenComparing(Item::getItemName)).toList();
        GridPane metrics = new GridPane(); metrics.setHgap(12);
        for (int i = 0; i < 4; i++) {
            ColumnConstraints column = new ColumnConstraints(); column.setPercentWidth(25);
            column.setHgrow(Priority.ALWAYS); metrics.getColumnConstraints().add(column);
        }
        metrics.addRow(0, metric("Net sales today", Money.format(summary.revenue()), "After returns"),
                metric("Sales today", String.valueOf(summary.bills()), "Completed transactions"),
                metric("Products", String.valueOf(items.size()), "In your catalog"),
                metric("Needs restocking", String.valueOf(lowStock.size()), "Products with 5 or fewer units"));

        CategoryAxis days = new CategoryAxis();
        NumberAxis revenue = new NumberAxis();
        revenue.setLabel("Net sales ($)");
        LineChart<String, Number> chart = new LineChart<>(days, revenue);
        chart.setId("weekly-sales-chart");
        chart.setLegendVisible(false); chart.setAnimated(false);
        chart.setMinHeight(220); chart.setPrefHeight(250);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (int offset = 6; offset >= 0; offset--) {
            LocalDate date = today.minusDays(offset);
            series.getData().add(new XYChart.Data<>(date.format(DateTimeFormatter.ofPattern("dd MMM", java.util.Locale.ENGLISH)),
                    Reports.summarize(Reports.inPeriod(bills, date, date)).revenue()));
        }
        chart.getData().add(series);
        VBox trend = AppTheme.card(section("Sales this week"), AppTheme.muted("Daily net sales, including today. Returns adjust the original sale date."), chart);
        VBox stock = AppTheme.card(section("Stock watch"));
        if (lowStock.isEmpty()) {
            stock.getChildren().add(AppTheme.muted(items.isEmpty()
                    ? "Your catalog is empty. Add your first product to get started."
                    : "Stock looks healthy. All products have more than 5 units."));
        } else {
            for (Item item : lowStock.stream().limit(4).toList()) {
                Label name = new Label(item.getItemName()); name.setWrapText(true); name.setMinWidth(0);
                HBox.setHgrow(name, Priority.ALWAYS); name.setMaxWidth(Double.MAX_VALUE);
                Label badge = new Label(item.getStock() == 0 ? "Out of stock" : item.getStock() + " left");
                badge.getStyleClass().addAll("stock-badge", "stock-low"); badge.setMinWidth(Region.USE_PREF_SIZE);
                stock.getChildren().add(new HBox(12, name, badge));
            }
            stock.getChildren().add(AppTheme.muted(lowStock.size() + " products need attention. Open inventory or reports for the full list."));
        }
        FlowPane shortcuts = new FlowPane(10, 10); shortcuts.getChildren().addAll(actions);
        VBox result = new VBox(18, AppTheme.title("Store overview"),
                AppTheme.muted("A quick look at your store, with the next steps close at hand."), metrics, shortcuts, trend, stock);
        result.setId("store-overview");
        return result;
    }

    private static Label section(String text) {
        Label label = new Label(text); label.getStyleClass().add("section-title"); return label;
    }

    private static VBox metric(String title, String value, String hint) {
        Label number = new Label(value); number.getStyleClass().add("metric");
        VBox card = AppTheme.card(AppTheme.muted(title), number, AppTheme.muted(hint));
        card.setMinWidth(0); card.setMaxWidth(Double.MAX_VALUE); return card;
    }
}
