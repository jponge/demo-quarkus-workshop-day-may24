package demo.quarkus.reactive.activity;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.Tuple;
import io.vertx.pgclient.PgException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import java.time.LocalDateTime;

import static java.time.temporal.ChronoUnit.SECONDS;

@ApplicationScoped
public class EventsProcessor {

  @Inject
  PgPool pgPool;

  @Incoming("steps")
  @Retry(delay = 10, delayUnit = SECONDS)
  @Outgoing("records")
  public Uni<JsonObject> insertRecord(JsonObject data) {
    Log.info("Incoming steps update: " + data.encode());

    Tuple values = Tuple.of(
      data.getString("deviceId"),
      data.getLong("deviceSync"),
      data.getInteger("stepsCount"));

    return pgPool.preparedQuery(SqlQueries.insertStepEvent())
      .execute(values)
      .replaceWith(data)
      .onFailure(EventsProcessor::isDuplicateKeyInsert).recoverWithItem(data);
  }

  private static boolean isDuplicateKeyInsert(Throwable err) {
    return (err instanceof PgException) && "23505".equals(((PgException) err).getSqlState());
  }

  @Incoming("records")
  @Outgoing("updates")
  public Uni<KafkaRecord<String, JsonObject>> generateActivityUpdate(JsonObject data) {
    Log.info("Incoming activity update: " + data.encode());

    String deviceId = data.getString("deviceId");
    LocalDateTime now = LocalDateTime.now();
    String key = deviceId + ":" + now.getYear() + "-" + now.getMonth() + "-" + now.getDayOfMonth();

    return pgPool.preparedQuery(SqlQueries.stepsCountForToday())
      .execute(Tuple.of(deviceId))
      .onItem().transform(rs -> {
        Row row = rs.iterator().next();

        JsonObject payload = new JsonObject()
          .put("deviceId", deviceId)
          .put("timestamp", row.getTemporal(0).toString())
          .put("stepsCount", row.getLong(1));

        return KafkaRecord.of(key, payload);
      });
  }
}
