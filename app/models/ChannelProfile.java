package models;

import org.apache.pekko.actor.ActorRef;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static org.apache.pekko.pattern.Patterns.ask;

public class ChannelProfile {
    private final Channel channel;
    private final List<Video> latestVideos;

    private static final Duration duration = Duration.ofSeconds(10);

    private ChannelProfile(Channel channel, List<Video> latestVideos) {
        this.channel = channel;
        this.latestVideos = latestVideos;
    }

    static CompletionStage<ChannelProfile> create(String channelId, ActorRef storeActor) {
        return ask(storeActor, new StoreActor.GetChannel(channelId), duration)
                .thenCompose(channel -> ((CompletionStage<Channel>) channel))
                .thenCompose(channel -> ask(storeActor, new StoreActor.GetPlaylist(channel.getUploadsPlaylistId()), duration)
                        .thenCompose(playlist -> ((CompletionStage<PlaylistItems>) playlist))
                        .thenCompose(pl -> {
                            List<CompletionStage<Video>> cVideo = pl.getVideoIds().stream().map(videoId -> ask(storeActor, new StoreActor.GetVideo(videoId), duration)
                                            .thenCompose(video -> (CompletionStage<Video>) video))
                                    .collect(Collectors.toList());

                            return CompletableFuture.allOf(
                                    cVideo.stream()
                                            .map(CompletionStage::toCompletableFuture)
                                            .toArray(CompletableFuture[]::new)

                            ).thenApply(v ->
                                    cVideo.stream()
                                            .map(CompletionStage::toCompletableFuture)
                                            .map(CompletableFuture::join)
                                            .collect(Collectors.toList()));
                        })
                        .thenApply(videos -> new ChannelProfile(channel, videos)));
    }

    public Channel getChannel() {
        return channel;
    }

    public List<Video> getLatestVideos() {
        return latestVideos;
    }
}
