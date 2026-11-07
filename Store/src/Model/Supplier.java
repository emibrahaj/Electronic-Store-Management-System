package Model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Supplier implements Serializable {
    private static final long serialVersionUID = 1L;

    private String supplierName;  // Regular String for serialization
    private String contactDetails;  // Regular String for serialization
    private List<Item> suppliedItems;

    // Transient StringProperties for JavaFX binding (not serialized)
    private transient StringProperty supplierNameProperty;
    private transient StringProperty contactDetailsProperty;

    // Constructor to initialize Supplier
    public Supplier(String supplierName, String contactDetails) {
        this.supplierName = supplierName;
        this.contactDetails = contactDetails;
        this.suppliedItems = new ArrayList<>();
        initializeProperties();  // Initialize StringProperties for UI binding
    }

    // Initialize StringProperties after deserialization or object creation
    private void initializeProperties() {
        supplierNameProperty = new SimpleStringProperty(supplierName);
        contactDetailsProperty = new SimpleStringProperty(contactDetails);
    }

    // Getter and setter for regular String fields
    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
        supplierNameProperty.set(supplierName);  // Update StringProperty for UI binding
    }

    public String getContactDetails() {
        return contactDetails;
    }

    public void setContactDetails(String contactDetails) {
        this.contactDetails = contactDetails;
        contactDetailsProperty.set(contactDetails);  // Update StringProperty for UI binding
    }

    public StringProperty contactDetailsProperty() {
        if (contactDetailsProperty == null) {
            initializeProperties();
        }
        return contactDetailsProperty;
    }

    // Add an item to the supplied items list
    public void addItem(Item item) {
        if (suppliedItems.stream().noneMatch(existing -> existing.getItemName().equalsIgnoreCase(item.getItemName())))
            suppliedItems.add(item);
    }

    // Get the list of supplied items
    public List<Item> getSuppliedItems() {
        return new ArrayList<>(suppliedItems);
    }

    // Custom serialization to handle non-serializable StringProperty
    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();  // Serialize the regular fields (String supplierName and contactDetails)
    }

    // Custom deserialization to handle non-serializable StringProperty
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();  // Deserialize the regular fields
        initializeProperties();   // Reinitialize the StringProperties after deserialization
    }

    @Override
    public String toString() {
        return supplierName + " - " + contactDetails;
    }
}
