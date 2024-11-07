package models;

import org.apache.pekko.actor.ActorRef;
import play.libs.ws.WSClient;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

import static org.apache.pekko.pattern.Patterns.ask;

public class YoutubePage {
    private final Video video;
    private final Channel channel;

    private static final Duration duration = Duration.ofSeconds(10);

    private YoutubePage(Video video, Channel channel) {
        this.channel = channel;
        this.video = video;
    }

    static CompletionStage<YoutubePage> create(String videoId, WSClient wsClient, ActorRef storeActor) {
        return ask(storeActor, new StoreActor.GetVideo(videoId), duration)
                .thenCompose(video -> ((CompletionStage<Video>) video))
                .thenCompose(video -> ask(storeActor, new StoreActor.GetChannel(video.getChannelId()), duration)
                        .thenCompose(channel -> ((CompletionStage<Channel>) channel))
                        .thenApply(channel -> new YoutubePage(video, channel)));
    }

    public Video getVideo() {
        return video;
    }

    public Channel getChannel() {
        return channel;
    }
}