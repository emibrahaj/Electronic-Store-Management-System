package DAO;
import Model.Supplier;
public final class SupplierDAO {
    private SupplierDAO() { }
    public static java.util.List<Supplier> loadSuppliers() { return StoreDatabase.loadSuppliers(); }
    public static void saveSuppliers(java.util.List<Supplier> value) { StoreDatabase.saveSuppliers(value); }
}
