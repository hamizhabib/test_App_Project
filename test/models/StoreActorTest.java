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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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
    public void testActorGetVideo() throws Exception {

        WSClient mockWsClient = Mockito.mock(WSClient.class);
        WSRequest mockRequest = Mockito.mock(WSRequest.class);
        WSResponse mockResponse = Mockito.mock(WSResponse.class);

        Config config = Mockito.mock(Config.class);
        Duration duration = Duration.ofMillis(2000);

        when(mockWsClient.url(anyString())).thenReturn(mockRequest);
        when(mockRequest.addQueryParameter(anyString(), anyString())).thenReturn(mockRequest);
        when(config.getString("api.key")).thenReturn("mock-api-key");
        when(config.getLong("pekko.ask.duration")).thenReturn(2000L);

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

        ActorRef storeActor = system.actorOf(StoreActor.props(mockWsClient, config), "storeActor-1");

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

        WSClient mockWsClient = Mockito.mock(WSClient.class);
        WSRequest mockRequest = Mockito.mock(WSRequest.class);
        WSResponse mockResponse = Mockito.mock(WSResponse.class);

        Config config = Mockito.mock(Config.class);
        Duration duration = Duration.ofMillis(2000);

        when(mockWsClient.url(anyString())).thenReturn(mockRequest);
        when(mockRequest.addQueryParameter(anyString(), anyString())).thenReturn(mockRequest);
        when(config.getString("api.key")).thenReturn("mock-api-key");
        when(config.getLong("pekko.ask.duration")).thenReturn(2000L);

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

        ActorRef storeActor = system.actorOf(StoreActor.props(mockWsClient, config), "storeActor-2");

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

    @Test
    public void testActorGetSearch() throws Exception {

        WSClient mockWsClient = Mockito.mock(WSClient.class);
        WSRequest mockRequest = Mockito.mock(WSRequest.class);
        WSResponse mockResponse = Mockito.mock(WSResponse.class);

        Config config = Mockito.mock(Config.class);
        Duration duration = Duration.ofMillis(2000);

        when(mockWsClient.url(anyString())).thenReturn(mockRequest);
        when(mockRequest.addQueryParameter(anyString(), anyString())).thenReturn(mockRequest);
        when(config.getString("api.key")).thenReturn("mock-api-key");
        when(config.getLong("pekko.ask.duration")).thenReturn(2000L);

        // Step 2: Prepare JSON response as per YouTube API
        String jsonSearchResponse = "{\n" +
                "  \"items\": [\n" +
                "    {\n" +
                "      \"kind\": \"youtube#searchResult\",\n" +
                "      \"etag\": \"mockEtag1\",\n" +
                "      \"id\": {\n" +
                "        \"kind\": \"youtube#video\",\n" +
                "        \"videoId\": \"mockVideoId1\",\n" +
                "        \"channelId\": \"mockChannelId1\",\n" +
                "        \"playlistId\": \"mockPlaylistId1\"\n" +
                "      },\n" +
                "      \"snippet\": {\n" +
                "        \"publishedAt\": \"2023-01-01T00:00:00Z\",\n" +
                "        \"channelId\": \"mockChannelId1\",\n" +
                "        \"title\": \"Mock Video Title 1\",\n" +
                "        \"description\": \"Description of the first mock video.\",\n" +
                "        \"thumbnails\": {\n" +
                "          \"default\": {\n" +
                "            \"url\": \"https://mockurl.com/thumbnail1_default.jpg\",\n" +
                "            \"width\": 120,\n" +
                "            \"height\": 90\n" +
                "          },\n" +
                "          \"medium\": {\n" +
                "            \"url\": \"https://mockurl.com/thumbnail1_medium.jpg\",\n" +
                "            \"width\": 320,\n" +
                "            \"height\": 180\n" +
                "          },\n" +
                "          \"high\": {\n" +
                "            \"url\": \"https://mockurl.com/thumbnail1_high.jpg\",\n" +
                "            \"width\": 480,\n" +
                "            \"height\": 360\n" +
                "          }\n" +
                "        },\n" +
                "        \"channelTitle\": \"Mock Channel Title 1\",\n" +
                "        \"liveBroadcastContent\": \"none\"\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"kind\": \"youtube#searchResult\",\n" +
                "      \"etag\": \"mockEtag2\",\n" +
                "      \"id\": {\n" +
                "        \"kind\": \"youtube#video\",\n" +
                "        \"videoId\": \"mockVideoId2\",\n" +
                "        \"channelId\": \"mockChannelId2\",\n" +
                "        \"playlistId\": \"mockPlaylistId2\"\n" +
                "      },\n" +
                "      \"snippet\": {\n" +
                "        \"publishedAt\": \"2023-01-02T00:00:00Z\",\n" +
                "        \"channelId\": \"mockChannelId2\",\n" +
                "        \"title\": \"Mock Video Title 2\",\n" +
                "        \"description\": \"Description of the second mock video.\",\n" +
                "        \"thumbnails\": {\n" +
                "          \"default\": {\n" +
                "            \"url\": \"https://mockurl.com/thumbnail2_default.jpg\",\n" +
                "            \"width\": 120,\n" +
                "            \"height\": 90\n" +
                "          },\n" +
                "          \"medium\": {\n" +
                "            \"url\": \"https://mockurl.com/thumbnail2_medium.jpg\",\n" +
                "            \"width\": 320,\n" +
                "            \"height\": 180\n" +
                "          },\n" +
                "          \"high\": {\n" +
                "            \"url\": \"https://mockurl.com/thumbnail2_high.jpg\",\n" +
                "            \"width\": 480,\n" +
                "            \"height\": 360\n" +
                "          }\n" +
                "        },\n" +
                "        \"channelTitle\": \"Mock Channel Title 2\",\n" +
                "        \"liveBroadcastContent\": \"none\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";


        String jsonVideoResponse1 = "{\n" +
                "  \"items\": [{\n" +
                "    \"snippet\": {\n" +
                "      \"title\": \"Mock Video Title 1\",\n" +
                "      \"description\": \"Mock Video Description 1\",\n" +
                "      \"thumbnails\": { \"medium\": { \"url\": \"https://mockvideourl1.com/thumbnail.jpg\" } },\n" +
                "      \"channelId\": \"mockChannelId 1\",\n" +
                "      \"tags\": [\"mockTag1\", \"mockTag2\"]\n" +
                "    }\n" +
                "  }]\n" +
                "}";

        String jsonVideoResponse2 = "{\n" +
                "  \"items\": [{\n" +
                "    \"snippet\": {\n" +
                "      \"title\": \"Mock Video Title 2\",\n" +
                "      \"description\": \"Mock Video Description 2\",\n" +
                "      \"thumbnails\": { \"medium\": { \"url\": \"https://mockvideourl2.com/thumbnail.jpg\" } },\n" +
                "      \"channelId\": \"mockChannelId 2\",\n" +
                "      \"tags\": [\"mockTag1\", \"mockTag2\"]\n" +
                "    }\n" +
                "  }]\n" +
                "}";

        String jsonChannelResponse1 = "{\n" +
                "  \"items\": [{\n" +
                "    \"snippet\": {\n" +
                "      \"title\": \"Mock Channel Title 1\",\n" +
                "      \"description\": \"Mock Channel Description 1\",\n" +
                "      \"customUrl\": \"MockCustomURL 1\",\n" +
                "      \"thumbnails\": { \"medium\": { \"url\": \"https://mockchannelurl1.com/channel-thumbnail.jpg\" } }\n" +
                "    },\n" +
                "    \"contentDetails\": {\n" +
                "      \"relatedPlaylists\": {\n" +
                "        \"uploads\": \"MockUploadsPlaylistId 1\"\n" +
                "      }\n" +
                "    }\n" +
                "  }]\n" +
                "}";

        String jsonChannelResponse2 = "{\n" +
                "  \"items\": [{\n" +
                "    \"snippet\": {\n" +
                "      \"title\": \"Mock Channel Title 2\",\n" +
                "      \"description\": \"Mock Channel Description 2\",\n" +
                "      \"customUrl\": \"MockCustomURL 2\",\n" +
                "      \"thumbnails\": { \"medium\": { \"url\": \"https://mockchannelurl2.com/channel-thumbnail.jpg\" } }\n" +
                "    },\n" +
                "    \"contentDetails\": {\n" +
                "      \"relatedPlaylists\": {\n" +
                "        \"uploads\": \"MockUploadsPlaylistId 2\"\n" +
                "      }\n" +
                "    }\n" +
                "  }]\n" +
                "}";

        // Convert String to JsonNode
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonSearchResponse);

        ObjectMapper jsonVideoResponse1Mapper = new ObjectMapper();
        JsonNode jsonNodeVideoResponse1 = jsonVideoResponse1Mapper.readTree(jsonVideoResponse1);

        ObjectMapper jsonVideoResponse2Mapper = new ObjectMapper();
        JsonNode jsonNodeVideoResponse2 = jsonVideoResponse2Mapper.readTree(jsonVideoResponse2);

        ObjectMapper jsonChannelResponse1Mapper = new ObjectMapper();
        JsonNode jsonNodeChannelResponse1 = jsonChannelResponse1Mapper.readTree(jsonChannelResponse1);

        ObjectMapper jsonChannelResponse2Mapper = new ObjectMapper();
        JsonNode jsonNodeChannelResponse2 = jsonChannelResponse2Mapper.readTree(jsonChannelResponse2);

        // Configure mock response to return our JSON
        when(mockResponse.asJson()).thenReturn(jsonNode)
                .thenReturn(jsonNodeVideoResponse1)
                .thenReturn(jsonNodeVideoResponse2)
                .thenReturn(jsonNodeChannelResponse1)
                .thenReturn(jsonNodeChannelResponse2);

        when(mockRequest.get()).thenReturn(CompletableFuture.completedFuture(mockResponse));

        ActorRef storeActor = system.actorOf(StoreActor.props(mockWsClient, config), "storeActor-3");

        // Step 5: Test the actor's GetVideo message handling
        CompletionStage<List<Search>> future = ask(storeActor, new StoreActor.GetSearch("mockSearch", 2), duration)
                .thenCompose(search -> (CompletionStage<List<Search>>) search);

        // Step 6: Assert the Video response data
        List<Search> search = future.toCompletableFuture().get();  // Await completion of the future
        assertEquals("mockSearch", search.get(0).getSearchTerm());
        assertEquals(2, search.get(0).getSearchResults().size());
        String videoTitle1 = search.get(0).getSearchResults().get(0).video.getTitle();
        String videoTitle2 = search.get(0).getSearchResults().get(1).video.getTitle();
        String thumbnail1 = search.get(0).getSearchResults().get(0).video.getThumbnail();
        String thumbnail2 = search.get(0).getSearchResults().get(1).video.getThumbnail();

        assertTrue(
                "Either of the video titles should be 'Mock Video Title 1'",
                "Mock Video Title 1".equals(videoTitle1) || "Mock Video Title 1".equals(videoTitle2)
        );

        assertTrue(
                "Either of the thumbnails should be 'https://mockvideourl2.com/thumbnail.jpg'",
                "https://mockvideourl2.com/thumbnail.jpg".equals(thumbnail1) || "https://mockvideourl2.com/thumbnail.jpg".equals(thumbnail2)
        );

        String channelTitle1 = search.get(0).getSearchResults().get(0).channel.getTitle();
        String channelTitle2 = search.get(0).getSearchResults().get(1).channel.getTitle();
        assertTrue(
                "Either of the channel titles should be 'Mock Channel Title 2'",
                "Mock Channel Title 2".equals(channelTitle1) || "Mock Channel Title 2".equals(channelTitle2)
        );
        assertTrue(
                "Either of the channel titles should be 'Mock Channel Title 1'",
                "Mock Channel Title 1".equals(channelTitle1) || "Mock Channel Title 1".equals(channelTitle2)
        );
        assertEquals("Expected Flesch-Kincaid Reading score of 54.73 for 'Mock Video Description 1', but received a different value.", 54.73, search.get(0).getSearchResults().get(0).fleshReadingScore, 0.01);
        assertEquals("Expected Flesch-Kincaid Grade Level score of 6.62 for 'Mock Video Description 1', but received a different value.", 6.62, search.get(0).getSearchResults().get(0).fleshKincaidGradeLevel, 0.01);
    }

    @Test
    public void testActorGetMoreStats() throws Exception {

        WSClient mockWsClient = Mockito.mock(WSClient.class);
        WSRequest mockRequest = Mockito.mock(WSRequest.class);
        WSResponse mockResponse = Mockito.mock(WSResponse.class);

        Config config = Mockito.mock(Config.class);
        Duration duration = Duration.ofMillis(2000);

        when(mockWsClient.url(anyString())).thenReturn(mockRequest);
        when(mockRequest.addQueryParameter(anyString(), anyString())).thenReturn(mockRequest);
        when(config.getString("api.key")).thenReturn("mock-api-key");
        when(config.getLong("pekko.ask.duration")).thenReturn(2000L);

        // Step 2: Prepare JSON response as per YouTube API
        String jsonSearchResponse = "{\n" +
                "  \"items\": [\n" +
                "    {\n" +
                "      \"kind\": \"youtube#searchResult\",\n" +
                "      \"etag\": \"mockEtag1\",\n" +
                "      \"id\": {\n" +
                "        \"kind\": \"youtube#video\",\n" +
                "        \"videoId\": \"mockVideoId1\",\n" +
                "        \"channelId\": \"mockChannelId1\",\n" +
                "        \"playlistId\": \"mockPlaylistId1\"\n" +
                "      },\n" +
                "      \"snippet\": {\n" +
                "        \"publishedAt\": \"2023-01-01T00:00:00Z\",\n" +
                "        \"channelId\": \"mockChannelId1\",\n" +
                "        \"title\": \"Mock Video Title 1\",\n" +
                "        \"description\": \"Description of the first mock video.\",\n" +
                "        \"thumbnails\": {\n" +
                "          \"default\": {\n" +
                "            \"url\": \"https://mockurl.com/thumbnail1_default.jpg\",\n" +
                "            \"width\": 120,\n" +
                "            \"height\": 90\n" +
                "          },\n" +
                "          \"medium\": {\n" +
                "            \"url\": \"https://mockurl.com/thumbnail1_medium.jpg\",\n" +
                "            \"width\": 320,\n" +
                "            \"height\": 180\n" +
                "          },\n" +
                "          \"high\": {\n" +
                "            \"url\": \"https://mockurl.com/thumbnail1_high.jpg\",\n" +
                "            \"width\": 480,\n" +
                "            \"height\": 360\n" +
                "          }\n" +
                "        },\n" +
                "        \"channelTitle\": \"Mock Channel Title 1\",\n" +
                "        \"liveBroadcastContent\": \"none\"\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"kind\": \"youtube#searchResult\",\n" +
                "      \"etag\": \"mockEtag2\",\n" +
                "      \"id\": {\n" +
                "        \"kind\": \"youtube#video\",\n" +
                "        \"videoId\": \"mockVideoId2\",\n" +
                "        \"channelId\": \"mockChannelId2\",\n" +
                "        \"playlistId\": \"mockPlaylistId2\"\n" +
                "      },\n" +
                "      \"snippet\": {\n" +
                "        \"publishedAt\": \"2023-01-02T00:00:00Z\",\n" +
                "        \"channelId\": \"mockChannelId2\",\n" +
                "        \"title\": \"Mock Video Title 2\",\n" +
                "        \"description\": \"Description of the second mock video.\",\n" +
                "        \"thumbnails\": {\n" +
                "          \"default\": {\n" +
                "            \"url\": \"https://mockurl.com/thumbnail2_default.jpg\",\n" +
                "            \"width\": 120,\n" +
                "            \"height\": 90\n" +
                "          },\n" +
                "          \"medium\": {\n" +
                "            \"url\": \"https://mockurl.com/thumbnail2_medium.jpg\",\n" +
                "            \"width\": 320,\n" +
                "            \"height\": 180\n" +
                "          },\n" +
                "          \"high\": {\n" +
                "            \"url\": \"https://mockurl.com/thumbnail2_high.jpg\",\n" +
                "            \"width\": 480,\n" +
                "            \"height\": 360\n" +
                "          }\n" +
                "        },\n" +
                "        \"channelTitle\": \"Mock Channel Title 2\",\n" +
                "        \"liveBroadcastContent\": \"none\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";


        String jsonVideoResponse1 = "{\n" +
                "  \"items\": [{\n" +
                "    \"snippet\": {\n" +
                "      \"title\": \"Mock Video Title 1\",\n" +
                "      \"description\": \"Mock Video Description 1\",\n" +
                "      \"thumbnails\": { \"medium\": { \"url\": \"https://mockvideourl1.com/thumbnail.jpg\" } },\n" +
                "      \"channelId\": \"mockChannelId 1\",\n" +
                "      \"tags\": [\"mockTag1\", \"mockTag2\"]\n" +
                "    }\n" +
                "  }]\n" +
                "}";

        String jsonVideoResponse2 = "{\n" +
                "  \"items\": [{\n" +
                "    \"snippet\": {\n" +
                "      \"title\": \"Mock Video Title 2\",\n" +
                "      \"description\": \"Mock Video Description 2\",\n" +
                "      \"thumbnails\": { \"medium\": { \"url\": \"https://mockvideourl2.com/thumbnail.jpg\" } },\n" +
                "      \"channelId\": \"mockChannelId 2\",\n" +
                "      \"tags\": [\"mockTag1\", \"mockTag2\"]\n" +
                "    }\n" +
                "  }]\n" +
                "}";

        String jsonChannelResponse1 = "{\n" +
                "  \"items\": [{\n" +
                "    \"snippet\": {\n" +
                "      \"title\": \"Mock Channel Title 1\",\n" +
                "      \"description\": \"Mock Channel Description 1\",\n" +
                "      \"customUrl\": \"MockCustomURL 1\",\n" +
                "      \"thumbnails\": { \"medium\": { \"url\": \"https://mockchannelurl1.com/channel-thumbnail.jpg\" } }\n" +
                "    },\n" +
                "    \"contentDetails\": {\n" +
                "      \"relatedPlaylists\": {\n" +
                "        \"uploads\": \"MockUploadsPlaylistId 1\"\n" +
                "      }\n" +
                "    }\n" +
                "  }]\n" +
                "}";

        String jsonChannelResponse2 = "{\n" +
                "  \"items\": [{\n" +
                "    \"snippet\": {\n" +
                "      \"title\": \"Mock Channel Title 2\",\n" +
                "      \"description\": \"Mock Channel Description 2\",\n" +
                "      \"customUrl\": \"MockCustomURL 2\",\n" +
                "      \"thumbnails\": { \"medium\": { \"url\": \"https://mockchannelurl2.com/channel-thumbnail.jpg\" } }\n" +
                "    },\n" +
                "    \"contentDetails\": {\n" +
                "      \"relatedPlaylists\": {\n" +
                "        \"uploads\": \"MockUploadsPlaylistId 2\"\n" +
                "      }\n" +
                "    }\n" +
                "  }]\n" +
                "}";

        // Convert String to JsonNode
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonSearchResponse);

        ObjectMapper jsonVideoResponse1Mapper = new ObjectMapper();
        JsonNode jsonNodeVideoResponse1 = jsonVideoResponse1Mapper.readTree(jsonVideoResponse1);

        ObjectMapper jsonVideoResponse2Mapper = new ObjectMapper();
        JsonNode jsonNodeVideoResponse2 = jsonVideoResponse2Mapper.readTree(jsonVideoResponse2);

        ObjectMapper jsonChannelResponse1Mapper = new ObjectMapper();
        JsonNode jsonNodeChannelResponse1 = jsonChannelResponse1Mapper.readTree(jsonChannelResponse1);

        ObjectMapper jsonChannelResponse2Mapper = new ObjectMapper();
        JsonNode jsonNodeChannelResponse2 = jsonChannelResponse2Mapper.readTree(jsonChannelResponse2);

        // Configure mock response to return our JSON
        when(mockResponse.asJson()).thenReturn(jsonNode)
                .thenReturn(jsonNodeVideoResponse1)
                .thenReturn(jsonNodeVideoResponse2)
                .thenReturn(jsonNodeChannelResponse1)
                .thenReturn(jsonNodeChannelResponse2);

        when(mockRequest.get()).thenReturn(CompletableFuture.completedFuture(mockResponse));

        ActorRef storeActor = system.actorOf(StoreActor.props(mockWsClient, config), "storeActor-4");

        // Step 5: Test the actor's GetVideo message handling
        CompletionStage<MoreStats> future = ask(storeActor, new StoreActor.GetMoreStats("mockSearch", 2), duration)
                .thenCompose(moreStats -> (CompletionStage<MoreStats>) moreStats);

        // Step 6: Assert the Video response data
        MoreStats moreStats = future.toCompletableFuture().get();
        assertEquals(Integer.valueOf(2), moreStats.getCountedWords().get("Mock".toLowerCase()));
        assertEquals(Integer.valueOf(1), moreStats.getCountedWords().get("2"));
    }

    @Test
    public void testActorGetPlaylist() throws Exception {
        WSClient mockWsClient = Mockito.mock(WSClient.class);
        WSRequest mockRequest = Mockito.mock(WSRequest.class);
        WSResponse mockResponse = Mockito.mock(WSResponse.class);

        Config config = Mockito.mock(Config.class);
        Duration duration = Duration.ofMillis(2000);

        when(mockWsClient.url(anyString())).thenReturn(mockRequest);
        when(mockRequest.addQueryParameter(anyString(), anyString())).thenReturn(mockRequest);
        when(config.getString("api.key")).thenReturn("mock-api-key");
        when(config.getLong("pekko.ask.duration")).thenReturn(2000L);

        // Step 2: Prepare JSON response as per YouTube API
        String jsonPlaylistResponse = "{\n" +
                "  \"items\": [\n" +
                "    {\n" +
                "      \"snippet\": {\n" +
                "        \"title\": \"Mock Playlist Video Title 1\",\n" +
                "        \"description\": \"Description for video 1\",\n" +
                "        \"thumbnails\": { \"medium\": { \"url\": \"https://mockurl.com/thumbnail1.jpg\" } },\n" +
                "        \"channelId\": \"mockChannelId1\",\n" +
                "        \"resourceId\": { \"videoId\": \"mockVideoId1\" }\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"snippet\": {\n" +
                "        \"title\": \"Mock Playlist Video Title 2\",\n" +
                "        \"description\": \"Description for video 2\",\n" +
                "        \"thumbnails\": { \"medium\": { \"url\": \"https://mockurl.com/thumbnail2.jpg\" } },\n" +
                "        \"channelId\": \"mockChannelId2\",\n" +
                "        \"resourceId\": { \"videoId\": \"mockVideoId2\" }\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"snippet\": {\n" +
                "        \"title\": \"Mock Playlist Video Title 3\",\n" +
                "        \"description\": \"Description for video 3\",\n" +
                "        \"thumbnails\": { \"medium\": { \"url\": \"https://mockurl.com/thumbnail3.jpg\" } },\n" +
                "        \"channelId\": \"mockChannelId3\",\n" +
                "        \"resourceId\": { \"videoId\": \"mockVideoId3\" }\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";


        // Convert String to JsonNode
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonPlaylistResponse);

        // Configure mock response to return our JSON
        when(mockResponse.asJson()).thenReturn(jsonNode);
        when(mockRequest.get()).thenReturn(CompletableFuture.completedFuture(mockResponse));

        ActorRef storeActor = system.actorOf(StoreActor.props(mockWsClient, config), "storeActor-5");

        // Step 5: Test the actor's GetVideo message handling
        CompletionStage<PlaylistItems> future = ask(storeActor, new StoreActor.GetPlaylist("mockPlaylistId"), duration)
                .thenCompose(playlistItems -> (CompletionStage<PlaylistItems>) playlistItems);

        // Step 6: Assert the Video response data
        PlaylistItems playlistItems = future.toCompletableFuture().get();
        assertEquals("mockPlaylistId", playlistItems.getPlaylistId());
        assertEquals(Arrays.asList("mockVideoId1", "mockVideoId2", "mockVideoId3"), playlistItems.getVideoIds());
    }

    @Test
    public void testActorGetYoutubePage() throws Exception {
        WSClient mockWsClient = Mockito.mock(WSClient.class);
        WSRequest mockRequest = Mockito.mock(WSRequest.class);
        WSResponse mockResponse = Mockito.mock(WSResponse.class);

        Config config = Mockito.mock(Config.class);
        Duration duration = Duration.ofMillis(2000);

        when(mockWsClient.url(anyString())).thenReturn(mockRequest);
        when(mockRequest.addQueryParameter(anyString(), anyString())).thenReturn(mockRequest);
        when(config.getString("api.key")).thenReturn("mock-api-key");
        when(config.getLong("pekko.ask.duration")).thenReturn(2000L);

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
        ObjectMapper videoMapper = new ObjectMapper();
        JsonNode jsonVideoNode = videoMapper.readTree(jsonVideoResponse);

        ObjectMapper channelMapper = new ObjectMapper();
        JsonNode jsonChannelNode = channelMapper.readTree(jsonChannelResponse);

        // Configure mock response to return our JSON
        when(mockResponse.asJson())
                .thenReturn(jsonVideoNode)
                .thenReturn(jsonChannelNode);
        when(mockRequest.get()).thenReturn(CompletableFuture.completedFuture(mockResponse));

        ActorRef storeActor = system.actorOf(StoreActor.props(mockWsClient, config), "storeActor-6");

        // Step 5: Test the actor's GetVideo message handling
        CompletionStage<YoutubePage> future = ask(storeActor, new StoreActor.GetYoutubePage("mockVideoId"), duration)
                .thenCompose(youtubePage -> (CompletionStage<YoutubePage>) youtubePage);

        // Step 6: Assert the Video response data
        YoutubePage youtubePage = future.toCompletableFuture().get();
        assertEquals("Mock Video Title", youtubePage.getVideo().getTitle());
        assertEquals("Mock Channel Title", youtubePage.getChannel().getTitle());
    }

    @Test
    public void testActorGetChannelProfile() throws Exception {
        WSClient mockWsClient = Mockito.mock(WSClient.class);
        WSRequest mockRequest = Mockito.mock(WSRequest.class);
        WSResponse mockResponse = Mockito.mock(WSResponse.class);

        Config config = Mockito.mock(Config.class);
        Duration duration = Duration.ofMillis(2000);

        when(mockWsClient.url(anyString())).thenReturn(mockRequest);
        when(mockRequest.addQueryParameter(anyString(), anyString())).thenReturn(mockRequest);
        when(config.getString("api.key")).thenReturn("mock-api-key");
        when(config.getLong("pekko.ask.duration")).thenReturn(2000L);

        // Step 2: Prepare JSON response as per YouTube API
        String jsonVideoResponse1 = "{\n" +
                "  \"items\": [{\n" +
                "    \"snippet\": {\n" +
                "      \"title\": \"Mock Playlist Video Title 1\",\n" +
                "      \"description\": \"Description for video 1\",\n" +
                "      \"thumbnails\": { \"medium\": { \"url\": \"https://mockurl1.com/thumbnail.jpg\" } },\n" +
                "      \"channelId\": \"mockChannelId\",\n" +
                "      \"tags\": [\"mockTag1\", \"mockTag2\"]\n" +
                "    }\n" +
                "  }]\n" +
                "}";

        String jsonVideoResponse2 = "{\n" +
                "  \"items\": [{\n" +
                "    \"snippet\": {\n" +
                "      \"title\": \"Mock Playlist Video Title 2\",\n" +
                "      \"description\": \"Description for video 2\",\n" +
                "      \"thumbnails\": { \"medium\": { \"url\": \"https://mockurl2.com/thumbnail.jpg\" } },\n" +
                "      \"channelId\": \"mockChannelId\",\n" +
                "      \"tags\": [\"mockTag1\", \"mockTag2\"]\n" +
                "    }\n" +
                "  }]\n" +
                "}";

        String jsonVideoResponse3 = "{\n" +
                "  \"items\": [{\n" +
                "    \"snippet\": {\n" +
                "      \"title\": \"Mock Playlist Video Title 3\",\n" +
                "      \"description\": \"Description for video 3\",\n" +
                "      \"thumbnails\": { \"medium\": { \"url\": \"https://mockurl3.com/thumbnail.jpg\" } },\n" +
                "      \"channelId\": \"mockChannelId\",\n" +
                "      \"tags\": [\"mockTag1\", \"mockTag2\"]\n" +
                "    }\n" +
                "  }]\n" +
                "}";

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

        String jsonPlaylistResponse = "{\n" +
                "  \"items\": [\n" +
                "    {\n" +
                "      \"snippet\": {\n" +
                "        \"title\": \"Mock Playlist Video Title 1\",\n" +
                "        \"description\": \"Description for video 1\",\n" +
                "        \"thumbnails\": { \"medium\": { \"url\": \"https://mockurl1.com/thumbnail.jpg\" } },\n" +
                "        \"channelId\": \"mockChannelId\",\n" +
                "        \"resourceId\": { \"videoId\": \"mockVideoId1\" }\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"snippet\": {\n" +
                "        \"title\": \"Mock Playlist Video Title 2\",\n" +
                "        \"description\": \"Description for video 2\",\n" +
                "        \"thumbnails\": { \"medium\": { \"url\": \"https://mockurl2.com/thumbnail.jpg\" } },\n" +
                "        \"channelId\": \"mockChannelId\",\n" +
                "        \"resourceId\": { \"videoId\": \"mockVideoId2\" }\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"snippet\": {\n" +
                "        \"title\": \"Mock Playlist Video Title 3\",\n" +
                "        \"description\": \"Description for video 3\",\n" +
                "        \"thumbnails\": { \"medium\": { \"url\": \"https://mockurl3.com/thumbnail.jpg\" } },\n" +
                "        \"channelId\": \"mockChannelId\",\n" +
                "        \"resourceId\": { \"videoId\": \"mockVideoId3\" }\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";


        // Convert String to JsonNode
        ObjectMapper channelMapper = new ObjectMapper();
        JsonNode jsonChannelNode = channelMapper.readTree(jsonChannelResponse);

        ObjectMapper playlistMapper = new ObjectMapper();
        JsonNode jsonPlaylistNode = playlistMapper.readTree(jsonPlaylistResponse);

        ObjectMapper videMapper1 = new ObjectMapper();
        JsonNode jsonVideo1Node = videMapper1.readTree(jsonVideoResponse1);

        ObjectMapper videMapper2 = new ObjectMapper();
        JsonNode jsonVideo2Node = videMapper2.readTree(jsonVideoResponse2);

        ObjectMapper videMapper3 = new ObjectMapper();
        JsonNode jsonVideo3Node = videMapper3.readTree(jsonVideoResponse3);

        // Configure mock response to return our JSON
        when(mockResponse.asJson())
                .thenReturn(jsonChannelNode)
                .thenReturn(jsonPlaylistNode)
                .thenReturn(jsonVideo1Node)
                .thenReturn(jsonVideo2Node)
                .thenReturn(jsonVideo3Node);

        when(mockRequest.get()).thenReturn(CompletableFuture.completedFuture(mockResponse));

        ActorRef storeActor = system.actorOf(StoreActor.props(mockWsClient, config), "storeActor-7");

        // Step 5: Test the actor's GetVideo message handling
        CompletionStage<ChannelProfile> future = ask(storeActor, new StoreActor.GetChannelProfile("mockChannelId"), duration)
                .thenCompose(channelProfile -> (CompletionStage<ChannelProfile>) channelProfile);

        // Step 6: Assert the Video response data
        ChannelProfile channelProfile = future.toCompletableFuture().get();
        assertEquals("Mock Channel Title", channelProfile.getChannel().getTitle());
        assertEquals("Mock Playlist Video Title 1", channelProfile.getLatestVideos().get(0).getTitle());
        assertEquals("https://mockurl2.com/thumbnail.jpg", channelProfile.getLatestVideos().get(1).getThumbnail());
        assertEquals("Description for video 3", channelProfile.getLatestVideos().get(2).getDescription());
    }
}
