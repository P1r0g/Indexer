package com.example.core;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

class FileSystemWatcher implements Closeable {

    interface FileChangeListener {
        void onFileCreated(Path file);
        void onFileModified(Path file);
        void onFileDeleted(Path file);
        void onDirectoryCreated(Path directory);
    }

    private final WatchService watchService;
    private final Map<WatchKey, Path> keysToDirectories = new HashMap<>();
    private final Thread watcherThread;
    private final FileChangeListener listener;
    private volatile boolean running = true;

    public FileSystemWatcher(FileChangeListener listener) {
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            throw new RuntimeException("Ошибка создания WatchService", e);
        }

        this.listener = listener;
        this.watcherThread = new Thread(this::processEvents, "file-system-watcher");
        this.watcherThread.setDaemon(true);
        this.watcherThread.start();
    }

    public void registerDirectoryRecursively(Path rootDirectory) {
        if (!Files.exists(rootDirectory) || !Files.isDirectory(rootDirectory)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(rootDirectory)) {
            paths.filter(Files::isDirectory).forEach(this::registerDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка рекурсивной регистрации директории: " + rootDirectory, e);
        }
    }

    private void registerDirectory(Path directory) {
        try {
            WatchKey key = directory.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE
            );
            keysToDirectories.put(key, directory.toAbsolutePath().normalize());
        } catch (IOException e) {
            throw new RuntimeException("Ошибка регистрации директории: " + directory, e);
        }
    }

    @SuppressWarnings("unchecked")
    private void processEvents() {
        while (running) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                if (running) {
                    e.printStackTrace();
                }
                return;
            }

            Path parentDirectory = keysToDirectories.get(key);
            if (parentDirectory == null) {
                key.reset();
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }

                Path relativePath = ((WatchEvent<Path>) event).context();
                Path fullPath = parentDirectory.resolve(relativePath).toAbsolutePath().normalize();

                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                    handleCreate(fullPath);
                } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                    handleModify(fullPath);
                } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                    handleDelete(fullPath);
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                keysToDirectories.remove(key);
            }
        }
    }

    private void handleCreate(Path path) {
        if (Files.isDirectory(path)) {
            registerDirectoryRecursively(path);
            listener.onDirectoryCreated(path);
        } else {
            listener.onFileCreated(path);
        }
    }

    private void handleModify(Path path) {
        if (Files.isRegularFile(path)) {
            listener.onFileModified(path);
        }
    }

    private void handleDelete(Path path) {
        listener.onFileDeleted(path);
    }

    @Override
    public void close() {
        running = false;
        watcherThread.interrupt();
        try {
            watchService.close();
        } catch (IOException e) {
            throw new RuntimeException("Ошибка удаления WatchService", e);
        }
    }
}