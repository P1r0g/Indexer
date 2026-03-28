package com.example.core;


import com.example.api.FileIndexService;
import com.example.api.Tokenizer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

public class InMemoryFileIndexService implements FileIndexService {

    private final Tokenizer tokenizer;
    private final IndexStorage storage;
    private final ReadWriteLock lock;
    private final FileSystemWatcher watcher;

    public InMemoryFileIndexService(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.storage = new IndexStorage();
        this.lock = new ReentrantReadWriteLock();

        this.watcher = new FileSystemWatcher(new FileSystemWatcher.FileChangeListener() {
            @Override
            public void onFileCreated(Path file) {
                safelyIndexFile(file);
            }

            @Override
            public void onFileModified(Path file) {
                safelyReindexFile(file);
            }

            @Override
            public void onFileDeleted(Path file) {
                safelyRemoveFile(file);
            }

            @Override
            public void onDirectoryCreated(Path directory) {
                addDirectory(directory);
            }
        });
    }

    @Override
    public void addFile(Path file) {
        Path normalizedFile = normalize(file);
        validateRegularFile(normalizedFile);

        String content = readFileContent(normalizedFile);
        Set<String> words = tokenizer.tokenize(content);

        lock.writeLock().lock();
        try {
            storage.indexFile(normalizedFile, words);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void addDirectory(Path directory) {
        Path normalizedDirectory = normalize(directory);

        if (!Files.exists(normalizedDirectory)) {
            throw new IllegalArgumentException("Директория не найдена: " + normalizedDirectory);
        }

        if (!Files.isDirectory(normalizedDirectory)) {
            throw new IllegalArgumentException("Путь не является директорией: " + normalizedDirectory);
        }

        try (Stream<Path> paths = Files.walk(normalizedDirectory)) {
            paths.filter(Files::isRegularFile)
                    .forEach(this::addFile);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка индексации директории: " + normalizedDirectory, e);
        }

        watcher.registerDirectoryRecursively(normalizedDirectory);
    }

    @Override
    public Set<Path> search(String word) {
        if (word == null || word.isBlank()) {
            return Set.of();
        }

        String normalizedWord = word.toLowerCase(Locale.ROOT);

        lock.readLock().lock();
        try {
            return storage.search(normalizedWord);
        } finally {
            lock.readLock().unlock();
        }
    }

    private void reindexFile(Path file) {
        Path normalizedFile = normalize(file);

        if (!Files.exists(normalizedFile) || !Files.isRegularFile(normalizedFile)) {
            removeFileFromIndex(normalizedFile);
            return;
        }

        String content = readFileContent(normalizedFile);
        Set<String> words = tokenizer.tokenize(content);

        lock.writeLock().lock();
        try {
            storage.indexFile(normalizedFile, words);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void removeFileFromIndex(Path file) {
        Path normalizedFile = normalize(file);

        lock.writeLock().lock();
        try {
            storage.removeFile(normalizedFile);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void safelyIndexFile(Path file) {
        try {
            if (Files.exists(file) && Files.isRegularFile(file)) {
                addFile(file);
            }
        } catch (Exception ignored) {
        }
    }

    private void safelyReindexFile(Path file) {
        try {
            reindexFile(file);
        } catch (Exception ignored) {
        }
    }

    private void safelyRemoveFile(Path file) {
        try {
            removeFileFromIndex(file);
        } catch (Exception ignored) {
        }
    }

    private void validateRegularFile(Path file) {
        if (!Files.exists(file)) {
            throw new IllegalArgumentException("Файл не найден: " + file);
        }

        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Путь не является файлом: " + file);
        }
    }

    private String readFileContent(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка чтения файла: " + file, e);
        }
    }

    private Path normalize(Path path) {
        return path.toAbsolutePath().normalize();
    }

    @Override
    public void close() {
        watcher.close();
    }
}
