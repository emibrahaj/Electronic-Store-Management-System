package Model;

import javafx.beans.property.*;

import java.io.Serializable;

public class Item implements Serializable {
    private static final long serialVersionUID = 1L;

    // Standard fields (Serializable)
    private String sku = java.util.UUID.randomUUID().toString();
    private String barcode = "";
    private String itemName;
    private String category;
    private String supplier;
    private double purchasePrice;
    private double sellingPrice;
    private int stock;
    private int quantity;  // New field for quantity in the bill

    // Transient JavaFX properties (Not serialized)
    private transient StringProperty itemNameProperty;
    private transient StringProperty categoryProperty;
    private transient StringProperty supplierProperty;
    private transient DoubleProperty purchasePriceProperty;
    private transient DoubleProperty sellingPriceProperty;
    private transient IntegerProperty stockProperty;
    private transient IntegerProperty quantityProperty;  // JavaFX property for quantity

    public Item(String itemName, String category, String supplier, double purchasePrice, double sellingPrice, int stock) {
        this.itemName = itemName;
        this.category = category;
        this.supplier = supplier;
        this.purchasePrice = purchasePrice;
        this.sellingPrice = sellingPrice;
        this.stock = stock;
        this.quantity = 0;  // Default quantity is 0 until added to a bill

        // Initialize JavaFX properties
        initializeProperties();
    }

    // Initialize JavaFX properties
    private void initializeProperties() {
        itemNameProperty = new SimpleStringProperty(itemName);
        categoryProperty = new SimpleStringProperty(category);
        supplierProperty = new SimpleStringProperty(supplier);
        purchasePriceProperty = new SimpleDoubleProperty(purchasePrice);
        sellingPriceProperty = new SimpleDoubleProperty(sellingPrice);
        stockProperty = new SimpleIntegerProperty(stock);
        quantityProperty = new SimpleIntegerProperty(quantity);  // Initialize the quantity property
    }

    // Getters and setters for standard fields (used for serialization)
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { 
        this.itemName = itemName; 
        itemNameProperty().set(itemName);
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { 
        this.category = category; 
        categoryProperty().set(category);
    }

    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { 
        this.supplier = supplier; 
        supplierProperty().set(supplier);
    }

    public double getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(double purchasePrice) { 
        this.purchasePrice = purchasePrice; 
        purchasePriceProperty().set(purchasePrice);
    }

    public double getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(double sellingPrice) { 
        this.sellingPrice = sellingPrice; 
        sellingPriceProperty().set(sellingPrice);
    }

    public int getStock() { return stock; }
    public void setStock(int stock) {
        this.stock = stock;
        stockProperty().set(stock);  // Update the JavaFX property when stock changes
    }

    public int getQuantity() { return quantity; }  // Get the quantity for the bill
    public void setQuantity(int quantity) { 
        this.quantity = quantity; 
        quantityProperty().set(quantity);  // Update the JavaFX property when quantity changes
    }

    public String getSku() {
        if (sku == null || sku.isBlank()) sku = java.util.UUID.randomUUID().toString();
        return sku;
    }
    public void restoreSku(String sku) { this.sku = java.util.Objects.requireNonNull(sku); }
    public String getBarcode() { return barcode == null ? "" : barcode; }
    public void setBarcode(String barcode) {
        if (barcode == null || !barcode.matches("[A-Za-z0-9_-]{0,64}"))
            throw new IllegalArgumentException("Barcode must contain at most 64 letters, numbers, underscores or hyphens.");
        this.barcode = barcode;
    }
    public java.math.BigDecimal getPurchaseAmount() { return Util.Money.of(purchasePrice); }
    public java.math.BigDecimal getSellingAmount() { return Util.Money.of(sellingPrice); }
    public Item copy() {
        Item copy = new Item(itemName, category, supplier, purchasePrice, sellingPrice, stock);
        copy.restoreSku(getSku()); copy.setBarcode(getBarcode()); copy.setQuantity(quantity);
        return copy;
    }

    // JavaFX property getters (lazy initialization)
    public StringProperty itemNameProperty() {
        if (itemNameProperty == null) itemNameProperty = new SimpleStringProperty(itemName);
        return itemNameProperty;
    }

    public StringProperty categoryProperty() {
        if (categoryProperty == null) categoryProperty = new SimpleStringProperty(category);
        return categoryProperty;
    }

    public StringProperty supplierProperty() {
        if (supplierProperty == null) supplierProperty = new SimpleStringProperty(supplier);
        return supplierProperty;
    }

    public DoubleProperty purchasePriceProperty() {
        if (purchasePriceProperty == null) purchasePriceProperty = new SimpleDoubleProperty(purchasePrice);
        return purchasePriceProperty;
    }

    public DoubleProperty sellingPriceProperty() {
        if (sellingPriceProperty == null) sellingPriceProperty = new SimpleDoubleProperty(sellingPrice);
        return sellingPriceProperty;
    }

    public IntegerProperty stockProperty() {
        if (stockProperty == null) stockProperty = new SimpleIntegerProperty(stock);
        return stockProperty;
    }

    public IntegerProperty quantityProperty() { 
        if (quantityProperty == null) quantityProperty = new SimpleIntegerProperty(quantity); 
        return quantityProperty; 
    }

    // Method to reinitialize transient properties after deserialization
    public void restoreProperties() {
        initializeProperties();
    }

    @Override
    public String toString() {
        return "Item: " + itemName + " | Category: " + category + " | Supplier: " + supplier +
               " | Purchase Price: " + purchasePrice + " | Selling Price: " + sellingPrice + " | Stock: " + stock + " | Quantity in Bill: " + quantity;
    }
}
