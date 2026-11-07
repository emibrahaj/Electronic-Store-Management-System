package DAO;

import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Utility class for secure password hashing and verification using PBKDF2.
 * Passwords are hashed with a random salt, making rainbow table attacks infeasible.
 */
public class PasswordManager {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA1";
    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;

    /**
     * Hashes a plain text password with a random salt using PBKDF2.
     *
     * @param password the plain text password to hash
     * @return a string in format "salt:hashedPassword" encoded in Base64
     * @throws Exception if hashing fails
     */
    public static String hashPassword(String password) throws Exception {
        byte[] salt = generateSalt();
        byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);

        // Encode salt and hash, return as "salt:hash" for storage
        String saltStr = Base64.getEncoder().encodeToString(salt);
        String hashStr = Base64.getEncoder().encodeToString(hash);

        return saltStr + ":" + hashStr;
    }

    /**
     * Verifies that a plain text password matches a stored hash.
     *
     * @param password the plain text password to verify
     * @param storedHash the stored hash in format "salt:hash"
     * @return true if password matches the hash, false otherwise
     * @throws Exception if verification fails
     */
    public static boolean verifyPassword(String password, String storedHash) throws Exception {
        try {
            String[] parts = storedHash.split(":");
            if (parts.length != 2) {
                System.err.println("Invalid hash format");
                return false;
            }

            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] storedHashBytes = Base64.getDecoder().decode(parts[1]);

            byte[] computedHash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);

            return slowEquals(computedHash, storedHashBytes);
        } catch (Exception e) {
            System.err.println("Error verifying password: " + e.getMessage());
            return false;
        }
    }

    /**
     * Generates a random salt for password hashing.
     *
     * @return a byte array containing the random salt
     */
    private static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }

    /**
     * Applies PBKDF2 key derivation function to password and salt.
     *
     * @param password the password characters
     * @param salt the salt bytes
     * @param iterations number of iterations
     * @param keyLength desired key length in bits
     * @return the derived key bytes
     * @throws Exception if derivation fails
     */
    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength)
            throws Exception {
        KeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
        return factory.generateSecret(spec).getEncoded();
    }

    /**
     * Timing-safe comparison of two byte arrays to prevent timing attacks.
     *
     * @param a first byte array
     * @param b second byte array
     * @return true if arrays are equal, false otherwise
     */
    private static boolean slowEquals(byte[] a, byte[] b) {
        int diff = a.length ^ b.length;
        for (int i = 0; i < a.length && i < b.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }
}
