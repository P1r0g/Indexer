package com.example.tokenizers;

import com.example.api.Tokenizer;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class SimpleTextTokenize implements Tokenizer {

    @Override
    public Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{Nd}]+"))
                .filter(token -> !token.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
