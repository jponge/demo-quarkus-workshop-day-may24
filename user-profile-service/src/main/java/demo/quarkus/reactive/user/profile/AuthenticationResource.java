package demo.quarkus.reactive.user.profile;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestResponse;

@Path("authenticate")
public class AuthenticationResource {

  @Inject
  UserProfileRepository repository;

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public Uni<RestResponse<Void>> authenticate(Credentials credentials) {
    Log.info("Authenticating " + credentials.username());

    return repository.authenticate(credentials)
      .onItem().transform(authenticated -> authenticated ? RestResponse.ok() : RestResponse.status(401));
  }
}
