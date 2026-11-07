package DAO;
import Model.Bill;
public final class BillDAO {
    private BillDAO() { }
    public static java.util.List<Bill> loadBills() { return StoreDatabase.loadBills(); }
    public static void saveBills(java.util.List<Bill> value) { StoreDatabase.saveBills(value); }
}
