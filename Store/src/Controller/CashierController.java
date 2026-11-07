package Controller;

import Model.Cashier;
import Model.Item;
import Model.Inventory;
import Model.Bill;
import java.util.List;

public class CashierController {
    private Cashier cashier;
    private Inventory inventory;

    public CashierController(Cashier cashier, Inventory inventory) {
        this.cashier = cashier;
        this.inventory = inventory;
    }


    public void addItemToBill(String itemName, int quantity) {
        Item item = inventory.findItemByName(itemName);
        if (item == null) throw new IllegalArgumentException("Item does not exist.");
        cashier.addItemToCurrentBill(item, quantity);
    }


    public List<Item> getCartItems() {
        return cashier.getCurrentBill() == null ? java.util.List.of() : cashier.getCurrentBill().getItems();
    }
    public void updateCartQuantity(String sku, int quantity) { cashier.updateCartQuantity(sku, quantity); }
    public void cancelBill() { cashier.cancelBill(); }
    public void scanBarcode(String barcode, int quantity) {
        Item item = inventory.findByBarcode(barcode);
        if (item == null) item = inventory.findBySku(barcode);
        if (item == null) throw new IllegalArgumentException("No product matches that barcode or SKU.");
        cashier.addItemToCurrentBill(item, quantity);
    }

    public List<Bill> getTodaysBills() {
        return cashier.getTodaysBills();
    }

    public double getTotalOfTodaysBills() {
        return cashier.getTodaysBills().stream()
                .map(Bill::getNetAmount)
                .reduce(java.math.BigDecimal.ZERO,java.math.BigDecimal::add).doubleValue();
    }

    public void finalizeBill() {
        cashier.finalizeCurrentBill();
    }

    public List<Item> getInventoryItems() {
        return inventory.getItems();  // Always get updated inventory
    }

    public void printBills() {
        cashier.printBills();
    }

    public boolean returnItemFromBill(String billNumber, String itemName, int quantity) {
        return cashier.processReturn(billNumber, itemName, quantity);
    }

    public double getTotalAmount() {
        return cashier.getCurrentBill() != null ? cashier.getCurrentBill().getTotalAmount() : 0;
    }
}
