package controllers;

import models.Search;
import models.StoreActor;
import models.Video;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;

import static org.apache.pekko.pattern.Patterns.ask;

import play.libs.ws.WSClient;
import play.mvc.*;

import javax.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
            return Search.create(searchTerm, 10, wsClient)
                    .thenCompose(search -> {
                        List<CompletionStage<Video>> videoFutures =
                                search.getVideoIds().stream()
                                        .map(videoId ->
                                                ask(storeActor, new StoreActor.GetVideo(videoId), Duration.ofSeconds(50))
                                                        .thenCompose(res -> (CompletionStage<Video>) res)
                                        )
                                        .collect(Collectors.toList());

                        return CompletableFuture.allOf(
                                        videoFutures.stream()
                                                .map(CompletionStage::toCompletableFuture)
                                                .toArray(CompletableFuture[]::new)
                                )
                                .thenApply(v ->
                                        videoFutures.stream()
                                                .map(CompletionStage::toCompletableFuture)
                                                .map(CompletableFuture::join)
                                                .collect(Collectors.toList())
                                );
                    })
                    .thenApply(videos -> ok(views.html.index.render(videos, request)));
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

//    public CompletionStage<Result> search(Http.Request request) {
//
//    }

}
