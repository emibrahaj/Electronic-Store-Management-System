package DAO;

import java.nio.file.Files;
import java.nio.file.Path;

public final class AppPaths {
    private AppPaths() { }

    public static Path daoFile(String fileName) {
        return storeRoot().resolve("src/DAO").resolve(fileName);
    }

    public static Path printableBillsFolder() {
        return storeRoot().resolve("src/Database/PrintableBills");
    }

    private static Path storeRoot() {
        String configured = System.getProperty("store.home");
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured).toAbsolutePath().normalize();
        }
        Path current = Path.of("").toAbsolutePath();
        for (Path candidate = current; candidate != null; candidate = candidate.getParent()) {
            if (Files.isDirectory(candidate.resolve("src/DAO"))) return candidate;
            if (Files.isDirectory(candidate.resolve("Store/src/DAO"))) return candidate.resolve("Store");
        }
        try {
            Path location = Path.of(AppPaths.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            for (Path candidate = location; candidate != null; candidate = candidate.getParent()) {
                if (Files.isDirectory(candidate.resolve("src/DAO"))) return candidate;
            }
        } catch (Exception ignored) { }
        return Path.of(System.getProperty("user.home"), ".storepos");
    }
}
