package models;

import play.libs.ws.WSClient;

import java.util.HashMap;
import java.util.Map;

import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.actor.Props;

public class StoreActor extends AbstractActor {
    private final Map<String, Video> videoMap = new HashMap<>();
    private final Map<String, Channel> channelMap = new HashMap<>();
//    private final Map<Map<Str>, Search>
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

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(GetVideo.class, msg -> {
                    if (videoMap.containsKey(msg.videoId)) {
                        getSender().tell(videoMap.get(msg.videoId), getSelf());
                    } else {
                        Video.create(msg.videoId, wsClient).thenAccept(video -> {
                            System.out.println("Video Id is: " + msg.videoId);
                            videoMap.put(msg.videoId, video);
                            getSender().tell(video, getSelf());
                        });
                    }
                })
                .match(GetChannel.class, msg -> {
                    if (channelMap.containsKey(msg.channelId)) {
                        getSender().tell(channelMap.get(msg.channelId), getSelf());
                    } else {
                        Channel.create(msg.channelId, wsClient).thenAccept(channel -> {
                            channelMap.put(msg.channelId, channel);
                            getSender().tell(channel, getSelf());
                        });
                    }
                })
                .build();
    }

}
