package demo.quarkus.reactive.activity;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestResponse;

import java.time.DateTimeException;
import java.time.LocalDateTime;

@Path("device")
public class DeviceResource {

  @Inject
  PgPool pgPool;

  @Path("{deviceId}/total")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Uni<RestResponse<JsonObject>> total(String deviceId) {
    Log.info("Total steps for deviceId=" + deviceId);

    Tuple params = Tuple.of(deviceId);
    return pgPool.preparedQuery(SqlQueries.totalStepsCount())
      .execute(params)
      .onItem().transform(DeviceResource::countResponse);
  }

  @Path("{deviceId}/{year}/{month}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Uni<RestResponse<JsonObject>> stepsOnMonth(String deviceId, int year, int month) {
    Log.info("Steps for year=" + year + " month=" + month + " deviceId=" + deviceId);

    try {
      LocalDateTime dateTime = LocalDateTime.of(year, month, 1, 0, 0);
      Tuple params = Tuple.of(deviceId, dateTime);
      return pgPool.preparedQuery(SqlQueries.monthlyStepsCount())
        .execute(params)
        .onItem().transform(DeviceResource::countResponse);
    } catch (DateTimeException | NumberFormatException e) {
      throw new BadRequestException(e);
    }
  }

  @Path("{deviceId}/{year}/{month}/{day}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Uni<RestResponse<JsonObject>> stepsOnDay(String deviceId, int year, int month, int day) {
    Log.info("Steps for year=" + year + " month=" + month + " day=" + day + " deviceId=" + deviceId);

    try {
      LocalDateTime dateTime = LocalDateTime.of(year, month, day, 0, 0);
      Tuple params = Tuple.of(deviceId, dateTime);
      return pgPool.preparedQuery(SqlQueries.dailyStepsCount())
        .execute(params)
        .onItem().transform(DeviceResource::countResponse);
    } catch (DateTimeException | NumberFormatException e) {
      throw new BadRequestException(e);
    }
  }

  private static RestResponse<JsonObject> countResponse(RowSet<Row> rs) {
    Row row = rs.iterator().next();
    Integer count = row.getInteger(0);
    if (count != null) {
      JsonObject payload = new JsonObject()
        .put("count", count);
      return RestResponse.ResponseBuilder.ok(payload).build();
    }
    return RestResponse.ResponseBuilder.<JsonObject>notFound().build();
  }
}
