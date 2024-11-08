package models;

import com.typesafe.config.Config;
import org.apache.pekko.actor.ActorRef;
import play.libs.ws.WSClient;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public class MoreStats {
    private final Map<String, Integer> countedWords;

    private MoreStats(Map<String, Integer> countedWords) {
        this.countedWords = countedWords;
    }

    static CompletionStage<MoreStats> create(String searchTerm, int maxResults, WSClient wsClient, ActorRef storeActor, Config config) {
        return Search.create(searchTerm, maxResults, wsClient, storeActor, config)
                .thenApply(search -> {
                    Map<String, Integer> countedWords = countWords(
                            search.getSearchResults().stream().map(s -> s.video.getDescription()).collect(Collectors.toList()));
                    return new MoreStats(countedWords);
                });
    }

    private static Map<String, Integer> countWords(List<String> descriptions) {
        return descriptions.stream()
                .flatMap(desc -> Arrays.stream(desc.split("\\W+"))) // Split by non-word characters
                .map(String::toLowerCase)                           // Normalize to lowercase
                .filter(word -> !word.isEmpty())                    // Filter out empty words
                .collect(Collectors.toMap(
                        word -> word,                               // Word as key
                        word -> 1,                                  // Initial count of 1
                        Integer::sum                                // Sum counts of duplicate words
                ));
    }

    public Map<String, Integer> getCountedWords() {
        return countedWords;
    }
}
