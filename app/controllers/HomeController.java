package controllers;

import models.*;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;

import java.util.*;

import static org.apache.pekko.pattern.Patterns.ask;

import play.libs.ws.WSClient;
import com.typesafe.config.Config;
import play.mvc.*;

import javax.inject.Inject;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class HomeController extends Controller {

    private final ActorRef storeActor;
    private final Duration duration;

    private final String GET = "GET";
    private final String POST = "POST";

    @Inject
    public HomeController(WSClient wsClient, ActorSystem actorSystem, Config config) {
        this.storeActor = actorSystem.actorOf(StoreActor.props(wsClient, config), "storeActor");
        this.duration = Duration.ofMillis(config.getLong("pekko.ask.duration"));
    }

    public CompletionStage<Result> index(Http.Request request) {
        if (request.method().equals(GET)) {
            return CompletableFuture.supplyAsync(() -> ok(views.html.index.render(new ArrayList<>(), request)));
        } else if (request.method().equals(POST)) {
            Map<String, String[]> formData = request.body().asFormUrlEncoded();

            // Extract search terms using Stream API
            String searchTerm = Optional.ofNullable(formData)
                    .map(data -> data.get("search_terms"))
                    .filter(terms -> terms.length > 0)
                    .map(terms -> terms[0])
                    .orElse(null);

            return ask(storeActor, new StoreActor.GetSearch(searchTerm, 10), this.duration)
                    .thenCompose(search -> ((CompletionStage<List<Search>>) search))
                    .thenApply(searchList -> ok(views.html.index.render(searchList, request)));
        } else {
            return CompletableFuture.supplyAsync(() -> badRequest("Unsupported request"));
        }
    }

    public CompletionStage<Result> moreStats(String searchTerm, Http.Request request) {
        if (request.method().equals(GET)) {
            return ask(storeActor, new StoreActor.GetMoreStats(searchTerm, 50), this.duration)
                    .thenCompose(moreStats -> ((CompletionStage<MoreStats>) moreStats))
                    .thenApply(moreStats -> ok(views.html.moreStats.render(moreStats, request)));
        } else {
            return CompletableFuture.supplyAsync(() -> badRequest("Unsupported request"));
        }
    }

    public CompletionStage<Result> youtubePage(String videoId, Http.Request request) {
        if (request.method().equals(GET)) {
            return ask(storeActor, new StoreActor.GetYoutubePage(videoId), this.duration)
                    .thenCompose(youtubePage -> ((CompletionStage<YoutubePage>) youtubePage))
                    .thenApply(youtubePage -> ok(views.html.youtubePage.render(youtubePage, request)));
        } else {
            return CompletableFuture.supplyAsync(() -> badRequest("Unsupported request"));
        }
    }

    public CompletionStage<Result> channelProfile(String channelId, Http.Request request) {
        if (request.method().equals(GET)) {
            return ask(storeActor, new StoreActor.GetChannelProfile(channelId), this.duration)
                    .thenCompose(channelProfile -> ((CompletionStage<ChannelProfile>) channelProfile))
                    .thenApply(channelProfile -> ok(views.html.channelProfile.render(channelProfile, request)));
        } else {
            return CompletableFuture.supplyAsync(() -> badRequest("Unsupported request"));
        }
    }
}
