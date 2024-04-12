package demo.quarkus.reactive;

import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.sqlclient.Row;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.RestResponse.ResponseBuilder;

public class RankingResource {

  @Path("/ranking-last-24-hours")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public JsonObject ranking() {
    // FIXME
    return new JsonObject();
  }
}
