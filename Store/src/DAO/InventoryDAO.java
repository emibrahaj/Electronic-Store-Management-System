package DAO;
import Model.Inventory;
public final class InventoryDAO {
    private InventoryDAO() { }
    public static Inventory loadInventory() { return StoreDatabase.loadInventory(); }
    public static void saveInventory(Inventory value) { StoreDatabase.saveInventory(value); }
}
