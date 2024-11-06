package models;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.pekko.actor.ActorRef;
import play.libs.ws.WSClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static org.apache.pekko.pattern.Patterns.ask;

public class Search {
    private final String searchTerm;
    private final double avgFleshKincaidGradeLevel;
    private final double avgFleshReadingScore;
    private final List<SearchResult> searchResults;

    public static class SearchResult {
        public Video video;
        public Channel channel;
        public double fleshKincaidGradeLevel;
        public Double fleshReadingScore;
        public List<String> tags;

        public SearchResult(Video video, Channel channel) {
            this.video = video;
            this.channel = channel;
            this.tags = null; // TODO
            calculateReadability(video.getDescription());
        }

        private void calculateReadability(String text)  {
            int wordCount = countWords(text);
            int sentenceCount = countSentences(text);
            int syllableCount = countSyllables(text);

            double averageWordsPerSentence = (double) wordCount / sentenceCount;
            double averageSyllablesPerWord = (double) syllableCount / wordCount;

            double fleschReadingEase = 206.835 - (1.015 * averageWordsPerSentence) - (84.6 * averageSyllablesPerWord);
            fleschReadingEase = Math.max(fleschReadingEase, 0);
            double fleschKincaidGradeLevel = (0.39 * averageWordsPerSentence) + (11.8 * averageSyllablesPerWord) - 15.59;
            fleschKincaidGradeLevel = Math.max(fleschKincaidGradeLevel, 0);

            this.fleshReadingScore = fleschReadingEase;
            this.fleshKincaidGradeLevel = fleschKincaidGradeLevel;
        }
        // Counts the words in a text
        private int countWords(String text) {
            return text.split("\\s+").length;
        }

        // Counts the sentences in a text
        private int countSentences(String text) {
            return text.split("[.!?]").length;
        }

        // Counts the syllables in a text
        private int countSyllables(String text) {
            return Arrays.stream(text.split("\\s+")) // Convert array to stream
                    .mapToInt(this::countSyllablesInWord) // Apply syllable count to each word
                    .sum();
        }

        // Counts syllables in a single word
        private int countSyllablesInWord(String word) {
            String lowerWord = word.toLowerCase();
            String vowels = "aeiouy";  // Define vowels
            int count = 0;
            boolean lastWasVowel = false;  // Flag to track the last character's vowel status

            for (int i = 0; i < lowerWord.length(); i++) {
                char c = lowerWord.charAt(i);
                if (vowels.indexOf(c) != -1) {  // Check if the character is a vowel
                    if (!lastWasVowel) {
                        count += 1;
                        lastWasVowel = true;
                    }
                } else {
                    lastWasVowel = false;
                }
            }

            // Special rule: if the word ends with "e", subtract 1 syllable
            if (lowerWord.endsWith("e")) {
                count -= 1;
            }

            // Ensure there's at least one syllable
            return Math.max(count, 1);
        }
    }

    private static String apiUrl = "https://www.googleapis.com/youtube/v3/search";
    private static String apiKey = "AIzaSyBUo0A_y27wxO2GHtEO0Uoji1ND8Os1z9Q";

    private Search(String searchTerm, List<SearchResult> searchResults) {
        this.searchTerm = searchTerm;
        this.searchResults = searchResults;
        this.avgFleshKincaidGradeLevel = searchResults.stream().mapToDouble(searchResult -> searchResult.fleshKincaidGradeLevel).average().orElse(0.0);
        this.avgFleshReadingScore = searchResults.stream().mapToDouble(searchResult -> searchResult.fleshReadingScore).average().orElse(0.0);
    }

    static CompletionStage<Search> create(String searchTerm, int maxResults, WSClient wsClient, ActorRef storeActor) {
        return wsClient.url(apiUrl)
                .addQueryParameter("part", "snippet")
                .addQueryParameter("maxResults", "10")
                .addQueryParameter("q", searchTerm)
                .addQueryParameter("type", "video")
                .addQueryParameter("key", apiKey)
                .get()
                .thenApply(wsResponse -> {
                    JsonNode items = wsResponse.asJson().get("items");

                    List<String> videoIds = new ArrayList<>();

                    if (items != null && items.isArray()) {
                        items.forEach(itemNode -> videoIds.add(itemNode.get("id").get("videoId").asText()));
                    }

                    return videoIds;

//                    return new Search(searchTerm, videoIds);

                })
                .thenCompose(videoIds -> {
                    List<CompletionStage<SearchResult>> searchResultFutures = videoIds.stream().map(videoId ->
                                    ask(storeActor, new StoreActor.GetVideo(videoId), Duration.ofSeconds(50))
                                            .thenCompose(videoRes -> (CompletionStage<Video>) videoRes)
                                            .thenCompose(video -> ask(storeActor, new StoreActor.GetChannel(video.getChannelId()), Duration.ofSeconds(50))
                                                    .thenCompose(channelRes -> (CompletionStage<Channel>) channelRes)
                                                    .thenApply(channel -> new SearchResult(video, channel))
                                            )
                            )
                            .collect(Collectors.toList());

                    return CompletableFuture.allOf(
                                    searchResultFutures.stream()
                                            .map(CompletionStage::toCompletableFuture)
                                            .toArray(CompletableFuture[]::new)
                            )
                            .thenApply(v ->
                                    searchResultFutures.stream()
                                            .map(CompletionStage::toCompletableFuture)
                                            .map(CompletableFuture::join)
                                            .collect(Collectors.toList())
                            );
                })
                .thenApply(searchResList -> new Search(searchTerm, searchResList));
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public double getAvgFleshKincaidGradeLevel() {
        return avgFleshKincaidGradeLevel;
    }

    public double getAvgFleshReadingScore() {
        return avgFleshReadingScore;
    }

    public List<SearchResult> getSearchResults() {
        return searchResults;
    }
}
