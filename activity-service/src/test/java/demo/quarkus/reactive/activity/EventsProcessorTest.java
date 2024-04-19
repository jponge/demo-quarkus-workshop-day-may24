package demo.quarkus.reactive.activity;

import io.quarkus.kafka.client.serialization.JsonObjectSerde;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.quarkus.test.kafka.KafkaCompanionResource;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.pgclient.PgPool;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@QuarkusTestResource(KafkaCompanionResource.class)
@QuarkusTestResource(PgPoolTestResource.class)
class EventsProcessorTest {

  @InjectKafkaCompanion
  KafkaCompanion companion;

  PgPool pgPool;

  @BeforeEach
  void setUp() {
    TestDbSetup.cleanDb(pgPool);
    companion.registerSerde(JsonObject.class, new JsonObjectSerde());
    for (String topic : companion.topics().list()) {
      if ("incoming.steps".equals(topic) || "daily.step.updates".equals(topic)) {
        companion.topics().clear(topic);
      }
    }
  }

  @Test
  @DisplayName("Send events from the same device, and observe that a correct daily steps count event is being produced")
  void observeDailyStepsCountEvent() {
    companion.produce(JsonObject.class).fromRecords(List.of(
      new ProducerRecord<>("incoming.steps", new JsonObject()
        .put("deviceId", "123")
        .put("deviceSync", 1L)
        .put("stepsCount", 200)),
      new ProducerRecord<>("incoming.steps", new JsonObject()
        .put("deviceId", "123")
        .put("deviceSync", 2L)
        .put("stepsCount", 200))
    ));

    List<ConsumerRecord<String, JsonObject>> records = companion.consume(JsonObject.class)
      .fromTopics("daily.step.updates", 1)
      .awaitCompletion()
      .getRecords();
    JsonObject last = records.getFirst().value();
    assertThat(last.getString("deviceId")).isEqualTo("123");
    assertThat(last.containsKey("timestamp")).isTrue();
    assertThat(last.getInteger("stepsCount")).isEqualTo(200);
  }

  @Test
  @DisplayName("Send same event from the same device, and observe that a correct daily steps count event is being produced")
  void observeDailyStepsCountDuplicateEvent() {
    companion.produce(JsonObject.class).fromRecords(List.of(
      new ProducerRecord<>("incoming.steps", new JsonObject()
        .put("deviceId", "123")
        .put("deviceSync", 1L)
        .put("stepsCount", 200)),
      new ProducerRecord<>("incoming.steps", new JsonObject()
        .put("deviceId", "123")
        .put("deviceSync", 1L)
        .put("stepsCount", 200))
    ));

    List<ConsumerRecord<String, JsonObject>> records = companion.consume(JsonObject.class)
      .fromTopics("daily.step.updates", 2)
      .awaitCompletion()
      .getRecords();
    JsonObject last = records.get(1).value();
    assertThat(last.getString("deviceId")).isEqualTo("123");
    assertThat(last.containsKey("timestamp")).isTrue();
    assertThat(last.getInteger("stepsCount")).isEqualTo(200);
  }
}
