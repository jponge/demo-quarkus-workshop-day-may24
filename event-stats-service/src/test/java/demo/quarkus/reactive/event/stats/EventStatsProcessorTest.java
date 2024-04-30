package demo.quarkus.reactive.event.stats;

import io.quarkus.kafka.client.serialization.JsonObjectSerde;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.quarkus.test.kafka.KafkaCompanionResource;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.RoutingContext;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

@QuarkusTest
@QuarkusTestResource(KafkaCompanionResource.class)
class EventStatsProcessorTest {

  @InjectKafkaCompanion
  KafkaCompanion companion;

  Vertx vertx;

  @BeforeEach
  void setUp() {
    companion.registerSerde(JsonObject.class, new JsonObjectSerde());
    Set<String> topicsToClear = Set.of(
      "incoming.steps",
      "event-stats.throughput",
      "daily.step.updates",
      "event-stats.user-activity.updates",
      "event-stats.city-trend.updates"
    );
    for (String topic : companion.topics().list()) {
      if (topicsToClear.contains(topic)) {
        companion.topics().clear(topic);
      }
    }
    vertx = Vertx.vertx();
    vertx.deployVerticle(new MockUserProfileServer()).await().indefinitely();
  }

  @Test
  @DisplayName("Incoming activity throughput computation")
  void throughput() {
    for (int i = 0; i < 10; i++) {
      companion.produce(JsonObject.class).fromRecords(List.of(incomingStepsRecord("abc", (long) i, 10)));
    }
    List<ConsumerRecord<String, JsonObject>> records = companion.consume(JsonObject.class)
      .fromTopics("event-stats.throughput", 1)
      .awaitCompletion()
      .getRecords();
    JsonObject data = records.getFirst().value();
    assertThat(data.getInteger("seconds")).isEqualTo(5);
    assertThat(data.getInteger("count")).isEqualTo(10);
    assertThat(data.getDouble("throughput")).isCloseTo(2.0d, offset(0.01d));
  }

  private ProducerRecord<String, JsonObject> incomingStepsRecord(String deviceId, long syncId, long steps) {
    LocalDateTime now = LocalDateTime.now();
    String key = deviceId + ":" + now.getYear() + "-" + now.getMonth() + "-" + now.getDayOfMonth();
    JsonObject json = new JsonObject()
      .put("deviceId", deviceId)
      .put("syncId", syncId)
      .put("stepsCount", steps);
    return new ProducerRecord<>("incoming.steps", key, json);
  }

  @Test
  @DisplayName("User activity updates")
  void userActivityUpdate() {
    companion.produce(JsonObject.class).fromRecords(List.of(dailyStepsUpdateRecord("abc", 2500)));
    List<ConsumerRecord<String, JsonObject>> records = companion.consume(JsonObject.class)
      .fromTopics("event-stats.user-activity.updates", 1)
      .awaitCompletion()
      .getRecords();
    JsonObject data = records.getFirst().value();
    assertThat(data.getString("deviceId")).isEqualTo("abc");
    assertThat(data.getString("username")).isEqualTo("Foo");
    assertThat(data.getInteger("stepsCount")).isEqualTo(2500);
    assertThat(data.containsKey("timestamp")).isTrue();
    assertThat(data.containsKey("city")).isTrue();
    assertThat(data.containsKey("makePublic")).isTrue();
  }

  private ProducerRecord<String, JsonObject> dailyStepsUpdateRecord(String deviceId, long steps) {
    LocalDateTime now = LocalDateTime.now();
    String key = deviceId + ":" + now.getYear() + "-" + now.getMonth() + "-" + now.getDayOfMonth();
    JsonObject json = new JsonObject()
      .put("deviceId", deviceId)
      .put("timestamp", now.toString())
      .put("stepsCount", steps);
    return new ProducerRecord<>("daily.step.updates", key, json);
  }

  @Test
  @DisplayName("City trend updates")
  void cityTrendUpdate() {
    companion.produce(JsonObject.class).fromRecords(List.of(
      dailyStepsUpdateRecord("abc", 2500),
      dailyStepsUpdateRecord("abc", 3500)
    ));
    List<ConsumerRecord<String, JsonObject>> records = companion.consume(JsonObject.class)
      .fromTopics("event-stats.city-trend.updates", 1)
      .awaitCompletion()
      .getRecords();
    JsonObject data = records.getFirst().value();
    assertThat(data.getInteger("seconds")).isEqualTo(5);
    assertThat(data.getInteger("updates")).isEqualTo(2);
    assertThat(data.getLong("stepsCount")).isEqualTo(1000L);
    assertThat(data.getString("city")).isEqualTo("Lyon");
  }

  @AfterEach
  void tearDown() {
    vertx.close().await().indefinitely();
  }

  private static class MockUserProfileServer extends AbstractVerticle {

    private String deviceId;

    @Override
    public Uni<Void> asyncStart() {
      Router router = Router.router(vertx);
      router.get("/owns/:deviceId").handler(this::owns);
      router.get("/:username").handler(this::username);
      return vertx.createHttpServer()
        .requestHandler(router)
        .listen(3000)
        .replaceWithVoid();
    }

    private void owns(RoutingContext rc) {
      deviceId = rc.pathParam("deviceId");
      JsonObject notAllData = new JsonObject()
        .put("username", "Foo")
        .put("deviceId", deviceId);
      rc.response()
        .putHeader("Content-Type", "application/json")
        .endAndForget(notAllData.encode());
    }

    private void username(RoutingContext rc) {
      JsonObject notAllData = new JsonObject()
        .put("username", "Foo")
        .put("email", "foo@mail.tld")
        .put("deviceId", deviceId)
        .put("city", "Lyon")
        .put("makePublic", true);
      rc.response()
        .putHeader("Content-Type", "application/json")
        .endAndForget(notAllData.encode());
    }
  }
}
