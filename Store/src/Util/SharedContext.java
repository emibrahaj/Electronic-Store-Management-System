package Util;

import Model.Inventory;
import java.util.ArrayList;
import java.util.List;
import DAO.BillDAO;
import Model.Bill;

/**
 * Shared application context containing resources needed across controllers.
 * Implements singleton pattern to ensure single instance throughout application lifetime.
 */
public class SharedContext {
    private static SharedContext instance;
    private Inventory inventory;
    private List<Bill> bills;
    private String currentUsername;
    private String currentUserRole;

    // Private constructor for singleton pattern
    private SharedContext() {
        initializeResources();
    }

    /**
     * Gets the singleton instance of SharedContext.
     * Creates one if it doesn't exist yet.
     *
     * @return the SharedContext instance
     */
    public static synchronized SharedContext getInstance() {
        if (instance == null) {
            instance = new SharedContext();
        }
        return instance;
    }

    /**
     * Initializes shared resources (inventory, bills, etc.)
     */
    private void initializeResources() {
        this.inventory = DAO.InventoryDAO.loadInventory();
        this.bills = new ArrayList<>(BillDAO.loadBills());
    }

    /**
     * Gets the shared inventory instance.
     *
     * @return the inventory object, never null
     */
    public Inventory getInventory() {
        if (inventory == null) {
            inventory = new Inventory();
        }
        return inventory;
    }

    /**
     * Sets the inventory instance.
     *
     * @param inventory the inventory to set
     */
    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    /**
     * Gets the list of bills.
     *
     * @return the bills list, never null
     */
    public List<Bill> getBills() {
        if (bills == null) {
            bills = new ArrayList<>(BillDAO.loadBills());
        }
        return bills;
    }

    /**
     * Sets the bills list.
     *
     * @param bills the bills list to set
     */
    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    /**
     * Gets the currently logged-in username.
     *
     * @return the username, or empty string if not logged in
     */
    public String getCurrentUsername() {
        return currentUsername != null ? currentUsername : "";
    }

    /**
     * Sets the currently logged-in username.
     *
     * @param username the username to set
     */
    public void setCurrentUsername(String username) {
        this.currentUsername = username;
    }

    /**
     * Gets the current user's role.
     *
     * @return the role, or empty string if not set
     */
    public String getCurrentUserRole() {
        return currentUserRole != null ? currentUserRole : "";
    }

    /**
     * Sets the current user's role.
     *
     * @param role the role to set
     */
    public void setCurrentUserRole(String role) {
        this.currentUserRole = role;
    }

    /**
     * Checks if a user is currently logged in.
     *
     * @return true if username is set and not empty
     */
    public boolean isLoggedIn() {
        return currentUsername != null && !currentUsername.isEmpty();
    }

    /**
     * Clears all user context data (logout).
     */
    public void clearUserContext() {
        currentUsername = null;
        currentUserRole = null;
    }

    /**
     * Resets the entire context (for testing or application restart).
     */
    public synchronized void reset() {
        instance = null;
    }
}
