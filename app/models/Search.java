package models;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.pekko.actor.ActorRef;
import play.libs.ws.WSClient;

import java.time.Duration;
import java.util.ArrayList;
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
            this.fleshReadingScore = 0.0; // TODO
            this.fleshKincaidGradeLevel = 0.0; //TODO
            this.tags = null; // TODO
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
                                            .thenCompose(video -> {
                                                        System.out.println("video is not null" + video.getVideoId());
                                                        return ask(storeActor, new StoreActor.GetChannel(video.getChannelId()), Duration.ofSeconds(50))
                                                                .thenCompose(channelRes -> (CompletionStage<Channel>) channelRes)
                                                                .thenApply(channel -> new SearchResult(video, channel));
                                                    }
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
