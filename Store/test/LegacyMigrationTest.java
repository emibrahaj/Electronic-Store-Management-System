import DAO.*;
import Model.*;
import java.nio.file.*;
import java.util.*;
import java.io.*;

/** Imports copies of the repository's original data, never the originals. */
public class LegacyMigrationTest {
    @SuppressWarnings("unchecked") public static void main(String[] args) throws Exception {
        Path home=Files.createTempDirectory(Path.of(".build"),"legacy-compatibility-").toAbsolutePath();
        Files.createDirectories(home.resolve("src/DAO"));
        for(String file:List.of("inventory.dat","bills.dat","suppliers.dat"))
            Files.copy(Path.of("Store/src/DAO",file),home.resolve("src/DAO").resolve(file));
        System.setProperty("store.home",home.toString());
        Inventory expectedInventory;
        List<Bill> expectedBills;
        List<Supplier> expectedSuppliers;
        try(ObjectInputStream in=new ObjectInputStream(Files.newInputStream(AppPaths.daoFile("inventory.dat")))) { expectedInventory=(Inventory)in.readObject(); }
        try(ObjectInputStream in=new ObjectInputStream(Files.newInputStream(AppPaths.daoFile("bills.dat")))) { expectedBills=(List<Bill>)in.readObject(); }
        try(ObjectInputStream in=new ObjectInputStream(Files.newInputStream(AppPaths.daoFile("suppliers.dat")))) { expectedSuppliers=(List<Supplier>)in.readObject(); }
        if(InventoryDAO.loadInventory().getItems().size()!=expectedInventory.getItems().size())throw new AssertionError("Inventory import count");
        if(BillDAO.loadBills().size()!=expectedBills.size())throw new AssertionError("Bill import count");
        if(SupplierDAO.loadSuppliers().size()!=expectedSuppliers.size())throw new AssertionError("Supplier import count");
        System.out.println("PASS: original serialized inventory, bills and suppliers import successfully");
    }
}
