package demo.quarkus.reactive.congrats;

import io.quarkiverse.mailpit.test.InjectMailbox;
import io.quarkiverse.mailpit.test.Mailbox;
import io.quarkiverse.mailpit.test.WithMailbox;
import io.quarkiverse.mailpit.test.model.Address;
import io.quarkiverse.mailpit.test.model.Message;
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
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@QuarkusTest
@QuarkusTestResource(KafkaCompanionResource.class)
@WithMailbox
class CongratsMailerTest {

  @InjectKafkaCompanion
  KafkaCompanion companion;

  @InjectMailbox
  Mailbox mailbox;

  Vertx vertx;

  @BeforeEach
  void setUp() throws Exception {
    companion.registerSerde(JsonObject.class, new JsonObjectSerde());
    companion.topics().clear("daily.step.updates");
    mailbox.clear();
    vertx = Vertx.vertx();
    vertx.deployVerticle(new MockUserProfileServer()).await().indefinitely();
  }

  @AfterEach
  void tearDown() {
    vertx.close().await().indefinitely();
  }

  @Test
  @DisplayName("No email must be sent below 10k steps")
  void checkNothingBelow10k() throws Exception {
    companion.produce(JsonObject.class).fromRecords(List.of(record("123", 5000)));

    Thread.sleep(Duration.ofSeconds(10).toMillis());

    List<Message> message = mailbox.find("is:unread", 0, 1);
    assertThat(message).isEmpty();
  }

  private ProducerRecord<String, JsonObject> record(String deviceId, long steps) {
    LocalDateTime now = LocalDateTime.now();
    String key = deviceId + ":" + now.getYear() + "-" + now.getMonth() + "-" + now.getDayOfMonth();
    JsonObject json = new JsonObject()
      .put("deviceId", deviceId)
      .put("timestamp", now.toString())
      .put("stepsCount", steps);
    return new ProducerRecord<>("daily.step.updates", key, json);
  }

  @Test
  @DisplayName("An email must be sent for 10k+ steps")
  void checkSendsOver10k() throws Exception {
    companion.produce(JsonObject.class).fromRecords(List.of(record("123", 11_000)));

    List<Message> messages = await()
      .atMost(Duration.ofSeconds(10))
      .pollInterval(Duration.ofSeconds(2))
      .until(() -> mailbox.find("is:unread", 0, 1), not(List::isEmpty));

    Message message = messages.getFirst();
    assertThat(message.getFrom()).extracting(Address::getAddress).isEqualTo("noreply@tenksteps.tld");
    assertThat(message.getTo()).extracting(Address::getAddress).containsExactly("foo@mail.tld");
    assertThat(message.getSubject()).isEqualTo("You made it!");
    assertThat(message.getText()).contains("Congratulations on reaching " + 11_000);
  }

  private static class MockUserProfileServer extends AbstractVerticle {

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
      JsonObject notAllData = new JsonObject()
        .put("username", "Foo");
      rc.response()
        .putHeader("Content-Type", "application/json")
        .endAndForget(notAllData.encode());
    }

    private void username(RoutingContext rc) {
      JsonObject notAllData = new JsonObject()
        .put("username", "Foo")
        .put("email", "foo@mail.tld");
      rc.response()
        .putHeader("Content-Type", "application/json")
        .endAndForget(notAllData.encode());
    }
  }
}
