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
    public static void main(String[] args) throws Exception {
        Path home = Files.createTempDirectory(Path.of(".build"), "ui-smoke-").toAbsolutePath();
        System.setProperty("store.home", home.toString());
        launch(args);
        if (failure != null) throw new AssertionError("UI smoke test failed", failure);
        System.out.println("PASS: login, admin, manager, statistics, cashier scenes");
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
            new ManagerDashboardView(stage, controller).start(stage);
            capture(stage, "manager");
            new StatisticsView(stage, controller).show();
            capture(stage, "statistics");
            Cashier cashier = new Cashier("cashier", "password", "Cashier", "c@example.com", "0123456789", "Sales", SharedContext.getInstance().getInventory());
            CashierController checkout=new CashierController(cashier,SharedContext.getInstance().getInventory());
            checkout.addItemToBill("Laptop",1); checkout.finalizeBill();
            checkout.addItemToBill("USB-C cable",2);
            new CashierDashboardView(stage, checkout).start();
            capture(stage, "cashier");
            stage.getScene().getRoot().applyCss();
            stage.getScene().getRoot().layout();
        } catch (Throwable e) { failure = e; }
        finally { stage.close(); Platform.exit(); }
    }
}
