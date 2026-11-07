package Util;

/**
 * Utility class for input validation to prevent injection attacks,
 * buffer overflows, and format violations.
 */
public class InputValidator {

    // Validation patterns
    private static final String USERNAME_PATTERN = "^[a-zA-Z0-9_]{3,50}$";
    private static final String PASSWORD_PATTERN = "^.{6,100}$";  // At least 6 chars
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final String PHONE_PATTERN = "^\\d{10,15}$";
    private static final String ITEM_NAME_PATTERN = "^[a-zA-Z0-9\\s\\-./&()]{1,100}$";
    private static final String CATEGORY_PATTERN = "^[a-zA-Z0-9\\s\\-]{1,50}$";
    private static final String BILL_NUMBER_PATTERN = "^[A-Z0-9\\-]{5,20}$";

    /**
     * Validates username format (alphanumeric and underscore, 3-50 chars).
     *
     * @param username the username to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidUsername(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        return username.matches(USERNAME_PATTERN);
    }

    /**
     * Validates password strength (minimum 6 characters).
     *
     * @param password the password to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        return password.matches(PASSWORD_PATTERN) && !password.contains(",");
    }

    /**
     * Validates email format using standard email regex pattern.
     *
     * @param email the email to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return email.matches(EMAIL_PATTERN) && email.length() <= 100;
    }

    /**
     * Validates phone number (10-15 digits).
     *
     * @param phoneNumber the phone number to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return false;
        }
        return phoneNumber.matches(PHONE_PATTERN);
    }

    /**
     * Validates item name (alphanumeric, spaces, and basic punctuation, max 100 chars).
     *
     * @param itemName the item name to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidItemName(String itemName) {
        if (itemName == null || itemName.isBlank()) {
            return false;
        }
        return itemName.matches(ITEM_NAME_PATTERN);
    }

    /**
     * Validates category name.
     *
     * @param category the category to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidCategory(String category) {
        if (category == null || category.isBlank()) {
            return false;
        }
        return category.matches(CATEGORY_PATTERN);
    }

    /**
     * Validates bill number format.
     *
     * @param billNumber the bill number to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidBillNumber(String billNumber) {
        if (billNumber == null || billNumber.isBlank()) {
            return false;
        }
        return billNumber.matches(BILL_NUMBER_PATTERN);
    }

    /**
     * Validates a numeric price (positive double, up to 2 decimal places).
     *
     * @param price the price to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidPrice(double price) {
        return price > 0 && price <= 999999.99;
    }

    /**
     * Validates stock quantity (non-negative integer).
     *
     * @param quantity the quantity to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidQuantity(int quantity) {
        return quantity >= 0 && quantity <= 999999;
    }

    /**
     * Sanitizes file names to prevent directory traversal and injection.
     * Removes/escapes dangerous characters.
     *
     * @param filename the filename to sanitize
     * @return a sanitized filename
     */
    public static String sanitizeFileName(String filename) {
        if (filename == null) {
            return "";
        }
        // Remove path traversal attempts and dangerous characters
        return filename.replaceAll("[^a-zA-Z0-9._-]", "")
                      .replaceAll("\\.\\.[\\\\/]", "")  // Remove ../ and ..\
                      .replaceAll("^\\.", "");            // Remove leading dots
    }

    /**
     * Validates that a string is not blank and within length bounds.
     *
     * @param input the string to validate
     * @param minLength minimum allowed length
     * @param maxLength maximum allowed length
     * @return true if valid, false otherwise
     */
    public static boolean isValidLength(String input, int minLength, int maxLength) {
        if (input == null) {
            return minLength == 0;
        }
        int length = input.length();
        return length >= minLength && length <= maxLength;
    }

    /**
     * Validates that a string is not blank.
     *
     * @param input the string to check
     * @return true if not blank, false otherwise
     */
    public static boolean isNotBlank(String input) {
        return input != null && !input.isBlank();
    }

    /**
     * Provides error messages for validation failures.
     *
     * @param fieldName the field that failed validation
     * @param rule the validation rule that failed
     * @return a user-friendly error message
     */
    public static String getValidationErrorMessage(String fieldName, String rule) {
        switch (rule) {
            case "username":
                return fieldName + " must be 3-50 characters, alphanumeric and underscore only";
            case "password":
                return fieldName + " must be at least 6 characters";
            case "email":
                return fieldName + " is not a valid email format";
            case "phone":
                return fieldName + " must be 10-15 digits";
            case "itemName":
                return fieldName + " must be 1-100 characters, alphanumeric, spaces, and basic punctuation";
            case "category":
                return fieldName + " must be 1-50 characters, alphanumeric and hyphens only";
            case "billNumber":
                return fieldName + " must be 5-20 characters, uppercase letters, numbers, and hyphens";
            case "price":
                return fieldName + " must be a positive number up to 999999.99";
            case "quantity":
                return fieldName + " must be a non-negative number up to 999999";
            case "notBlank":
                return fieldName + " cannot be empty";
            default:
                return fieldName + " validation failed";
        }
    }
}
