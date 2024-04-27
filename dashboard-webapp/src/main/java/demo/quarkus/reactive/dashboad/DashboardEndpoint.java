package demo.quarkus.reactive.dashboad;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.mutiny.ext.web.codec.BodyCodec;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.jboss.resteasy.reactive.RestStreamElementType;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.eclipse.microprofile.reactive.messaging.Acknowledgment.Strategy.PRE_PROCESSING;

@Path("/dashboard")
public class DashboardEndpoint {

  @ConfigProperty(name = "backend.user-service.host")
  String userServiceHost;

  @ConfigProperty(name = "backend.user-service.port")
  int userServicePort;

  @ConfigProperty(name = "backend.activity-service.host")
  String activityServiceHost;

  @ConfigProperty(name = "backend.activity-service.port")
  int activityServicePort;

  @Inject
  Vertx vertx;

  final Map<String, JsonObject> publicRanking = new ConcurrentHashMap<>();
  WebClient webClient;

  @PostConstruct
  void init() {
    webClient = WebClient.create(vertx);

    hydrate().subscribe().with(
      ok -> Log.info("Hydration completed with " + publicRanking.size() + " entries"),
      err -> Log.error("Hydration failed", err)
    );
  }

  @Inject
  @Channel("throughput")
  Multi<JsonObject> throughputEvents;

  @GET
  @Path("throughput")
  @RestStreamElementType(MediaType.APPLICATION_JSON)
  public Multi<JsonObject> throughputStream() {
    Log.info("New throughput stream subscriber");
    return throughputEvents;
  }

  @Inject
  @Channel("city-trends")
  Multi<JsonObject> cityTrendEvents;

  @GET
  @Path("city-trends")
  @RestStreamElementType(MediaType.APPLICATION_JSON)
  public Multi<JsonObject> cityTrendStream() {
    Log.info("New city trend stream subscriber");
    return cityTrendEvents;
  }

  @Incoming("user-activity")
  @Acknowledgment(PRE_PROCESSING)
  @Outgoing("public-ranking")
  public Multi<JsonArray> maintainPublicRankings(Multi<JsonObject> stream) {
    return stream
      .select().where(this::activityIsPublic)
      .group().intoLists().every(Duration.ofSeconds(5))
      .onItem().transform(this::computeRanking);
  }

  @Inject
  @Channel("public-ranking")
  Multi<JsonArray> publicRankingStream;

  @GET
  @Path("public-ranking")
  @RestStreamElementType(MediaType.APPLICATION_JSON)
  public Multi<JsonArray> publicRankingStream() {
    Log.info("New public ranking stream subscriber");

    return Multi.createBy().concatenating().streams(
      Multi.createFrom().item(computeRanking()),
      publicRankingStream
    );
  }

  private boolean activityIsPublic(JsonObject record) {
    return record.getBoolean("makePublic");
  }

  private JsonArray computeRanking(List<JsonObject> updates) {
    Log.info("Updating rankings with " + updates.size() + " updates");
    copyBetterScores(updates);
    pruneOldEntries();
    return computeRanking();
  }

  private void copyBetterScores(List<JsonObject> updates) {
    for (JsonObject update : updates) {
      long stepsCount = update.getLong("stepsCount");
      JsonObject previousData = publicRanking.get(update.getString("username"));
      if (previousData == null || previousData.getLong("stepsCount") < stepsCount) {
        publicRanking.put(update.getString("username"), update);
      }
    }
  }

  private void pruneOldEntries() {
    Instant now = Instant.now();
    Iterator<Map.Entry<String, JsonObject>> iterator = publicRanking.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<String, JsonObject> entry = iterator.next();
      Instant timestamp = Instant.parse(entry.getValue().getString("timestamp"));
      if (timestamp.until(now, ChronoUnit.DAYS) >= 1L) {
        iterator.remove();
      }
    }
  }

  private JsonArray computeRanking() {
    List<JsonObject> ranking = publicRanking.entrySet()
      .stream()
      .map(Map.Entry::getValue)
      .sorted(this::compareStepsCountInReverseOrder)
      .map(json -> new JsonObject()
        .put("username", json.getString("username"))
        .put("stepsCount", json.getLong("stepsCount"))
        .put("city", json.getString("city")))
      .collect(Collectors.toList());
    return new JsonArray(ranking);
  }

  private int compareStepsCountInReverseOrder(JsonObject a, JsonObject b) {
    Long first = a.getLong("stepsCount");
    Long second = b.getLong("stepsCount");
    return second.compareTo(first);
  }

  Uni<Void> hydrate() {
    return webClient.get(activityServicePort, activityServiceHost, "/ranking/last-24-hours")
      .as(BodyCodec.jsonArray())
      .send()
      .onFailure().retry().withBackOff(Duration.ofMillis(500)).atMost(5)
      .onItem().transform(HttpResponse::body)
      .onItem().transformToMulti(array -> Multi.createFrom().iterable(array))
      .onItem().transform(JsonObject.class::cast)
      .onItem().transformToUniAndConcatenate(this::whoOwnsDevice)
      .onItem().transformToUniAndConcatenate(this::fillWithUserProfile)
      .onItem().invoke(this::hydrateEntryIfPublic)
      .collect().asList()
      .replaceWithVoid();
  }

  Uni<JsonObject> fillWithUserProfile(JsonObject json) {
    return webClient
      .get(userServicePort, userServiceHost, "/" + json.getString("username"))
      .as(BodyCodec.jsonObject())
      .send()
      .onFailure().retry().withBackOff(Duration.ofMillis(500)).atMost(5)
      .map(HttpResponse::body)
      .map(resp -> resp.mergeIn(json));
  }

  Uni<JsonObject> whoOwnsDevice(JsonObject json) {
    return webClient
      .get(userServicePort, userServiceHost, "/owns/" + json.getString("deviceId"))
      .as(BodyCodec.jsonObject())
      .send()
      .onFailure().retry().withBackOff(Duration.ofMillis(500)).atMost(5)
      .map(HttpResponse::body)
      .map(resp -> resp.mergeIn(json));
  }

  void hydrateEntryIfPublic(JsonObject data) {
    if (data.getBoolean("makePublic")) {
      data.put("timestamp", Instant.now().toString());
      publicRanking.put(data.getString("username"), data);
    }
  }
}
