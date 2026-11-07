package DAO;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public final class PasswordMigrationUtil {
    private PasswordMigrationUtil() { }

    public static void migratePasswordsToHash() throws Exception {
        Path source = AppPaths.daoFile("passwords.txt");
        List<String> lines = Files.readAllLines(source);
        List<String> migrated = new ArrayList<>();
        for (String line : lines) {
            if (line.isBlank()) { migrated.add(line); continue; }
            String[] fields = line.split(",", -1);
            if (fields.length < 5) throw new IllegalArgumentException("Invalid credential record; migration cancelled.");
            if (!fields[1].contains(":")) fields[1] = PasswordManager.hashPassword(fields[1]);
            migrated.add(String.join(",", fields));
        }
        // Never overwrite an earlier backup, which may be needed for recovery.
        Path backup = source.resolveSibling("passwords.txt.backup");
        if (!Files.exists(backup)) Files.copy(source, backup);
        Path temporary = Files.createTempFile(source.getParent(), "passwords-", ".tmp");
        try {
            Files.write(temporary, migrated);
            AtomicFiles.replace(temporary, source);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static void main(String[] args) throws Exception {
        migratePasswordsToHash();
    }
}
