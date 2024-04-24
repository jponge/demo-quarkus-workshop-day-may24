package demo.quarkus.reactive.user.webapp;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/config")
public class FrontendConfig {

  @ConfigProperty(name = "user-api.endpoint")
  String userApiEndpoint;


  @GET
  @Path("user-api-endpoint")
  public Uni<String> getUserApiEndpoint() {
    Log.info("User API endpoint is at " + userApiEndpoint);
    return Uni.createFrom().item(userApiEndpoint);
  }
}
