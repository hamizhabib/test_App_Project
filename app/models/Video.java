package models;

import java.util.ArrayList;
import java.util.concurrent.CompletionStage;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.ws.WSClient;

public class Video {
    private final String title;
    private final String description;
    private final String thumbnail;
    private final String videoId;
    private final String videoURL;
    private final String channelId;
    private final List<String> tags;

    private static String apiUrl = "https://www.googleapis.com/youtube/v3/videos";
    private static String apiKey = "AIzaSyBNAoEvMHEWinDTtBWT4S77Fsqv9_8tQIc";  // Replace with actual API key

    private Video(String title, String description, String thumbnail, String videoId, String videoURL, String channelId, List<String> tags) {
        this.title = title;
        this.description = description;
        this.thumbnail = thumbnail;
        this.videoId = videoId;
        this.videoURL = videoURL;
        this.channelId = channelId;
        this.tags = tags;
    }

    static CompletionStage<Video> create(String videoId, WSClient wsClient) {
        return wsClient.url(apiUrl)
                .addQueryParameter("part", "snippet")
                .addQueryParameter("id", videoId)
                .addQueryParameter("key", apiKey)
                .get()
                .thenApply(response -> {
                    JsonNode json = response.asJson();
                    JsonNode item = json.get("items").get(0);
                    String title = item.get("snippet").get("title").asText();
                    String description = item.get("snippet").get("description").asText();
                    String thumbnail = item.get("snippet").get("thumbnails").get("medium").get("url").asText();
                    String channelId = item.get("snippet").get("channelId").asText();
                    String videoURL = "https://www.youtube.com/watch?v=" + videoId;

                    List<String> tags = new ArrayList<>();
                    JsonNode tagsNode = item.get("snippet").get("tags");
                    if (tagsNode != null && tagsNode.isArray()) {
                        tagsNode.forEach(tagNode -> tags.add(tagNode.asText()));
                    }

                    return new Video(title, description, thumbnail, videoId, videoURL, channelId, tags);
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

    public String getVideoId() {
        return videoId;
    }

    public String getVideoURL() {
        return videoURL;
    }

    public String getChannelId() {
        return channelId;
    }

    public List<String> getTags() {
        return tags;
    }
}




