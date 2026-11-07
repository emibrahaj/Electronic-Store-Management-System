package DAO;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.*;

/** Replaces a file only after its complete replacement has been written. */
public final class AtomicFiles {
    private AtomicFiles() { }

    public static void replace(Path temporary, Path target) throws IOException {
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void writeObject(Path target, Object value) throws IOException {
        Files.createDirectories(target.toAbsolutePath().getParent());
        Path temporary = Files.createTempFile(target.toAbsolutePath().getParent(), "store-", ".tmp");
        try {
            try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(temporary))) {
                output.writeObject(value);
            }
            replace(temporary, target);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
