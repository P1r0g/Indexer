package com.example;

import com.example.api.FileIndexService;
import com.example.core.InMemoryFileIndexService;
import com.example.tokenizers.SimpleTextTokenize;

import java.nio.file.Path;
import java.util.Scanner;
import java.util.Set;

public class Main {

    public static void main(String[] args) {
        try (FileIndexService service = new InMemoryFileIndexService(new SimpleTextTokenize());
             Scanner scanner = new Scanner(System.in)) {

            printHelp();

            while (true) {
                System.out.print("> ");
                String line = scanner.nextLine();

                if (line == null || line.isBlank()) {
                    continue;
                }

                String[] parts = line.trim().split("\\s+", 2);
                String command = parts[0];

                try {
                    switch (command) {
                        case "add-file" -> {
                            ensureArgument(parts);
                            service.addFile(Path.of(parts[1]));
                            System.out.println("Индексация файла.");
                        }
                        case "add-dir" -> {
                            ensureArgument(parts);
                            service.addDirectory(Path.of(parts[1]));
                            System.out.println("Директория индексирована и watcher зарегистрирован.");
                        }
                        case "search" -> {
                            ensureArgument(parts);
                            Set<Path> result = service.search(parts[1]);
                            if (result.isEmpty()) {
                                System.out.println("Ничего не найдено.");
                            } else {
                                System.out.println("Найдено в:");
                                result.forEach(path -> System.out.println(" - " + path));
                            }
                        }
                        case "help" -> printHelp();
                        case "exit" -> {
                            System.out.println("Выход.");
                            return;
                        }
                        default -> System.out.println("Неизвестная команда. Введите 'help'.");
                    }
                } catch (Exception e) {
                    System.out.println("Ошибка: " + e.getMessage());
                }
            }
        }
    }

    private static void ensureArgument(String[] parts) {
        if (parts.length < 2 || parts[1].isBlank()) {
            throw new IllegalArgumentException("Argument is required");
        }
    }

    private static void printHelp() {
        System.out.println("Команды:");
        System.out.println("  add-file <path>   - добавить файл для индексации");
        System.out.println("  add-dir <path>    - добавить директорию для индексации");
        System.out.println("  search <word>     - поиск файлов по слову");
        System.out.println("  help              - посмотреть команды");
        System.out.println("  exit              - выход");
    }
}