
import com.example.tokenizers.SimpleTextTokenize;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleTextTokenizerTest {

    @Test
    void shouldSplitTextIntoWords() {
        SimpleTextTokenize tokenizer = new SimpleTextTokenize();

        Set<String> result = tokenizer.tokenize("Hello, Java world! Java_2025");

        assertEquals(Set.of("hello", "java", "world", "2025"), result);
    }

    @Test
    void shouldReturnEmptySetForBlankText() {
        SimpleTextTokenize tokenizer = new SimpleTextTokenize();

        assertEquals(Set.of(), tokenizer.tokenize("   "));
    }
}