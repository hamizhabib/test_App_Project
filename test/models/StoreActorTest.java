package models;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.testkit.javadsl.TestKit;
import static org.apache.pekko.pattern.Patterns.ask;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class StoreActorTest {

    private static ActorSystem system;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create("test-system");
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    public void testActor() throws Exception {
        // Step 1: Mock WSClient and WSResponse
        WSClient mockWsClient = Mockito.mock(WSClient.class);
        WSRequest mockRequest = Mockito.mock(WSRequest.class);
        WSResponse mockResponse = Mockito.mock(WSResponse.class);

        // Define mock behavior for WSClient
        when(mockWsClient.url(anyString())).thenReturn(mockRequest);
        when(mockRequest.addQueryParameter(anyString(), anyString())).thenReturn(mockRequest);

        // Step 2: Prepare JSON response as per YouTube API
        String jsonResponse = "{\n" +
                "  \"items\": [{\n" +
                "    \"snippet\": {\n" +
                "      \"title\": \"Mock Video Title\",\n" +
                "      \"description\": \"Mock Video Description\",\n" +
                "      \"thumbnails\": { \"medium\": { \"url\": \"https://mockurl.com/thumbnail.jpg\" } },\n" +
                "      \"channelId\": \"mockChannelId\",\n" +
                "      \"tags\": [\"mockTag1\", \"mockTag2\"]\n" +
                "    }\n" +
                "  }]\n" +
                "}";


        // Convert String to JsonNode
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonResponse);

        // Configure mock response to return our JSON
        when(mockResponse.asJson()).thenReturn(jsonNode);
        when(mockRequest.get()).thenReturn(CompletableFuture.completedFuture(mockResponse));

        // Step 3: Set up Config and Duration
        Config config = Mockito.mock(Config.class);
        when(config.getString("api.key")).thenReturn("mock-api-key");
        Duration duration = Duration.ofMillis(2000);

        // Step 4: Create StoreActor using the mocked WSClient and config
        ActorRef storeActor = system.actorOf(StoreActor.props(mockWsClient, config), "storeActor");

        // Step 5: Test the actor's GetVideo message handling
        CompletionStage<Video> future = ask(storeActor, new StoreActor.GetVideo("mockVideoId"), duration)
                .thenCompose(video -> (CompletionStage<Video>) video);

        // Step 6: Assert the Video response data
        Video video = future.toCompletableFuture().get();
        assertEquals("Mock Video Title", video.getTitle());
        assertEquals("Mock Video Description", video.getDescription());
        assertEquals("https://mockurl.com/thumbnail.jpg", video.getThumbnail());
        assertEquals("mockChannelId", video.getChannelId());
        assertEquals(List.of("mockTag1", "mockTag2"), video.getTags());
    }
}
