package models;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.ws.WSClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

public class Search {
    private final String searchTerm;
    private final List<String> videoIds;

    private static String apiUrl = "https://www.googleapis.com/youtube/v3/search";
    private static String apiKey = "AIzaSyBUo0A_y27wxO2GHtEO0Uoji1ND8Os1z9Q";

    private Search(String searchTerm, List<String> videoIds) {
        this.searchTerm = searchTerm;
        this.videoIds = videoIds;
    }

    public static CompletionStage<Search> create(String searchTerm, int maxResults, WSClient wsClient) {
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

                    return new Search(searchTerm, videoIds);

                });
    }

    public List<String> getVideoIds() {
        return videoIds;
    }

}
