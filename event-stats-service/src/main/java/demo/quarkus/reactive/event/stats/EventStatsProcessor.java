package demo.quarkus.reactive.event.stats;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.mutiny.ext.web.client.predicate.ResponsePredicate;
import io.vertx.mutiny.ext.web.codec.BodyCodec;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.eclipse.microprofile.reactive.messaging.Acknowledgment.Strategy.PRE_PROCESSING;

@ApplicationScoped
public class EventStatsProcessor {

  @Inject
  Vertx vertx;

  private WebClient webClient;

  @PostConstruct
  void init() {
    webClient = WebClient.create(vertx);
  }

  @Incoming("steps")
  @Acknowledgment(PRE_PROCESSING)
  @Outgoing("throughput")
  public Multi<JsonObject> throughput(Multi<JsonObject> stream) {
    return stream
      .group().intoLists().every(Duration.ofSeconds(5))
      .onItem().transform(EventStatsProcessor::computeThroughput);
  }

  private static JsonObject computeThroughput(List<JsonObject> list) {
    return new JsonObject()
      .put("seconds", 5)
      .put("count", list.size())
      .put("throughput", (((double) list.size()) / 5.0d));
  }

  @Incoming("updates")
  @Acknowledgment(PRE_PROCESSING)
  @Outgoing("userActivityOut")
  public Multi<Message<JsonObject>> userActivity(Multi<Message<JsonObject>> stream) {
    return stream
      .onItem().transformToUniAndConcatenate(this::addDeviceOwner)
      .onItem().transformToUniAndConcatenate(this::addOwnerData)
      .onItem().transform(EventStatsProcessor::computeUserActivity);
  }

  private Uni<Message<JsonObject>> addDeviceOwner(Message<JsonObject> record) {
    JsonObject payload = record.getPayload();
    String deviceId = payload.getString("deviceId");
    return webClient.get(3000, "localhost", "/owns/" + deviceId)
      .expect(ResponsePredicate.SC_OK)
      .expect(ResponsePredicate.JSON)
      .as(BodyCodec.jsonObject())
      .send()
      .onItem().transform(HttpResponse::body)
      .onItem().transform(payload::mergeIn)
      .onItem().transform(record::withPayload)
      .onFailure().retry().withBackOff(Duration.ofSeconds(10)).atMost(3);
  }

  private Uni<Message<JsonObject>> addOwnerData(Message<JsonObject> record) {
    JsonObject payload = record.getPayload();
    String username = payload.getString("username");
    return webClient.get(3000, "localhost", "/" + username)
      .expect(ResponsePredicate.SC_OK)
      .expect(ResponsePredicate.JSON)
      .as(BodyCodec.jsonObject())
      .send()
      .onItem().transform(HttpResponse::body)
      .onItem().transform(payload::mergeIn)
      .onItem().transform(record::withPayload)
      .onFailure().retry().withBackOff(Duration.ofSeconds(10)).atMost(3);
  }

  private static Message<JsonObject> computeUserActivity(Message<JsonObject> message) {
    JsonObject payload = message.getPayload();
    return KafkaRecord.of(payload.getString("username"), payload);
  }

  @Incoming("userActivityIn")
  @Acknowledgment(PRE_PROCESSING)
  @Outgoing("cityTrends")
  public Multi<Message<JsonObject>> cityTrends(Multi<Message<JsonObject>> stream) {
    return stream
      .group().by(msg -> extractCity(msg.getPayload()))
      .onItem().transformToMultiAndConcatenate(byCity -> byCity.group().intoLists().every(Duration.ofSeconds(5)))
      .onItem().transform(EventStatsProcessor::computeCityTrend);
  }

  private static String extractCity(JsonObject jsonObject) {
    return jsonObject.getString("city");
  }

  private static Message<JsonObject> computeCityTrend(List<Message<JsonObject>> list) {
    String city = extractCity(list.getFirst().getPayload());
    long stepsCount = 0;
    for (Message<JsonObject> entry : list) {
      stepsCount += entry.getPayload().getLong("stepsCount");
    }
    JsonObject payload = new JsonObject()
      .put("timestamp", LocalDateTime.now().toString())
      .put("seconds", 5)
      .put("city", city)
      .put("stepsCount", stepsCount)
      .put("updates", list.size());
    return KafkaRecord.of(city, payload);
  }

  @PreDestroy
  void destroy() {
    webClient.close();
  }
}
