package demo.quarkus.reactive;

import io.vertx.core.json.JsonObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("device")
public class DeviceResource {

  @Path("{deviceId}/total")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public JsonObject total(String deviceId) {
    return new JsonObject();
  }
}
