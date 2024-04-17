package demo.quarkus.reactive.user.profile;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.persistence.NoResultException;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestResponse;

@Path("{username}")
public class UserProfileResource {

  @Inject
  UserProfileRepository repository;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Uni<UserProfileFetch> fetchUser(String username) {
    return repository.findByUsername(username)
      .onFailure(NoResultException.class).transform(ignored -> new WebApplicationException(404));
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Uni<RestResponse<Void>> updateUser(String username, @Valid UserProfileUpdate update) {
    return repository.update(username, update).replaceWith(RestResponse.ok());
  }
}
