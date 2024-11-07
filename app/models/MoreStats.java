package models;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MoreStats {
    private final List<Map<String, String>> countedWords;

    private MoreStats(List<Map<String, String>> countedWords) {
        this.countedWords = countedWords;
    }

    static MoreStats create(Search search) {
        List<Map<String, String>> countedWords = countWords(search.getSearchResults().stream().map(s -> s.video.getDescription()).collect(Collectors.toList()));
        return new MoreStats(countedWords);
    }

    private static List<Map<String, String>> countWords(List<String> descriptions) {
        return descriptions.stream()
                .flatMap(desc -> Arrays.stream(desc.split("\\W+"))) // Split by non-word characters
                .map(String::toLowerCase)                           // Normalize to lowercase
                .filter(word -> !word.isEmpty())                    // Filter out empty words
                .collect(Collectors.groupingBy(
                        word -> word,
                        Collectors.summingInt(e -> 1)))             // Count occurrences of each word
                .entrySet().stream()
                .sorted((entry1, entry2) -> Integer.compare(entry2.getValue(), entry1.getValue())) // Sort by descending count
                .map(entry -> {
                    // Create a map with String keys and String values
                    Map<String, String> map = new HashMap<>();
                    map.put("word", entry.getKey());                     // Word as String
                    map.put("count", String.valueOf(entry.getValue()));  // Count as String
                    return map;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, String>> getCountedWords() {
        return countedWords;
    }
}
