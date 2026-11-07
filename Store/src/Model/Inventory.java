package Model;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Inventory implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<Item> items;
    private static final int LOW_STOCK_THRESHOLD = 5;

    public Inventory() {
        items = new ArrayList<>();
    }

    public void addItem(Item item) {
        if (item != null) validateValues(item.getPurchasePrice(), item.getSellingPrice(), item.getStock());
        if (item == null || findItemByName(item.getItemName()) != null)
            throw new IllegalArgumentException("Item already exists or is invalid.");
        items.add(item);
    }

    public void removeItem(String itemName) {
        items.removeIf(item -> item.getItemName().equalsIgnoreCase(itemName));
    }

    public Item findItemByName(String itemName) {
        for (Item item : items) {
            if (item.getItemName().equalsIgnoreCase(itemName)) {
                return item;
            }
        }
        return null;
    }

    public Item findBySku(String sku) {
        return items.stream().filter(item -> item.getSku().equals(sku)).findFirst().orElse(null);
    }
    public Item findByBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) return null;
        return items.stream().filter(item -> item.getBarcode().equals(barcode)).findFirst().orElse(null);
    }

    public void updateItem(String itemName, double purchasePrice, double sellingPrice, int stock) {
        validateValues(purchasePrice, sellingPrice, stock);
        for (Item item : items) {
            if (item.getItemName().equalsIgnoreCase(itemName)) {
                item.setPurchasePrice(purchasePrice);
                item.setSellingPrice(sellingPrice);
                item.setStock(stock);
                return;
            }
        }
        throw new IllegalArgumentException("Item not found: " + itemName);
    }

    private static void validateValues(double purchasePrice, double sellingPrice, int stock) {
        if (!Util.InputValidator.isValidPrice(purchasePrice) || !Util.InputValidator.isValidPrice(sellingPrice)
                || !Util.InputValidator.isValidQuantity(stock))
            throw new IllegalArgumentException("Enter positive finite prices and stock between 0 and 999999.");
    }

    public List<Item> getItems() {
        return items;
    }

    public void checkLowStock() {
        for (Item item : items) {
            if (item.getStock() < LOW_STOCK_THRESHOLD) {
                System.out.println("Low stock alert! Item: " + item.getItemName() + ", Quantity: " + item.getStock());
            }
        }
    }

    public void displayInventory() {
        for (Item item : items) {
            System.out.println(item);
        }
    }

    public String generateReport() {
        StringBuilder report = new StringBuilder("Inventory Report:\n");
        for (Item item : items) {
            report.append(item.toString()).append("\n");
        }
        return report.toString();
    }

    public void saveInventoryToFile(String filePath) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(this);
            System.out.println("Inventory saved to file.");
        } catch (IOException e) {
            System.out.println("Error saving inventory to file: " + e.getMessage());
        }
    }

    public static Inventory loadInventoryFromFile(String filePath) {
        File file = new File(filePath);
        if (file.exists() && file.length() > 0) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                return (Inventory) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Error loading inventory from file: " + e.getMessage());
            }
        }
        return new Inventory();
    }
}
