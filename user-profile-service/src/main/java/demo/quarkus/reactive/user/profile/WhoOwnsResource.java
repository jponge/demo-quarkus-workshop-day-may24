package demo.quarkus.reactive.user.profile;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.persistence.NoResultException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;

@Path("owns")
public class WhoOwnsResource {

  @Inject
  UserProfileRepository repository;

  @Path("{deviceId}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Uni<UserProfileOwner> whoOwns(String deviceId) {
    Log.info("Checking who owns " + deviceId);

    return repository.findByDeviceId(deviceId)
      .onFailure(NoResultException.class).transform(ignored -> new WebApplicationException(404));
  }
}
