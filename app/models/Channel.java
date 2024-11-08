package models;

import java.util.concurrent.CompletionStage;

import com.fasterxml.jackson.databind.JsonNode;
import com.typesafe.config.Config;
import play.libs.ws.WSClient;

public class Channel {
    private final String title;
    private final String description;
    private final String thumbnail;
    private final String channelId;
    private final String channelURL;
    private final String uploadsPlaylistId;

    private final static String apiUrl = "https://www.googleapis.com/youtube/v3/channels";

    private Channel(String title, String description, String thumbnail, String channelId, String channelURL, String uploadsPlaylistId) {
        this.title = title;
        this.description = description;
        this.thumbnail = thumbnail;
        this.channelId = channelId;
        this.channelURL = channelURL;
        this.uploadsPlaylistId = uploadsPlaylistId;
    }

    public static CompletionStage<Channel> create(String channelId, WSClient wsClient, Config config) throws NullPointerException {
        return wsClient.url(apiUrl)
                .addQueryParameter("part", "snippet,contentDetails")
                .addQueryParameter("id", channelId)
                .addQueryParameter("key", config.getString("api.key"))
                .get()
                .thenApply(wsResponse -> {
                    JsonNode json = wsResponse.asJson();
                    JsonNode item = json.get("items").get(0);
                    String title = item.get("snippet").get("title").asText();
                    String description = item.get("snippet").get("description").asText();
                    String customURL = item.get("snippet").has("customUrl") && !item.get("snippet").get("customUrl").isNull() ? item.get("snippet").get("customUrl").asText() : "";
                    String channelURL = "https://www.youtube.com/" + customURL;
                    String thumbnail = item.get("snippet").get("thumbnails").get("medium").get("url").asText();

                    JsonNode contentDetails = item.get("contentDetails");
                    String uploadsPlaylistId = contentDetails.get("relatedPlaylists").get("uploads").asText();

                    return new Channel(title, description, thumbnail, channelId, channelURL, uploadsPlaylistId);

                });
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getChannelURL() {
        return channelURL;
    }

    public String getUploadsPlaylistId() {
        return uploadsPlaylistId;
    }
}
