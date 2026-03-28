
import com.example.api.FileIndexService;
import com.example.core.InMemoryFileIndexService;
import com.example.tokenizers.SimpleTextTokenize;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InMemoryFileIndexServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldIndexSingleFile() throws IOException {
        Path file = tempDir.resolve("a.txt");
        Files.writeString(file, "Java Maven");

        try (FileIndexService service = new InMemoryFileIndexService(new SimpleTextTokenize())) {
            service.addFile(file);

            assertEquals(Set.of(file.toAbsolutePath().normalize()), service.search("java"));
        }
    }

    @Test
    void shouldIndexDirectoryRecursively() throws IOException {
        Path subdir = tempDir.resolve("sub");
        Files.createDirectories(subdir);

        Path file1 = tempDir.resolve("a.txt");
        Path file2 = subdir.resolve("b.txt");

        Files.writeString(file1, "Java");
        Files.writeString(file2, "Java Gradle");

        try (FileIndexService service = new InMemoryFileIndexService(new SimpleTextTokenize())) {
            service.addDirectory(tempDir);

            assertEquals(
                    Set.of(
                            file1.toAbsolutePath().normalize(),
                            file2.toAbsolutePath().normalize()
                    ),
                    service.search("java")
            );
        }
    }

    @Test
    void shouldReturnEmptySetForUnknownWord() throws IOException {
        Path file = tempDir.resolve("a.txt");
        Files.writeString(file, "Java");

        try (FileIndexService service = new InMemoryFileIndexService(new SimpleTextTokenize())) {
            service.addFile(file);

            assertEquals(Set.of(), service.search("python"));
        }
    }
}