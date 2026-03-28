package com.example.core;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class IndexStorage {

    private final Map<String, Set<Path>> wordToFiles = new HashMap<>();
    private final Map<Path, Set<String>> fileToWords = new HashMap<>();

    public void indexFile(Path file, Set<String> words) {
        removeFile(file);

        fileToWords.put(file, new HashSet<>(words));

        for (String word : words) {
            wordToFiles
                    .computeIfAbsent(word, key -> new HashSet<>())
                    .add(file);
        }
    }

    public void removeFile(Path file) {
        Set<String> existingWords = fileToWords.remove(file);
        if (existingWords == null) {
            return;
        }

        for (String word : existingWords) {
            Set<Path> files = wordToFiles.get(word);
            if (files == null) {
                continue;
            }

            files.remove(file);
            if (files.isEmpty()) {
                wordToFiles.remove(word);
            }
        }
    }

    public Set<Path> search(String word) {
        Set<Path> files = wordToFiles.get(word);
        if (files == null) {
            return Set.of();
        }
        return Collections.unmodifiableSet(new HashSet<>(files));
    }
}