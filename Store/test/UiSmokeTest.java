import Controller.*;
import Model.*;
import Util.*;
import View.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import java.nio.file.*;

/** Opens each main scene against an isolated empty store, then exits. */
public class UiSmokeTest extends Application {
    private static Throwable failure;
    private static int checkoutChecks;
    private void check(boolean condition,String message) { if(!condition)throw new AssertionError(message); checkoutChecks++; }
    private javafx.scene.control.Button button(Stage stage,String id) {
        stage.getScene().getRoot().applyCss(); stage.getScene().getRoot().layout();
        return (javafx.scene.control.Button)stage.getScene().lookup("#"+id);
    }
    public static void main(String[] args) throws Exception {
        Path home = Files.createTempDirectory(Path.of(".build"), "ui-smoke-").toAbsolutePath();
        System.setProperty("store.home", home.toString());
        launch(args);
        if (failure != null) throw new AssertionError("UI smoke test failed", failure);
        System.out.println("PASS: login, admin, manager, statistics, cashier scenes; " + checkoutChecks + " checkout interaction checks");
    }
    private void capture(Stage stage, String name) throws Exception {
        stage.getScene().getRoot().applyCss();
        stage.getScene().getRoot().layout();
        javafx.scene.image.WritableImage image = stage.getScene().snapshot(null);
        java.awt.image.BufferedImage output = new java.awt.image.BufferedImage((int) image.getWidth(), (int) image.getHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < output.getHeight(); y++) for (int x = 0; x < output.getWidth(); x++)
            output.setRGB(x, y, image.getPixelReader().getArgb(x, y));
        Path folder = Path.of(".build/ui-preview"); Files.createDirectories(folder);
        javax.imageio.ImageIO.write(output, "png", folder.resolve(name + ".png").toFile());
    }
    @Override public void start(Stage stage) {
        try {
            AppNavigator navigator = new AppNavigator(stage);
            navigator.navigateToLogin();
            if (!stage.getTitle().equals("Login")) throw new AssertionError("Login scene failed");
            capture(stage, "login");
            capture(stage,"first-run-setup");
            AdminController.createFirstAdmin("owner","secret123","Owner","o@example.com","0123456789");
            AuthService.authenticate("owner","secret123");
            navigator.navigateToLogin(); capture(stage,"login");
            new AdminDashboardView().show(stage);
            capture(stage, "admin");
            Manager manager = new Manager("manager", "password", "Manager", "m@example.com", "0123456789");
            ManagerController controller = new ManagerController(manager);
            Item laptop=new Item("Laptop","Computers","Tech Supply",500,699.99,12); laptop.setBarcode("1234567890");
            manager.addItemToInventory(laptop,"sales@example.com");
            manager.addItemToInventory(new Item("USB-C cable","Accessories","Tech Supply",4,9.99,20),"sales@example.com");
            manager.addItemToInventory(new Item("Wireless mouse","Accessories","Tech Supply",12,24.99,3),"sales@example.com");
            manager.addItemToInventory(new Item("Bluetooth speaker","Audio","Tech Supply",25,49.99,1),"sales@example.com");
            manager.addItemToInventory(new Item("Smartphone","Phones","Tech Supply",320,499,8),"sales@example.com");
            manager.addItemToInventory(new Item("Headphones","Audio","Tech Supply",40,79.99,9),"sales@example.com");
            new ManagerDashboardView(stage, controller).start(stage);
            capture(stage, "manager");
            new StatisticsView(stage, controller).show();
            capture(stage, "statistics");
            SharedContext.getInstance().getInventory().findItemByName("Bluetooth speaker").setStock(0);
            Cashier cashier = new Cashier("cashier", "password", "Cashier", "c@example.com", "0123456789", "Sales", SharedContext.getInstance().getInventory());
            CashierController checkout=new CashierController(cashier,SharedContext.getInstance().getInventory());
            checkout.addItemToBill("Laptop",1); checkout.finalizeBill();
            checkout.addItemToBill("USB-C cable",2);
            new CashierDashboardView(stage, checkout).start();
            capture(stage, "cashier");
            Item cable=SharedContext.getInstance().getInventory().findItemByName("USB-C cable");
            Item soldOut=SharedContext.getInstance().getInventory().findItemByName("Bluetooth speaker");
            check(button(stage,"product-"+soldOut.getSku()).isDisabled(),"Out-of-stock tile is disabled");
            button(stage,"product-"+laptop.getSku()).fire();
            check(checkout.getCartItems().stream().anyMatch(item -> item.getSku().equals(laptop.getSku())&&item.getQuantity()==1),"Tile click adds one");
            button(stage,"increase-"+laptop.getSku()).fire();
            check(checkout.getCartItems().stream().filter(item -> item.getSku().equals(laptop.getSku())).findFirst().orElseThrow().getQuantity()==2,"Inline plus adds one");
            button(stage,"decrease-"+laptop.getSku()).fire();
            check(checkout.getCartItems().stream().filter(item -> item.getSku().equals(laptop.getSku())).findFirst().orElseThrow().getQuantity()==1,"Inline minus subtracts one");
            button(stage,"remove-"+laptop.getSku()).fire();
            check(checkout.getCartItems().size()==1,"Inline remove removes the line");
            javafx.scene.control.TextField search=(javafx.scene.control.TextField)stage.getScene().lookup("#product-search");
            search.setText("1234567890"); search.fireEvent(new javafx.event.ActionEvent());
            check(search.getText().isEmpty()&&checkout.getCartItems().size()==2,"Combined field scans barcode and clears");
            search.setText("not-a-product"); search.fireEvent(new javafx.event.ActionEvent());
            check(stage.getScene().lookup("#checkout-notice").isVisible(),"Invalid search shows inline feedback"); search.clear();
            button(stage,"complete-sale").fire();
            check(checkout.getCartItems().isEmpty(),"Complete sale clears the cart");
            check(button(stage,"complete-sale").isDisabled(),"Empty cart disables completion");
            check(stage.getScene().lookup("#checkout-notice").isVisible(),"Success banner replaces modal popup");
            capture(stage,"checkout-empty");
            ((javafx.scene.control.ToggleButton)stage.getScene().lookup("#history-tab")).fire();
            javafx.scene.control.TableView<?> history=(javafx.scene.control.TableView<?>)stage.getScene().lookup("#sales-history");
            check(history.getItems().size()==2,"Sales tab shows both completed transactions");
            capture(stage,"checkout-history");
            ((javafx.scene.control.ToggleButton)stage.getScene().lookup("#checkout-tab")).fire();
            check(stage.getScene().lookup("#product-search")!=null,"Return to checkout preserves the screen");
            search.setText("1234567890"); search.fireEvent(new javafx.event.ActionEvent());
            checkout.updateCartQuantity(laptop.getSku(),laptop.getStock());
            search.setText("1234567890"); search.fireEvent(new javafx.event.ActionEvent());
            check(checkout.getCartItems().get(0).getQuantity()==laptop.getStock(),"Scanner cannot exceed available stock");

            stage.getScene().getRoot().applyCss();
            stage.getScene().getRoot().layout();
        } catch (Throwable e) { failure = e; }
        finally { stage.close(); Platform.exit(); }
    }
}
