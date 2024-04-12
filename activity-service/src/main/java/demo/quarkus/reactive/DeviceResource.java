package demo.quarkus.reactive;

import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.sqlclient.Row;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestResponse;

@Path("device")
public class DeviceResource {

  @Path("{deviceId}/total")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public JsonObject total(String deviceId) {
    // FIXME
    return new JsonObject();
  }

  @Path("{deviceId}/{year}/{month}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public JsonObject stepsOnMonth(String deviceId, int year, int month) {
    // FIXME
    return new JsonObject();
  }

  @Path("{deviceId}/{year}/{month}/{day}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public JsonObject stepsOnDay(String deviceId, int year, int month, int day) {
    // FIXME
    return new JsonObject();
  }

  private RestResponse<JsonObject> countResponse(Row row) {
    Integer count = row.getInteger(0);
    if (count != null) {
      JsonObject payload = new JsonObject()
        .put("count", count);
      return RestResponse.ResponseBuilder.ok(payload).build();
    }
    return RestResponse.ResponseBuilder.<JsonObject>notFound().build();
  }
}
