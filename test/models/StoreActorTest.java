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
    private static WSClient mockWsClient;
    private static WSRequest mockRequest;
    private static WSResponse mockResponse;
    private static Config config;
    private static Duration duration;
    private static ActorRef storeActor;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create("test-system");
        // Define mock behavior for WSClient
        mockWsClient = Mockito.mock(WSClient.class);
        mockRequest = Mockito.mock(WSRequest.class);
        mockResponse = Mockito.mock(WSResponse.class);

        config = Mockito.mock(Config.class);
        duration = Duration.ofMillis(2000);

        when(mockWsClient.url(anyString())).thenReturn(mockRequest);
        when(mockRequest.addQueryParameter(anyString(), anyString())).thenReturn(mockRequest);
        when(config.getString("api.key")).thenReturn("mock-api-key");

        storeActor = system.actorOf(StoreActor.props(mockWsClient, config), "storeActor");
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    public void testActorGetVideo() throws Exception {

        // Step 2: Prepare JSON response as per YouTube API
        String jsonVideoResponse = "{\n" +
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
        JsonNode jsonNode = mapper.readTree(jsonVideoResponse);

        // Configure mock response to return our JSON
        when(mockResponse.asJson()).thenReturn(jsonNode);
        when(mockRequest.get()).thenReturn(CompletableFuture.completedFuture(mockResponse));

        // Step 5: Test the actor's GetVideo message handling
        CompletionStage<Video> future = ask(storeActor, new StoreActor.GetVideo("mockVideoId"), duration)
                .thenCompose(video -> (CompletionStage<Video>) video);

        // Step 6: Assert the Video response data
        Video video = future.toCompletableFuture().get();
        assertEquals("Mock Video Title", video.getTitle());
        assertEquals("Mock Video Description", video.getDescription());
        assertEquals("https://mockurl.com/thumbnail.jpg", video.getThumbnail());
        assertEquals("mockChannelId", video.getChannelId());
        assertEquals("mockVideoId", video.getVideoId());
        assertEquals("https://www.youtube.com/watch?v=mockVideoId", video.getVideoURL());
        assertEquals(List.of("mockTag1", "mockTag2"), video.getTags());
    }

    @Test
    public void testActorGetChannel() throws Exception {

        // Step 2: Prepare JSON response as per YouTube API
        String jsonChannelResponse = "{\n" +
                "  \"items\": [{\n" +
                "    \"snippet\": {\n" +
                "      \"title\": \"Mock Channel Title\",\n" +
                "      \"description\": \"Mock Channel Description\",\n" +
                "      \"customUrl\": \"MockCustomURL\",\n" +
                "      \"thumbnails\": { \"medium\": { \"url\": \"https://mockurl.com/channel-thumbnail.jpg\" } }\n" +
                "    },\n" +
                "    \"contentDetails\": {\n" +
                "      \"relatedPlaylists\": {\n" +
                "        \"uploads\": \"MockUploadsPlaylistId\"\n" +
                "      }\n" +
                "    }\n" +
                "  }]\n" +
                "}";

        // Convert String to JsonNode
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonChannelResponse);

        // Configure mock response to return our JSON
        when(mockResponse.asJson()).thenReturn(jsonNode);
        when(mockRequest.get()).thenReturn(CompletableFuture.completedFuture(mockResponse));

        // Step 5: Test the actor's GetVideo message handling
        CompletionStage<Channel> future = ask(storeActor, new StoreActor.GetChannel("mockChannelId"), duration)
                .thenCompose(channel -> (CompletionStage<Channel>) channel);

        // Step 6: Assert the Video response data
        Channel channel = future.toCompletableFuture().get();
        assertEquals("Mock Channel Title", channel.getTitle());
        assertEquals("Mock Channel Description", channel.getDescription());
        assertEquals("https://mockurl.com/channel-thumbnail.jpg", channel.getThumbnail());
        assertEquals("MockUploadsPlaylistId", channel.getUploadsPlaylistId());
        assertEquals("mockChannelId", channel.getChannelId());
        assertEquals("https://www.youtube.com/MockCustomURL", channel.getChannelURL());
    }
}
