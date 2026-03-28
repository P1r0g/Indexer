package com.example.api;


import java.nio.file.Path;
import java.util.Set;

public interface FileIndexService extends AutoCloseable {

    void addFile(Path file);

    void addDirectory(Path directory);

    Set<Path> search(String word);

    @Override
    void close();
}
