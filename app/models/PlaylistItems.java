package models;

import java.util.ArrayList;
import java.util.concurrent.CompletionStage;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.ws.WSClient;

import java.util.List;

public class PlaylistItems {
    private final String playlistId;
    private final List<String> videoIds;

    private static String apiUrl = "https://www.googleapis.com/youtube/v3/playlistItems";
    private static String apiKey = "AIzaSyBUo0A_y27wxO2GHtEO0Uoji1ND8Os1z9Q";

    private PlaylistItems(String playlistId, List<String> videoIds) {
        this.playlistId = playlistId;
        this.videoIds = videoIds;
    }

    public static CompletionStage<PlaylistItems> create(String playlistId, WSClient wsClient) {
        return wsClient.url(apiUrl)
                .addQueryParameter("part", "snippet")
                .addQueryParameter("playlistId", playlistId)
                .addQueryParameter("maxResults", "10")
                .addQueryParameter("key", apiKey)
                .get()
                .thenApply(wsResponse -> {
                    JsonNode json = wsResponse.asJson();
                    JsonNode items = json.get("items");

                    List<String> videoIds = new ArrayList<>();

                    if (items != null && items.isArray()) {
                        items.forEach(itemNode -> videoIds.add(itemNode.get("snippet").get("resourceId").get("videoId").asText()));
                    }

                    return new PlaylistItems(playlistId, videoIds);
                });
    }
}
