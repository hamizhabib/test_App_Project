package models;

import com.typesafe.config.Config;
import play.libs.ws.WSClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.actor.Props;

public class StoreActor extends AbstractActor {
    private final Map<String, Video> videoMap = new HashMap<>();
    private final Map<String, Channel> channelMap = new HashMap<>();
    private final List<Search> searchHistory = new ArrayList<>();

    private final WSClient wsClient;
    private final Config config;

    private StoreActor(WSClient wsClient, Config config) {
        this.wsClient = wsClient;
        this.config = config;
    }

    public static Props props(WSClient wsClient, Config config) {
        return Props.create(StoreActor.class, () -> new StoreActor(wsClient, config));
    }

    public static class GetVideo {
        String videoId;

        public GetVideo(String videoId) {
            this.videoId = videoId;
        }
    }

    public static class GetChannel {
        String channelId;

        public GetChannel(String channelId) {
            this.channelId = channelId;
        }
    }

    public static class GetSearch {
        String searchTerm;
        int maxResults;

        public GetSearch(String searchTerm, int maxResults) {
            this.searchTerm = searchTerm;
            this.maxResults = maxResults;
        }
    }

    public static class GetPlaylist {
        String playlistId;

        public GetPlaylist(String playlistId) {
            this.playlistId = playlistId;
        }
    }

    public static class GetMoreStats {
        String searchTerm;
        int maxResults;

        public GetMoreStats(String searchTerm, int maxResults) {
            this.searchTerm = searchTerm;
            this.maxResults = maxResults;
        }
    }

    public static class GetYoutubePage {
        String videoId;

        public GetYoutubePage(String videoId) {
            this.videoId = videoId;
        }
    }

    public static class GetChannelProfile {
        String channelId;

        public GetChannelProfile(String channelId) {
            this.channelId = channelId;
        }
    }


    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(GetVideo.class, msg -> {
                    if (videoMap.containsKey(msg.videoId)) {
                        getSender().tell(CompletableFuture.completedFuture(videoMap.get(msg.videoId)), getSelf());
                    } else {
                        getSender().tell(
                                Video.create(msg.videoId, wsClient, config)
                                        .thenApply(video -> {
                                            videoMap.put(msg.videoId, video);
                                            return video;
                                        }),
                                getSelf());
                    }
                })
                .match(GetChannel.class, msg -> {
                    if (channelMap.containsKey(msg.channelId)) {
                        getSender().tell(CompletableFuture.completedFuture(channelMap.get(msg.channelId)), getSelf());
                    } else {
                        getSender().tell(
                                Channel.create(msg.channelId, wsClient, config)
                                        .thenApply(channel -> {
                                            channelMap.put(msg.channelId, channel);
                                            return channel;
                                        }),
                                getSelf());
                    }
                })
                .match(GetSearch.class, msg -> getSender().tell(
                        Search.create(msg.searchTerm, msg.maxResults, wsClient, getSelf(), config)
                                .thenApply(search -> {
                                    searchHistory.add(0, search);
                                    return searchHistory;
                                }),
                        getSelf()
                ))
                .match(GetMoreStats.class, msg -> getSender().tell(
                        MoreStats.create(msg.searchTerm, msg.maxResults, wsClient, getSelf(), config),
                        getSelf()
                ))
                .match(GetPlaylist.class, msg -> getSender().tell(
                        PlaylistItems.create(msg.playlistId, wsClient, config),
                        getSelf()
                ))
                .match(GetYoutubePage.class, msg -> getSender().tell(
                        YoutubePage.create(msg.videoId, wsClient, getSelf(), config),
                        getSelf()
                ))
                .match(GetChannelProfile.class, msg -> getSender().tell(
                        ChannelProfile.create(msg.channelId, getSelf(), config),
                        getSelf()
                ))
                .build();
    }

}
