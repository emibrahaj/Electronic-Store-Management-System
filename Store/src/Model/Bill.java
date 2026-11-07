package Model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Bill implements Serializable {
    private static final long serialVersionUID = 1L;
    private String billNumber;
    private Date date;
    private List<Item> items;
    private double totalAmount;
    private double refundedAmount;
    private boolean fullyRefunded;

    public Bill(String billNumber, Date date) {
        this.billNumber = billNumber;
        this.date = date;
        this.items = new ArrayList<>();
        this.totalAmount = 0;
        this.refundedAmount = 0;
        this.fullyRefunded = false;
    }

    public void addItem(Item item) {
        if (item == null || item.getQuantity() <= 0) throw new IllegalArgumentException("Invalid bill quantity.");
        Item existing = items.stream().filter(line -> line.getSku().equals(item.getSku()))
                .findFirst().orElse(null);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + item.getQuantity());
        } else {
            Item snapshot = item.copy();
            this.items.add(snapshot);
        }
        this.totalAmount = getNetAmount().doubleValue();
    }

    public String getBillNumber() {
        return billNumber;
    }

    public Date getDate() {
        return date;
    }

    public List<Item> getItems() {
        return items;
    }

    public double getTotalAmount() {
        return getNetAmount().doubleValue();
    }

    public java.math.BigDecimal getNetAmount() {
        return items.stream().map(item -> item.getSellingAmount().multiply(java.math.BigDecimal.valueOf(item.getQuantity())))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add).setScale(2);
    }
    public java.math.BigDecimal getProfit() {
        return items.stream().map(item -> item.getSellingAmount().subtract(item.getPurchaseAmount())
                .multiply(java.math.BigDecimal.valueOf(item.getQuantity())))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add).setScale(2);
    }
    public void restoreRefund(double amount) { refundedAmount = amount; fullyRefunded = items.isEmpty() && amount > 0; }
    public void setQuantity(String sku, int quantity) {
        if (quantity < 0) throw new IllegalArgumentException("Quantity cannot be negative.");
        Item line = items.stream().filter(item -> item.getSku().equals(sku)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Select a cart item."));
        if (quantity == 0) items.remove(line); else line.setQuantity(quantity);
        totalAmount = getNetAmount().doubleValue();
    }

    public int getTotalItemsSold() {
        return items.stream().mapToInt(Item::getQuantity).sum();
    }

    public double getRefundedAmount() {
        return refundedAmount;
    }

    public boolean isFullyRefunded() {
        return fullyRefunded;
    }

    Bill snapshot() {
        Bill copy = new Bill(billNumber, date);
        for (Item item : items) copy.addItem(item);
        copy.totalAmount = totalAmount;
        copy.refundedAmount = refundedAmount;
        copy.fullyRefunded = fullyRefunded;
        return copy;
    }

    void restore(Bill snapshot) {
        items = snapshot.items;
        totalAmount = snapshot.totalAmount;
        refundedAmount = snapshot.refundedAmount;
        fullyRefunded = snapshot.fullyRefunded;
    }

    public double returnItem(String itemName, int quantity) {
        for (Item item : new ArrayList<>(items)) {
            if (item.getItemName().equalsIgnoreCase(itemName)) {
                if (quantity <= 0 || quantity > item.getQuantity()) {
                    return 0;
                }

                double refundValue = item.getSellingAmount().multiply(java.math.BigDecimal.valueOf(quantity)).doubleValue();
                int remainingQuantity = item.getQuantity() - quantity;
                item.setQuantity(remainingQuantity);
                this.totalAmount = Util.Money.of(this.totalAmount).subtract(Util.Money.of(refundValue)).doubleValue();
                this.refundedAmount = Util.Money.of(this.refundedAmount).add(Util.Money.of(refundValue)).doubleValue();

                if (remainingQuantity == 0) {
                    items.remove(item);
                }

                if (items.isEmpty()) {
                    fullyRefunded = true;
                }

                return refundValue;
            }
        }
        return 0;
    }

    @Override
    public String toString() {
        return "Bill Number: " + billNumber + "\nDate: " + date + "\nTotal Amount: " + totalAmount + "\nRefunded: " + refundedAmount;
    }
}
