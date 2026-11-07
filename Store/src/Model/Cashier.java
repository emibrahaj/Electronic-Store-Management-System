package Model;
import DAO.AppPaths;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


public class Cashier extends User implements Serializable {
    private static final long serialVersionUID = 1L;
    private String sector;
    private List<Bill> bills;
    private Bill currentBill;

    private Inventory inventory;  // Add inventory field

    public Cashier(String username, String password, String name, String email, String phoneNumber, String sector, Inventory inventory) {
        super(username, password, name, email, phoneNumber);
        setRole(Role.CASHIER);
        this.sector = sector;
        this.inventory = inventory;
        this.bills = new ArrayList<>();
        loadBillsFromFile();
    }

    // Getter for sector
    public String getSector() {
        return sector;
    }


    public Bill createNewBill() {
        // Check if there's already an active bill
        if (currentBill == null) {
            currentBill = new Bill(generateBillNumber(), new Date()); // Create new bill if none exists

            return currentBill;
        }
        return currentBill; // Return the existing bill if one is already open
    }

    public void addItemToCurrentBill(Item item, int quantity) {
        Util.Session.require(Role.CASHIER, Role.ADMIN);
        if (item == null || quantity <= 0) throw new IllegalArgumentException("Quantity must be positive.");
        if (currentBill == null) createNewBill();
        int alreadyAdded = currentBill.getItems().stream()
                .filter(line -> line.getSku().equals(item.getSku()))
                .mapToInt(Item::getQuantity).sum();
        if ((long) alreadyAdded + quantity > item.getStock())
            throw new IllegalArgumentException("Not enough stock available.");
        Item line = item.copy();
        line.setQuantity(quantity);
        currentBill.addItem(line);
    }

    public void finalizeCurrentBill() {
        Util.Session.require(Role.CASHIER, Role.ADMIN);
        if (currentBill == null || currentBill.getItems().isEmpty())
            throw new IllegalArgumentException("Add items before finalizing a bill.");
        for (Item line : currentBill.getItems()) {
            Item stock = inventory.findBySku(line.getSku());
            if (stock == null || stock.getStock() < line.getQuantity())
                throw new IllegalArgumentException("Insufficient stock for " + line.getItemName());
        }
        for (Item line : currentBill.getItems()) {
            Item stock = inventory.findBySku(line.getSku());
            stock.setStock(stock.getStock() - line.getQuantity());
        }
        bills.add(currentBill);
        try {
            DAO.StoreDatabase.checkout(inventory, bills, "SALE " + currentBill.getBillNumber());
        } catch (RuntimeException e) {
            bills.remove(currentBill);
            for (Item line : currentBill.getItems()) {
                Item stock = inventory.findBySku(line.getSku());
                stock.setStock(stock.getStock() + line.getQuantity());
            }
            throw e;
        }
        Bill completed = currentBill;
        currentBill = null;
        saveBillToPrintableFile(completed);
    }

    public boolean processReturn(String billNumber, String itemName, int quantity) {
        Util.Session.require(Role.CASHIER, Role.ADMIN);
        Bill bill = findBillByNumber(billNumber);
        if (bill == null || quantity <= 0) {
            return false;
        }

        Item sold = bill.getItems().stream().filter(item -> item.getItemName().equalsIgnoreCase(itemName)).findFirst().orElse(null);
        Item inventoryItem = sold == null ? null : inventory.findBySku(sold.getSku());
        if (inventoryItem == null) return false;
        Bill snapshot = bill.snapshot();
        double refundAmount = bill.returnItem(itemName, quantity);
        if (refundAmount <= 0) {
            return false;
        }

        if (inventoryItem != null) {
            inventoryItem.setStock(inventoryItem.getStock() + quantity);
        }

        try {
            DAO.StoreDatabase.checkout(inventory, bills, "RETURN " + bill.getBillNumber());
        } catch (RuntimeException e) {
            bill.restore(snapshot);
            inventoryItem.setStock(inventoryItem.getStock() - quantity);
            throw e;
        }
        return true;
    }

    private Bill findBillByNumber(String billNumber) {
        for (Bill bill : bills) {
            if (bill.getBillNumber().equalsIgnoreCase(billNumber)) {
                return bill;
            }
        }
        return null;
    }

    private void saveBillToPrintableFile(Bill bill) {
        Path folderPath = AppPaths.printableBillsFolder();
        File directory = folderPath.toFile();

        // Ensure the directory exists
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Generate filename using bill number and date
        Path billPath = folderPath.resolve("Bill_" + bill.getBillNumber() + "_"
                          + new SimpleDateFormat("yyyyMMdd").format(bill.getDate()) + ".txt");

        try (BufferedWriter writer = Files.newBufferedWriter(billPath)) {
            writer.write("Bill Number: " + bill.getBillNumber() + "\n");
            writer.write("Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(bill.getDate()) + "\n");
            writer.write("----------------------------------------------------\n");
            writer.write(String.format("%-20s %-10s %-10s\n", "Item", "Qty", "Price"));
            writer.write("----------------------------------------------------\n");

            for (Item item : bill.getItems()) {
                writer.write(String.format("%-20s %-10d $%-10.2f\n",
                        item.getItemName(), item.getQuantity(), item.getSellingPrice() * item.getQuantity()));
            }

            writer.write("----------------------------------------------------\n");
            writer.write("Total Amount: $" + String.format("%.2f", bill.getTotalAmount()) + "\n");
            writer.write("----------------------------------------------------\n");

            // Show success alert when bill is saved
            showAlert("Success", "Bill saved successfully to:\n" + billPath);

        } catch (IOException e) {
            showAlert("Error", "Failed to save bill: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Util.Logger.info("Cashier", title + ": " + message);
    }

    public void saveInventoryToFile() {
        DAO.InventoryDAO.saveInventory(inventory);
    }

    public void updateCartQuantity(String sku, int quantity) {
        Util.Session.require(Role.CASHIER, Role.ADMIN);
        Item stock = inventory.findBySku(sku);
        if (stock == null || quantity < 0 || quantity > stock.getStock())
            throw new IllegalArgumentException("Quantity must be within available stock.");
        if (currentBill == null) throw new IllegalArgumentException("No active cart.");
        currentBill.setQuantity(sku, quantity);
    }
    public void cancelBill() { Util.Session.require(Role.CASHIER, Role.ADMIN); currentBill = null; }

    public Bill getCurrentBill() {
        return currentBill;
    }

    public List<Bill> getTodaysBills() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String today = sdf.format(new Date());

        return bills.stream()
            .filter(bill -> sdf.format(bill.getDate()).equals(today))
            .collect(Collectors.toList());
    }

    public void printBills() {
        List<Bill> todaysBills = getTodaysBills();
        for (Bill bill : todaysBills) {
            System.out.println("Bill Number: " + bill.getBillNumber());
            System.out.println("Date: " + bill.getDate());
            for (Item item : bill.getItems()) {
                System.out.println(item.getItemName() + " - Quantity: " + item.getQuantity() + ", Price: $" + item.getSellingPrice());
            }
            System.out.println("Total Amount: $" + bill.getTotalAmount());
            System.out.println("-----------------------------");
        }
    }


    private String generateBillNumber() {
        return "B" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 19).toUpperCase();
    }

    private void saveBillsToFile() {
        DAO.BillDAO.saveBills(bills);
    }

    private void loadBillsFromFile() {
        bills = Util.SharedContext.getInstance().getBills();
    }

    public List<Bill> getAllBills() {
        return bills;
    }
}
