package View;

import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/** Shared presentation helpers for every screen and dialog. */
public final class AppTheme {
    private AppTheme() { }
    public static void apply(Scene scene, Stage stage) {
        scene.getStylesheets().add(AppTheme.class.getResource("theme.css").toExternalForm());
        stage.setMinWidth(760);
        stage.setMinHeight(640);
    }
    public static void dialog(Dialog<?> dialog) {
        dialog.getDialogPane().getStylesheets().add(AppTheme.class.getResource("theme.css").toExternalForm());
    }
    public static Label title(String text) {
        Label label = new Label(text); label.getStyleClass().add("page-title"); return label;
    }
    public static Label muted(String text) {
        Label label = new Label(text); label.getStyleClass().add("muted"); label.setWrapText(true); return label;
    }
    public static VBox field(String text, Node control) {
        Label label = new Label(text); label.getStyleClass().add("field-label");
        if (control instanceof Region region) region.setMaxWidth(Double.MAX_VALUE);
        return new VBox(6, label, control);
    }
    public static VBox card(Node... children) {
        VBox box = new VBox(14, children); box.getStyleClass().add("card"); return box;
    }
    public static HBox header(String title, String subtitle, Node... actions) {
        VBox heading = new VBox(5, title(title), muted(subtitle));
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(12, heading, spacer);
        header.getChildren().addAll(actions); header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return header;
    }
    public static void table(TableView<?> table, String emptyMessage) {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(muted(emptyMessage)); table.setMinHeight(180);
    }
    public static Button primary(Button button) { button.getStyleClass().add("primary"); return button; }
    public static Button danger(Button button) { button.getStyleClass().add("danger"); return button; }
}
