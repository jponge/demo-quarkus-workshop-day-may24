package demo.quarkus.reactive.ingestion;

import io.quarkus.kafka.client.serialization.JsonObjectSerde;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.quarkus.test.kafka.KafkaCompanionResource;
import io.restassured.http.ContentType;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;
import io.vertx.core.json.JsonObject;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@TestHTTPEndpoint(IngestResource.class)
@QuarkusTestResource(KafkaCompanionResource.class)
class IngestResourceTest {

  @InjectKafkaCompanion
  KafkaCompanion companion;

  @BeforeEach
  void setUp() {
    companion.registerSerde(JsonObject.class, new JsonObjectSerde());
    companion.getOrCreateAdminClient().deleteTopics(List.of("incoming.steps"));
  }

  @Test
  @DisplayName("Ingest a well-formed JSON data over HTTP")
  void httpIngest() {
    JsonObject body = new JsonObject()
      .put("deviceId", "456")
      .put("deviceSync", 3L)
      .put("stepsCount", 125);

    given()
      .contentType(ContentType.JSON)
      .body(body.encode())
      .post()
      .then()
      .assertThat()
      .statusCode(200);

    List<ConsumerRecord<String, JsonObject>> records = companion.consume(JsonObject.class)
      .fromTopics("incoming.steps", 1)
      .awaitCompletion()
      .getRecords();

    JsonObject json = records.getFirst().value();
    assertThat(json.getString("deviceId")).isEqualTo("456");
    assertThat(json.getLong("deviceSync")).isEqualTo(3L);
    assertThat(json.getInteger("stepsCount")).isEqualTo(125);
  }

  @Test
  @DisplayName("Ingest a badly-formed JSON data over HTTP and observe no Kafka record")
  void httpIngestWrong() {
    JsonObject body = new JsonObject();

    given()
      .contentType(ContentType.JSON)
      .body(body.encode())
      .post()
      .then()
      .assertThat()
      .statusCode(400);

    companion.consume(JsonObject.class)
      .fromTopics("incoming.steps", 1)
      .awaitNoRecords(Duration.ofSeconds(5));
  }
}
