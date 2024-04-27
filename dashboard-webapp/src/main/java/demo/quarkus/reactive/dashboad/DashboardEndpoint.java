package demo.quarkus.reactive.dashboad;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
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
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.jboss.resteasy.reactive.RestStreamElementType;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
