package controllers;

import models.*;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;

import java.util.*;
import java.util.stream.*;

import static org.apache.pekko.pattern.Patterns.ask;

import play.libs.ws.WSClient;
import play.mvc.*;

import javax.inject.Inject;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class HomeController extends Controller {

    private final ActorRef storeActor;
    private final Duration duration = Duration.ofSeconds(10);

    @Inject
    public HomeController(WSClient wsClient, ActorSystem actorSystem) {
        this.storeActor = actorSystem.actorOf(StoreActor.props(wsClient), "storeActor");
    }

    public CompletionStage<Result> index(Http.Request request) {
        if (request.method().equals("GET")) {
            return CompletableFuture.supplyAsync(() -> ok(views.html.index.render(new ArrayList<>(), request)));
        } else if (request.method().equals("POST")) {
            Map<String, String[]> formData = request.body().asFormUrlEncoded();

            // Extract search terms using Stream API
            String searchTerm = Optional.ofNullable(formData)
                    .map(data -> data.get("search_terms"))
                    .filter(terms -> terms.length > 0)
                    .map(terms -> terms[0])
                    .orElse(null);

            return ask(storeActor, new StoreActor.GetSearch(searchTerm, 10), this.duration)
                    .thenCompose(search -> ((CompletionStage<List<Search>>)search))
                    .thenApply(searchList -> ok(views.html.index.render(searchList, request)));
        } else {
            return CompletableFuture.supplyAsync(() -> badRequest("Unsupported request"));
        }
    }

    public CompletionStage<Result> moreStats(String searchTerm, Http.Request request) {
        return ask(storeActor, new StoreActor.GetSearchPure(searchTerm, 50), this.duration)
                .thenCompose(search -> ((CompletionStage<Search>)search))
                .thenApply(search -> {
                   List<Map<String, String>> countedWords = countWords(search.getSearchResults().stream().map(s -> s.video.getDescription()).collect(Collectors.toList()));
                    return ok(views.html.moreStats.render(countedWords, request));
                });
    }

    private List<Map<String, String>> countWords(List<String> descriptions) {
        return descriptions.stream()
                .flatMap(desc -> Arrays.stream(desc.split("\\W+"))) // Split by non-word characters
                .map(String::toLowerCase)                           // Normalize to lowercase
                .filter(word -> !word.isEmpty())                    // Filter out empty words
                .collect(Collectors.groupingBy(
                        word -> word,
                        Collectors.summingInt(e -> 1)))             // Count occurrences of each word
                .entrySet().stream()
                .sorted((entry1, entry2) -> Integer.compare(entry2.getValue(), entry1.getValue())) // Sort by descending count
                .map(entry -> {
                    // Create a map with String keys and String values
                    Map<String, String> map = new HashMap<>();
                    map.put("word", entry.getKey());                     // Word as String
                    map.put("count", String.valueOf(entry.getValue()));  // Count as String
                    return map;
                })
                .collect(Collectors.toList());
    }

    public CompletionStage<Result> youtubePage(String videoId, Http.Request request) {
        return ask(storeActor, new StoreActor.GetVideo(videoId), Duration.ofSeconds(10))
                .thenCompose(video -> ((CompletionStage<Video>)video))
                .thenCompose(video -> ask(storeActor, new StoreActor.GetChannel(video.getChannelId()), this.duration)
                        .thenCompose(channel -> ((CompletionStage<Channel>)channel))
                        .thenApply(channel -> new ChannelVideo(video, channel)))
                .thenApply(channelVideo -> ok(views.html.youtubePage.render(channelVideo, request)));

    }

    public CompletionStage<Result> channelProfile(String channelId, Http.Request request) {
        return ask(storeActor, new StoreActor.GetChannel(channelId), this.duration)
                .thenCompose(channel -> ((CompletionStage<Channel>)channel))
                .thenCompose(channel -> ask(storeActor, new StoreActor.GetPlaylist(channel.getUploadsPlaylistId()), this.duration)
                        .thenCompose(playlist -> ((CompletionStage<PlaylistItems>)playlist))
                        .thenCompose(pl -> {
                            List<CompletionStage<Video>> cVideo = pl.getVideoIds().stream().map(videoId -> ask(storeActor, new StoreActor.GetVideo(videoId), this.duration)
                                    .thenCompose(video -> (CompletionStage<Video>)video))
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
                        .thenApply(videos -> new ChannelPlaylist(channel, videos))
                        .thenApply(channelPlaylist -> ok(views.html.channelProfile.render(channelPlaylist, request))));
    }


}
