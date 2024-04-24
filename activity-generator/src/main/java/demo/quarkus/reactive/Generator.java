package demo.quarkus.reactive;

import io.quarkus.logging.Log;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.random.RandomGenerator;

@QuarkusMain
public class Generator implements QuarkusApplication {

  @Inject
  Vertx vertx;

  WebClient webClient;
  JsonObject generatorData;
  RandomGenerator randomGenerator = RandomGenerator.getDefault();

  @Override
  public int run(String... args) throws Exception {
    webClient = WebClient.create(vertx);

    generatorData = readData(args);
    createProfiles(args);
    generateTraffic();

    return 0;
  }

  private JsonObject readData(String[] args) throws IOException {
    String dataFile = "generator.json";
    if (args.length >= 1) {
      dataFile = args[0];
    }
    String payload = Files.readString(Path.of(dataFile));
    return (JsonObject) Json.decodeValue(payload);
  }

  private void createProfiles(String[] args) {
    JsonObject conf = generatorData.getJsonObject("public-api");
    String host = conf.getString("hostname", "localhost");
    int port = conf.getInteger("port", 4000);
    HttpRequest<Buffer> request = webClient
      .post(port, host, "/register")
      .putHeader("Content-Type", "application/json");
    JsonArray array = generatorData.getJsonArray("profiles");
    for (int i = 0; i < array.size(); i++) {
      JsonObject profile = array.getJsonObject(i);
      Buffer buffer = Buffer.buffer(profile.encode());
      HttpResponse<Buffer> response = request.sendBuffer(buffer).await().atMost(Duration.ofSeconds(5));
      if (response.statusCode() == 200) {
        Log.info("Registered profile: " + profile.getString("username"));
      } else {
        Log.warn("Failed to register profile: " + profile.getString("username") + " - " + response.statusCode());
      }
    }
  }

  private void generateTraffic() {
    generatorData.getJsonArray("profiles").stream()
      .map(JsonObject.class::cast)
      .map(entry -> entry.getString("deviceId"))
      .forEach(this::scheduleNext);
  }

  private void scheduleNext(String deviceId) {
    JsonObject conf = generatorData.getJsonObject("ingestion");
    String host = conf.getString("hostname", "localhost");
    int port = conf.getInteger("port", 4000);
    vertx.setTimer(randomGenerator.nextInt(100, 5000), tick -> {
      JsonObject payload = new JsonObject()
        .put("deviceId", deviceId)
        .put("deviceSync", System.currentTimeMillis())
        .put("stepsCount", randomGenerator.nextInt(5, 50));
      webClient.post(port, host, "/ingest")
        .putHeader("Content-Type", "application/json")
        .sendBuffer(Buffer.buffer(payload.encode()))
        .onItemOrFailure().invoke(() -> scheduleNext(deviceId))
        .subscribe().with(
          response -> {
            Log.info("Sent " + payload.getInteger("stepsCount") + " steps for " + deviceId);
          },
          err -> Log.warn("HTTP request failed", err)
        );
    });
  }
}
