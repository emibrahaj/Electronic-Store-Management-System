package Model;

import DAO.BillDAO;
import DAO.InventoryDAO;
import DAO.SupplierDAO;

import java.time.LocalDate;
import java.util.List;

public class Manager extends User {
    private String sector;
    private Inventory inventory;
    private List<Supplier> suppliers;
    private List<Bill> bills;

    public Manager(String username, String password, String name, String email, String phoneNumber) {
        super(username, password, name, email, phoneNumber);
        setRole(Role.MANAGER);
        this.inventory = Util.SharedContext.getInstance().getInventory();
        this.suppliers = SupplierDAO.loadSuppliers();
        this.bills = Util.SharedContext.getInstance().getBills();
    }

    private void change(String reason, Runnable operation) {
        Util.Session.require(Role.MANAGER, Role.ADMIN);
        List<Item> oldItems=inventory.getItems().stream().map(Item::copy).toList();
        List<Supplier> oldSuppliers=new java.util.ArrayList<>();
        for(Supplier supplier:suppliers) {
            Supplier copy=new Supplier(supplier.getSupplierName(),supplier.getContactDetails());
            for(Item item:supplier.getSuppliedItems())copy.addItem(item.copy());
            oldSuppliers.add(copy);
        }
        try { operation.run(); DAO.StoreDatabase.saveInventoryAndSuppliers(inventory,suppliers,reason); }
        catch(RuntimeException e) {
            inventory.getItems().clear(); inventory.getItems().addAll(oldItems);
            suppliers.clear(); suppliers.addAll(oldSuppliers); throw e;
        }
    }
    public void addItemToInventory(Item item, String contactDetails) {
        change("ADD_PRODUCT", () -> {
            if(inventory.findByBarcode(item.getBarcode())!=null)throw new IllegalArgumentException("Barcode already belongs to another product.");
            inventory.addItem(item); upsertSupplier(item.getSupplier(),contactDetails,item.getItemName());
        });
    }
    public void removeItemFromInventory(String itemName) {
        change("REMOVE_PRODUCT", () -> {
            if(inventory.findItemByName(itemName)==null)throw new IllegalArgumentException("Product not found.");
            inventory.removeItem(itemName);
        });
    }
    public void updateItemInInventory(String itemName,double purchasePrice,double sellingPrice,int stock) {
        change("UPDATE_PRICE_OR_STOCK", () -> inventory.updateItem(itemName,purchasePrice,sellingPrice,stock));
    }
    public void updateProduct(String sku,double purchasePrice,double sellingPrice,int stock,String barcode) {
        change("UPDATE_PRODUCT", () -> {
            Item item=inventory.findBySku(sku);
            if(item==null)throw new IllegalArgumentException("Product not found.");
            Item duplicate=inventory.findByBarcode(barcode);
            if(duplicate!=null && duplicate!=item)throw new IllegalArgumentException("Barcode already belongs to another product.");
            item.setBarcode(barcode); inventory.updateItem(item.getItemName(),purchasePrice,sellingPrice,stock);
        });
    }

    public Inventory getInventory() {
        return inventory;
    }

    // Supplier Management Methods
    public void addSupplierIfNotExists(String supplierName,String contactDetails,String productName) {
        change("SUPPLIER_UPDATE", () -> upsertSupplier(supplierName,contactDetails,productName));
    }
    private void upsertSupplier(String supplierName,String contactDetails,String productName) {
        if(supplierName==null||supplierName.isBlank())throw new IllegalArgumentException("Supplier is required.");
        Supplier supplier=suppliers.stream().filter(s -> s.getSupplierName().equalsIgnoreCase(supplierName)).findFirst().orElse(null);
        if(supplier==null) { supplier=new Supplier(supplierName,contactDetails==null ? "" : contactDetails); suppliers.add(supplier); }
        if(contactDetails!=null&&!contactDetails.isBlank())supplier.setContactDetails(contactDetails);
        supplier.addItem(new Item(productName,"",supplierName,0,0,0));
    }

    public List<Supplier> getSuppliers() {
        return suppliers;
    }

    // Statistics Methods
    public int getTotalBillsByDate(LocalDate date) {
        return (int) bills.stream()
                .filter(bill -> bill.getDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate().equals(date))
                .count();
    }

    public int getTotalItemsSoldByDate(LocalDate date) {
        return bills.stream()
                .filter(bill -> bill.getDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate().equals(date))
                .mapToInt(Bill::getTotalItemsSold)
                .sum();
    }

    public double getTotalRevenueByDate(LocalDate date) {
        return bills.stream()
                .filter(bill -> bill.getDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate().equals(date))
                .map(Bill::getNetAmount)
                .reduce(java.math.BigDecimal.ZERO,java.math.BigDecimal::add).doubleValue();
    }

    public int getTotalBillsForPeriod(LocalDate startDate, LocalDate endDate) {
        return (int) bills.stream()
                .filter(bill -> {
                    LocalDate billDate = bill.getDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                    return !billDate.isBefore(startDate) && !billDate.isAfter(endDate);
                })
                .count();
    }

    public int getTotalItemsSoldForPeriod(LocalDate startDate, LocalDate endDate) {
        return bills.stream()
                .filter(bill -> {
                    LocalDate billDate = bill.getDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                    return !billDate.isBefore(startDate) && !billDate.isAfter(endDate);
                })
                .mapToInt(Bill::getTotalItemsSold)
                .sum();
    }

    public double getTotalRevenueForPeriod(LocalDate startDate, LocalDate endDate) {
        return bills.stream()
                .filter(bill -> {
                    LocalDate billDate = bill.getDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                    return !billDate.isBefore(startDate) && !billDate.isAfter(endDate);
                })
                .map(Bill::getNetAmount)
                .reduce(java.math.BigDecimal.ZERO,java.math.BigDecimal::add).doubleValue();
    }

    public List<Bill> getAllBills() {
        return bills;
    }

    // Debugging Method
    public void printBillDetails() {
        bills.forEach(bill -> System.out.println("Bill: " + bill.getBillNumber() + " Total: " + bill.getTotalAmount()));
    }
}
