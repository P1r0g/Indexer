
import com.example.api.FileIndexService;
import com.example.core.InMemoryFileIndexService;
import com.example.tokenizers.SimpleTextTokenize;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WatcherIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldIndexNewFileCreatedAfterDirectoryRegistration() throws Exception {
        try (FileIndexService service = new InMemoryFileIndexService(new SimpleTextTokenize())) {
            service.addDirectory(tempDir);

            Path file = tempDir.resolve("new.txt");
            Files.writeString(file, "dynamic java file");

            waitUntil(Duration.ofSeconds(3), () -> !service.search("java").isEmpty());

            assertTrue(service.search("java").contains(file.toAbsolutePath().normalize()));
        }
    }

    @Test
    void shouldReindexModifiedFile() throws Exception {
        Path file = tempDir.resolve("a.txt");
        Files.writeString(file, "java");

        try (FileIndexService service = new InMemoryFileIndexService(new SimpleTextTokenize())) {
            service.addDirectory(tempDir);

            Files.writeString(file, "python");

            waitUntil(
                    Duration.ofSeconds(3),
                    () -> service.search("python").contains(file.toAbsolutePath().normalize())
            );

            assertTrue(service.search("python").contains(file.toAbsolutePath().normalize()));
        }
    }

    private void waitUntil(Duration timeout, Condition condition) throws Exception {
        long deadline = System.currentTimeMillis() + timeout.toMillis();

        while (System.currentTimeMillis() < deadline) {
            if (condition.matches()) {
                return;
            }
            Thread.sleep(100);
        }

        throw new AssertionError("Времы истекло");
    }

    @FunctionalInterface
    interface Condition {
        boolean matches() throws Exception;
    }
}