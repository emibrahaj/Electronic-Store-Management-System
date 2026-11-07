import Controller.AdminController;
import Controller.CashierController;
import DAO.*;
import Model.*;
import Util.*;
import java.sql.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;

/** Run against an isolated store.home; never touches application data. */
public class RegressionTests {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static void rejected(Runnable action) {
        try { action.run(); throw new AssertionError("Expected rejection"); }
        catch (IllegalArgumentException expected) { checks++; }
    }
    public static void main(String[] args) throws Exception {
        Path home = Files.createTempDirectory(Path.of(".build"), "regression-").toAbsolutePath();
        System.setProperty("store.home", home.toString());
        Inventory inventory = new Inventory();
        Item item = new Item("Laptop", "Electronics", "Supplier", 50, 100, 10);
        inventory.addItem(item);
        // Seed a legacy store: the first read must import it into SQLite once.
        item.setBarcode("1234567890");
        Files.createDirectories(AppPaths.daoFile("inventory.dat").getParent());
        AtomicFiles.writeObject(AppPaths.daoFile("inventory.dat"),inventory);
        byte[] legacyBytes=Files.readAllBytes(AppPaths.daoFile("inventory.dat"));
        check(AuthService.needsSetup(), "Fresh store needs administrator setup");
        AdminController.createFirstAdmin("owner", "secret123", "Owner", "owner@example.com", "0123456789");
        check(!AuthService.needsSetup(), "Setup completed");
        rejected(() -> AuthService.authenticate("owner", "wrong"));
        AuthService.authenticate("owner", "secret123");
        AdminController accountAdmin=new AdminController();
        accountAdmin.addEmployee("cashierUser","cashierPass","Cashier","c@example.com","0123456789",Role.CASHIER,"Sales");
        accountAdmin.addEmployee("managerUser","managerPass","Manager","m@example.com","0123456789",Role.MANAGER,"Inventory");
        boolean setupRejected=false;
        try { AdminController.createFirstAdmin("other","secret123","Other","other@example.com","0123456789"); }
        catch(IllegalStateException expected) { setupRejected=true; }
        check(setupRejected,"Cannot bootstrap a second administrator");
        AuthService.authenticate("cashierUser","cashierPass");
        try { accountAdmin.deleteEmployee(accountAdmin.findEmployee("owner")); throw new AssertionError("Unauthorized deletion"); }
        catch(SecurityException expected) { checks++; }
        InventoryDAO.loadInventory();
        check(Files.exists(StoreDatabase.path()),"SQLite created");
        check(Arrays.equals(legacyBytes,Files.readAllBytes(AppPaths.daoFile("inventory.dat"))),"Legacy file unchanged");

        SharedContext context = SharedContext.getInstance();
        check(context.getInventory().findItemByName("Laptop").getStock() == 10, "Load persisted inventory");
        inventory = context.getInventory();
        item = inventory.findItemByName("Laptop");
        Cashier cashier = new Cashier("cashier", "password", "Cashier", "c@example.com", "0123456789", "Sales", inventory);
        CashierController controller = new CashierController(cashier, inventory);
        rejected(() -> controller.addItemToBill("Laptop", -1));
        rejected(() -> controller.addItemToBill("Laptop", 0));
        rejected(() -> controller.addItemToBill("missing", 1));
        controller.addItemToBill("Laptop", 2);
        controller.addItemToBill("Laptop", 3);
        check(cashier.getAllBills().isEmpty(), "Draft excluded from completed sales");
        check(controller.getTotalAmount() == 500, "Repeated item total");
        rejected(() -> controller.addItemToBill("Laptop", 6));
        rejected(() -> context.getInventory().updateItem("Laptop", Double.NaN, 100, 1));
        rejected(() -> context.getInventory().updateItem("Laptop", 50, 100, -1));
        controller.updateCartQuantity(item.getSku(),4);
        check(controller.getTotalAmount()==400,"Cart quantity edit");
        controller.updateCartQuantity(item.getSku(),5);
        int movementsBefore=StoreDatabase.movements().size();
        try(Connection db=DriverManager.getConnection("jdbc:sqlite:"+StoreDatabase.path()); Statement sql=db.createStatement()) {
            sql.execute("CREATE TRIGGER reject_sale BEFORE INSERT ON bills BEGIN SELECT RAISE(ABORT,'test failure'); END");
        }
        try { controller.finalizeBill(); throw new AssertionError("Failed write accepted"); }
        catch(DataPersistenceException expected) { checks++; }
        check(item.getStock()==10,"Failed sale restores in-memory stock");
        check(InventoryDAO.loadInventory().findBySku(item.getSku()).getStock()==10,"Failed sale rolls back stored stock");
        check(BillDAO.loadBills().isEmpty(),"Failed sale rolls back bill");
        check(StoreDatabase.movements().size()==movementsBefore,"Failed sale rolls back stock movements");
        check(controller.getTotalAmount()==500,"Failed sale retains cart for retry");
        try(Connection db=DriverManager.getConnection("jdbc:sqlite:"+StoreDatabase.path()); Statement sql=db.createStatement()) { sql.execute("DROP TRIGGER reject_sale"); }
        controller.finalizeBill();
        Bill bill = cashier.getAllBills().get(0);
        check(item.getStock() == 5, "Deduct stock once");
        item.setSellingPrice(150);
        check(bill.getItems().get(0).getSellingPrice() == 100, "Historical sale price is immutable from inventory");
        check(cashier.processReturn(bill.getBillNumber(), "Laptop", 2), "Partial return");
        check(item.getStock() == 7 && bill.getTotalAmount() == 300, "Return stock and value");
        check(!cashier.processReturn(bill.getBillNumber(), "Laptop", 4), "Reject excess return");
        try(Connection db=DriverManager.getConnection("jdbc:sqlite:"+StoreDatabase.path()); Statement sql=db.createStatement()) {
            sql.execute("CREATE TRIGGER reject_return BEFORE INSERT ON bills BEGIN SELECT RAISE(ABORT,'return failure'); END");
        }
        try { cashier.processReturn(bill.getBillNumber(),"Laptop",1); throw new AssertionError("Failed refund accepted"); }
        catch(DataPersistenceException expected) { checks++; }
        check(item.getStock()==7 && bill.getTotalAmount()==300,"Failed return restores memory");
        check(InventoryDAO.loadInventory().findBySku(item.getSku()).getStock()==7 && BillDAO.loadBills().get(0).getTotalAmount()==300,"Failed return restores database");
        try(Connection db=DriverManager.getConnection("jdbc:sqlite:"+StoreDatabase.path()); Statement sql=db.createStatement()) { sql.execute("DROP TRIGGER reject_return"); }

        check(cashier.processReturn(bill.getBillNumber(), "Laptop", 3), "Full return");
        check(item.getStock() == 10 && bill.isFullyRefunded(), "Full return state");
        rejected(controller::finalizeBill);
        check(InventoryDAO.loadInventory().findItemByName("Laptop").getStock() == 10, "Stock round trip");
        check(BillDAO.loadBills().get(0).isFullyRefunded(), "Bill round trip");
        controller.scanBarcode("1234567890",1);
        check(controller.getCartItems().size()==1,"Barcode adds to cart");
        controller.cancelBill(); check(controller.getCartItems().isEmpty(),"Cancel clears draft without stock change");
        Item decimal=new Item("Cable","Electronics","Supplier",0.10,0.30,10);
        context.getInventory().addItem(decimal);
        controller.addItemToBill("Cable",3);
        check(cashier.getCurrentBill().getNetAmount().equals(new BigDecimal("0.90")),"Exact decimal total");
        check(cashier.getCurrentBill().getProfit().equals(new BigDecimal("0.60")),"Exact profit");
        controller.finalizeBill();
        Reports.Summary summary=Reports.summarize(cashier.getAllBills());
        check(summary.revenue().equals(new BigDecimal("0.90")),"Reports use net decimal revenue");
        check(Reports.bestSellers(cashier.getAllBills()).get(0).name().equals("Cable"),"Best sellers aggregate products");
        check(Reports.inPeriod(cashier.getAllBills(),java.time.LocalDate.now().minusDays(2),java.time.LocalDate.now().minusDays(1)).isEmpty(),"Date filtering");
        Path csv=home.resolve("report.csv"); Reports.export(csv,List.of(List.of("a,b","quote\"","=SUM(A1)","normal")));
        String csvText=Files.readString(csv);
        check(csvText.contains("\"a,b\"")&&csvText.contains("quote\"\"")&&csvText.contains("'=SUM"),"CSV escaping and text protection");
        String sku=decimal.getSku();
        check(InventoryDAO.loadInventory().findBySku(sku)!=null,"SKU survives reload");
        check(InventoryDAO.loadInventory().findByBarcode("1234567890")!=null,"Barcode survives reload");
        check(Files.readAllBytes(AppPaths.daoFile("inventory.dat")).length==legacyBytes.length,"No legacy rewrites after sales");
        AuthService.authenticate("managerUser","managerPass");
        Manager manager=new Manager("managerUser","","Manager","m@example.com","0123456789");
        rejected(() -> manager.addItemToInventory(new Item("Laptop","Electronics","Supplier",50,100,1),"contact"));
        try { controller.finalizeBill(); throw new AssertionError("Manager checkout accepted"); }
        catch(SecurityException expected) { checks++; }
        int stockBefore=context.getInventory().findItemByName("Laptop").getStock();
        try(Connection db=DriverManager.getConnection("jdbc:sqlite:"+StoreDatabase.path()); Statement sql=db.createStatement()) {
            sql.execute("CREATE TRIGGER reject_inventory BEFORE INSERT ON products BEGIN SELECT RAISE(ABORT,'inventory failure'); END");
        }
        try { manager.updateItemInInventory("Laptop",50,100,99); throw new AssertionError("Failed inventory change accepted"); }
        catch(DataPersistenceException expected) { checks++; }
        check(context.getInventory().findItemByName("Laptop").getStock()==stockBefore,"Failed manager change restores memory");
        check(InventoryDAO.loadInventory().findItemByName("Laptop").getStock()==stockBefore,"Failed manager change restores database");
        try(Connection db=DriverManager.getConnection("jdbc:sqlite:"+StoreDatabase.path()); Statement sql=db.createStatement()) { sql.execute("DROP TRIGGER reject_inventory"); }
        AuthService.authenticate("owner","secret123");
        String hash = PasswordManager.hashPassword("secret123");
        check(PasswordManager.verifyPassword("secret123", hash), "Password verification");
        check(!PasswordManager.verifyPassword("wrong", hash), "Wrong password");
        AdminController admin = new AdminController();
        User employee = admin.findEmployee("cashierUser");
        String originalHash = employee.getPassword();
        admin.updateEmployee("cashierUser", "cashierUser", "", "Updated Name", "c@example.com", "0123456789", Role.CASHIER, "Sales");
        User reloaded = new AdminController().findEmployee("cashierUser");
        check(reloaded.getPassword().equals(originalHash), "Blank password preserves hash");
        check(reloaded.getName().equals("Updated Name"), "Employee name persists");
        rejected(() -> admin.deleteEmployee(admin.findEmployee("owner")));
        rejected(() -> admin.updateEmployee("owner","owner","","Owner","owner@example.com","0123456789",Role.CASHIER,"Sales"));
        Path credentials = AppPaths.daoFile("passwords.txt");
        Path held=credentials.resolveSibling("passwords.held");
        Files.move(credentials,held); Files.createDirectory(credentials);
        try {
            try { admin.addEmployee("newemployee","secret123","New","n@example.com","0123456789",Role.CASHIER,"Sales"); throw new AssertionError("Failed employee save accepted"); }
            catch(IllegalStateException expected) { checks++; }
            check(admin.findEmployee("newemployee")==null,"Failed employee add restores memory");
        } finally { Files.delete(credentials); Files.move(held,credentials); }
        check(new AdminController().findEmployee("newemployee")==null,"Failed employee save leaves credentials intact");
        admin.addEmployee("temporary","secret123","Temp","temp@example.com","0123456789",Role.CASHIER,"Sales");
        check(new AdminController().findEmployee("temporary")!=null,"Employee add persists");
        admin.deleteEmployee(admin.findEmployee("temporary"));
        check(new AdminController().findEmployee("temporary")==null,"Employee delete persists");
        Session.clear();
        try { controller.addItemToBill("Laptop",1); throw new AssertionError("Logged-out checkout accepted"); }
        catch(SecurityException expected) { checks++; }

        Files.writeString(credentials, "legacy,secret123,l@example.com,0123456789,Cashier\n");
        PasswordMigrationUtil.migratePasswordsToHash();
        String migrated = Files.readString(credentials);
        check(PasswordManager.verifyPassword("secret123", migrated.split(",")[1]), "Migration preserves account");
        check(Files.readString(credentials.resolveSibling("passwords.txt.backup")).contains("secret123"), "Migration backup");
        PasswordMigrationUtil.migratePasswordsToHash();
        check(Files.readString(credentials).equals(migrated), "Migration idempotent");
        Files.writeString(credentials, "invalid record");
        try { PasswordMigrationUtil.migratePasswordsToHash(); throw new AssertionError("Invalid migration accepted"); }
        catch (IllegalArgumentException expected) { checks++; }
        check(Files.readString(credentials).equals("invalid record"), "Failed migration leaves original intact");
        AuditLogger.logAction("user", "SALE", "bill", "1", "test");
        check(AuditLogger.loadAuditEntries().size() == 1, "Audit entries loaded");
        System.out.println("PASS: " + checks + " regression checks");
    }
}
