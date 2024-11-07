package models;

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

    private StoreActor(WSClient wsClient) {
        this.wsClient = wsClient;
    }

    public static Props props(WSClient wsClient) {
        return Props.create(StoreActor.class, () -> new StoreActor(wsClient));
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

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(GetVideo.class, msg -> {
                    if (videoMap.containsKey(msg.videoId)) {
                        getSender().tell(CompletableFuture.completedFuture(videoMap.get(msg.videoId)), getSelf());
                    } else {
                        getSender().tell(
                                Video.create(msg.videoId, wsClient)
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
                                Channel.create(msg.channelId, wsClient)
                                        .thenApply(channel -> {
                                            channelMap.put(msg.channelId, channel);
                                            return channel;
                                        }),
                                getSelf());
                    }
                })
                .match(GetSearch.class, msg -> getSender().tell(
                        Search.create(msg.searchTerm, msg.maxResults, wsClient, getSelf())
                                .thenApply(search -> {
                                    searchHistory.add(0, search);
                                    return searchHistory;
                                }),
                        getSelf()
                ))
                .match(GetMoreStats.class, msg -> getSender().tell(
                        Search.create(msg.searchTerm, msg.maxResults, wsClient, getSelf())
                                .thenApply(MoreStats::create),
                        getSelf()
                ))
                .match(GetPlaylist.class, msg -> getSender().tell(
                        PlaylistItems.create(msg.playlistId, wsClient),
                        getSelf()
                ))
                .build();
    }

}
