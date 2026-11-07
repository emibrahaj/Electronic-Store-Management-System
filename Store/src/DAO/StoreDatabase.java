package DAO;

import Model.*;
import Util.Money;
import Util.Session;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/** SQLite owns all business data; a checkout commits stock, history and movement records together. */
public final class StoreDatabase {
    private StoreDatabase() { }
    public static Path path() { return AppPaths.daoFile("store.db"); }
    private static Connection open() throws Exception {
        Class.forName("org.sqlite.JDBC");
        Files.createDirectories(path().getParent());
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + path());
        try (Statement s = connection.createStatement()) {
            s.execute("PRAGMA foreign_keys=ON");
            s.execute("PRAGMA busy_timeout=5000");
            s.execute("PRAGMA synchronous=FULL");
        }
        return connection;
    }
    @FunctionalInterface private interface Work<T> { T run(Connection c) throws Exception; }
    private static synchronized <T> T transaction(Work<T> work) {
        try (Connection c = open()) {
            c.setAutoCommit(false);
            try {
                schema(c);
                migrate(c);
                T result = work.run(c);
                c.commit();
                return result;
            } catch (Exception e) {
                c.rollback(); throw e;
            }
        } catch (Exception e) { throw new DataPersistenceException("access", "SQLite store", e); }
    }
    private static void schema(Connection c) throws SQLException {
        try (Statement s = c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS metadata (key TEXT PRIMARY KEY, value TEXT NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS products (sku TEXT PRIMARY KEY, name TEXT NOT NULL COLLATE NOCASE UNIQUE, category TEXT NOT NULL, supplier TEXT NOT NULL, purchase_cents INTEGER NOT NULL CHECK(purchase_cents>=0), selling_cents INTEGER NOT NULL CHECK(selling_cents>=0), stock INTEGER NOT NULL CHECK(stock>=0 AND stock<=999999), barcode TEXT UNIQUE)");
            s.execute("CREATE TABLE IF NOT EXISTS bills (id TEXT PRIMARY KEY, created_at INTEGER NOT NULL, refunded_cents INTEGER NOT NULL CHECK(refunded_cents>=0))");
            s.execute("CREATE TABLE IF NOT EXISTS bill_lines (bill_id TEXT NOT NULL REFERENCES bills(id) ON DELETE CASCADE, position INTEGER NOT NULL, sku TEXT NOT NULL, name TEXT NOT NULL, category TEXT NOT NULL, supplier TEXT NOT NULL, purchase_cents INTEGER NOT NULL, selling_cents INTEGER NOT NULL, quantity INTEGER NOT NULL CHECK(quantity>0), PRIMARY KEY(bill_id,position))");
            s.execute("CREATE TABLE IF NOT EXISTS suppliers (name TEXT PRIMARY KEY COLLATE NOCASE, contact TEXT NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS supplier_products (supplier TEXT NOT NULL REFERENCES suppliers(name) ON DELETE CASCADE, product TEXT NOT NULL, PRIMARY KEY(supplier,product))");
            s.execute("CREATE TABLE IF NOT EXISTS stock_movements (id INTEGER PRIMARY KEY, happened_at TEXT NOT NULL, sku TEXT NOT NULL, product TEXT NOT NULL, delta INTEGER NOT NULL, reason TEXT NOT NULL, username TEXT NOT NULL)");
        }
    }
    private static Object legacy(String name, Object fallback) throws Exception {
        Path path = AppPaths.daoFile(name);
        if (!Files.exists(path)) return fallback;
        if (Files.size(path) == 0) throw new IOException("Legacy file is empty: " + name + ". Restore a backup before migration.");
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(path))) {
            in.setObjectInputFilter(ObjectInputFilter.Config.createFilter("maxdepth=30;maxrefs=1000000;maxbytes=50000000;Model.*;java.base/*;!*"));
            return in.readObject();
        }
    }
    @SuppressWarnings("unchecked") private static void migrate(Connection c) throws Exception {
        try (PreparedStatement s = c.prepareStatement("SELECT value FROM metadata WHERE key='migration-v1'")) {
            try (ResultSet r = s.executeQuery()) { if (r.next()) return; }
        }
        Inventory inventory = (Inventory) legacy("inventory.dat", new Inventory());
        List<Bill> bills = (List<Bill>) legacy("bills.dat", new ArrayList<Bill>());
        List<Supplier> suppliers = (List<Supplier>) legacy("suppliers.dat", new ArrayList<Supplier>());
        // Old serialized bills had no SKU. Match their product names once during import.
        for (Bill bill : bills) for (Item line : bill.getItems()) {
            Item product = inventory.findItemByName(line.getItemName());
            if (product != null) line.restoreSku(product.getSku());
        }
        writeInventory(c, inventory, "LEGACY_IMPORT"); writeBills(c, bills); writeSuppliers(c, suppliers);
        try (Statement s = c.createStatement()) { s.executeUpdate("INSERT INTO metadata VALUES ('migration-v1','complete')"); }
        // Original .dat files are deliberately left unchanged as migration backups.
    }
    private static Item readItem(ResultSet r) throws SQLException {
        Item item = new Item(r.getString("name"), r.getString("category"), r.getString("supplier"),
                Money.fromCents(r.getLong("purchase_cents")), Money.fromCents(r.getLong("selling_cents")), r.getInt("stock"));
        item.restoreSku(r.getString("sku"));
        item.setBarcode(Objects.toString(r.getString("barcode"), ""));
        return item;
    }
    public static Inventory loadInventory() {
        return transaction(c -> {
            Inventory inventory = new Inventory();
            try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT * FROM products ORDER BY name")) {
                while (r.next()) inventory.addItem(readItem(r));
            }
            return inventory;
        });
    }
    public static List<Bill> loadBills() {
        return transaction(c -> {
            List<Bill> result = new ArrayList<>();
            try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT * FROM bills ORDER BY created_at,id")) {
                while (r.next()) {
                    Bill bill = new Bill(r.getString("id"), new java.util.Date(r.getLong("created_at")));
                    try (PreparedStatement lines = c.prepareStatement("SELECT *,0 AS stock,NULL AS barcode FROM bill_lines WHERE bill_id=? ORDER BY position")) {
                        lines.setString(1, bill.getBillNumber());
                        try (ResultSet row = lines.executeQuery()) {
                            while (row.next()) { Item item = readItem(row); item.setQuantity(row.getInt("quantity")); bill.addItem(item); }
                        }
                    }
                    bill.restoreRefund(Money.fromCents(r.getLong("refunded_cents"))); result.add(bill);
                }
            }
            return result;
        });
    }
    public static List<Supplier> loadSuppliers() {
        return transaction(c -> {
            List<Supplier> result = new ArrayList<>();
            try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT * FROM suppliers ORDER BY name")) {
                while (r.next()) {
                    Supplier supplier = new Supplier(r.getString("name"), r.getString("contact"));
                    try (PreparedStatement products = c.prepareStatement("SELECT product FROM supplier_products WHERE supplier=?")) {
                        products.setString(1, supplier.getSupplierName());
                        try (ResultSet names = products.executeQuery()) { while (names.next()) supplier.addItem(new Item(names.getString(1), "", supplier.getSupplierName(), 0, 0, 0)); }
                    }
                    result.add(supplier);
                }
            }
            return result;
        });
    }
    private static void movement(Connection c, String sku, String name, int delta, String reason) throws SQLException {
        if (delta == 0) return;
        try (PreparedStatement s = c.prepareStatement("INSERT INTO stock_movements(happened_at,sku,product,delta,reason,username) VALUES (?,?,?,?,?,?)")) {
            s.setString(1, Instant.now().toString()); s.setString(2, sku); s.setString(3, name);
            s.setInt(4, delta); s.setString(5, reason); s.setString(6, Session.username()); s.executeUpdate();
        }
    }
    private static void writeInventory(Connection c, Inventory inventory, String reason) throws SQLException {
        Map<String,Integer> old = new HashMap<>(); Map<String,String> names = new HashMap<>();
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT sku,name,stock FROM products")) {
            while (r.next()) { old.put(r.getString(1), r.getInt(3)); names.put(r.getString(1), r.getString(2)); }
        }
        try (Statement s = c.createStatement()) { s.executeUpdate("DELETE FROM products"); }
        try (PreparedStatement s = c.prepareStatement("INSERT INTO products VALUES (?,?,?,?,?,?,?,?)")) {
            for (Item item : inventory.getItems()) {
                s.setString(1,item.getSku()); s.setString(2,item.getItemName()); s.setString(3,item.getCategory()); s.setString(4,item.getSupplier());
                s.setLong(5,Money.cents(item.getPurchasePrice())); s.setLong(6,Money.cents(item.getSellingPrice())); s.setInt(7,item.getStock());
                if (item.getBarcode().isBlank()) s.setNull(8,Types.VARCHAR); else s.setString(8,item.getBarcode());
                s.executeUpdate();
                movement(c,item.getSku(),item.getItemName(),item.getStock()-old.getOrDefault(item.getSku(),0),reason);
                old.remove(item.getSku());
            }
        }
        for (var entry : old.entrySet()) movement(c,entry.getKey(),names.get(entry.getKey()),-entry.getValue(),reason);
    }
    private static void writeBills(Connection c, List<Bill> bills) throws SQLException {
        try (Statement s = c.createStatement()) { s.executeUpdate("DELETE FROM bill_lines"); s.executeUpdate("DELETE FROM bills"); }
        try (PreparedStatement bill = c.prepareStatement("INSERT INTO bills VALUES (?,?,?)");
             PreparedStatement line = c.prepareStatement("INSERT INTO bill_lines VALUES (?,?,?,?,?,?,?,?,?)")) {
            for (Bill b : bills) {
                bill.setString(1,b.getBillNumber()); bill.setLong(2,b.getDate().getTime()); bill.setLong(3,Money.cents(b.getRefundedAmount())); bill.executeUpdate();
                int position=0;
                for (Item item : b.getItems()) {
                    line.setString(1,b.getBillNumber()); line.setInt(2,position++); line.setString(3,item.getSku()); line.setString(4,item.getItemName());
                    line.setString(5,item.getCategory()); line.setString(6,item.getSupplier()); line.setLong(7,Money.cents(item.getPurchasePrice()));
                    line.setLong(8,Money.cents(item.getSellingPrice())); line.setInt(9,item.getQuantity()); line.executeUpdate();
                }
            }
        }
    }
    private static void writeSuppliers(Connection c, List<Supplier> suppliers) throws SQLException {
        try (Statement s = c.createStatement()) { s.executeUpdate("DELETE FROM supplier_products"); s.executeUpdate("DELETE FROM suppliers"); }
        try (PreparedStatement supplier = c.prepareStatement("INSERT INTO suppliers VALUES (?,?)"); PreparedStatement product = c.prepareStatement("INSERT OR IGNORE INTO supplier_products VALUES (?,?)")) {
            for (Supplier s : suppliers) {
                supplier.setString(1,s.getSupplierName()); supplier.setString(2,s.getContactDetails()); supplier.executeUpdate();
                for (Item item : s.getSuppliedItems()) { product.setString(1,s.getSupplierName()); product.setString(2,item.getItemName()); product.executeUpdate(); }
            }
        }
    }
    public static void saveInventory(Inventory inventory) { transaction(c -> { writeInventory(c,inventory,"STOCK_ADJUSTMENT"); return null; }); }
    public static void saveBills(List<Bill> bills) { transaction(c -> { writeBills(c,bills); return null; }); }
    public static void saveSuppliers(List<Supplier> suppliers) { transaction(c -> { writeSuppliers(c,suppliers); return null; }); }
    public static void saveInventoryAndSuppliers(Inventory inventory, List<Supplier> suppliers, String reason) {
        transaction(c -> { writeInventory(c,inventory,reason); writeSuppliers(c,suppliers); return null; });
    }
    public static void checkout(Inventory inventory, List<Bill> bills, String reason) {
        transaction(c -> { writeInventory(c,inventory,reason); writeBills(c,bills); return null; });
    }
    public static List<List<String>> movements() {
        return transaction(c -> {
            List<List<String>> result = new ArrayList<>();
            try (Statement s=c.createStatement(); ResultSet r=s.executeQuery("SELECT happened_at,sku,product,delta,reason,username FROM stock_movements ORDER BY id DESC")) {
                while(r.next()) result.add(List.of(r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6)));
            }
            return result;
        });
    }
}
