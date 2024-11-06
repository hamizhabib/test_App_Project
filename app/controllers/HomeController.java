package controllers;

import models.Search;
import models.StoreActor;
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

/**
 * This controller contains an action to handle HTTP requests
 * to the application's home page.
 */
public class HomeController extends Controller {

    private final WSClient wsClient;
    private final ActorRef storeActor;

    @Inject
    public HomeController(WSClient wsClient, ActorSystem actorSystem) {
        this.wsClient = wsClient;
        this.storeActor = actorSystem.actorOf(StoreActor.props(wsClient), "storeActor");
    }

    /**
     * An action that renders an HTML page with a welcome message.
     * The configuration in the <code>routes</code> file means that
     * this method will be called when the application receives a
     * <code>GET</code> request with a path of <code>/</code>.
     */
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

//        if (searchTerm != null) {
            return ask(storeActor, new StoreActor.GetSearch(searchTerm, 10), Duration.ofSeconds(10))
//                    Search.create(searchTerm, 10, wsClient)
                    .thenCompose(search -> {
                        return ((CompletionStage<List<Search>>)search);
//                        List<CompletionStage<Video>> videoFutures =
//                                (search).getVideoIds().stream()
//                                        .map(videoId ->
//                                                ask(storeActor, new StoreActor.GetVideo(videoId), Duration.ofSeconds(50))
//                                                        .thenCompose(res -> (CompletionStage<Video>) res)
//                                        )
//                                        .collect(Collectors.toList());
//
//                        return CompletableFuture.allOf(
//                                        videoFutures.stream()
//                                                .map(CompletionStage::toCompletableFuture)
//                                                .toArray(CompletableFuture[]::new)
//                                )
//                                .thenApply(v ->
//                                        videoFutures.stream()
//                                                .map(CompletionStage::toCompletableFuture)
//                                                .map(CompletableFuture::join)
//                                                .collect(Collectors.toList())
//                                );
                    })
                    .thenApply(searchList -> ok(views.html.index.render(searchList, request)));
//        } else {
//            return CompletableFuture.completedStage(badRequest("Missing search_terms parameter"));
//        }
        } else {
            return CompletableFuture.supplyAsync(() -> badRequest("Unsupported request"));
        }
    }

    public Result explore() {
        return ok(views.html.explore.render());
    }

    public Result tutorial() {
        return ok(views.html.tutorial.render());
    }

    public CompletionStage<Result> moreStats(String searchTerm, Http.Request request) {
        return ask(storeActor, new StoreActor.GetSearchPure(searchTerm, 50), Duration.ofSeconds(10))
                .thenCompose(search -> {
                    return ((CompletionStage<Search>)search);
                })
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


}
