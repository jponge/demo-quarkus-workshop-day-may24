package demo.quarkus.reactive.user.profile;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.hibernate.exception.ConstraintViolationException;
import org.jboss.resteasy.reactive.RestResponse;

@Path("register")
public class RegisterResource {

  @Inject
  UserProfileRepository repository;

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Uni<RestResponse<Void>> register(@Valid UserProfile userProfile) {
    Log.info("Registering " + userProfile.username);

    return repository.save(userProfile)
      .replaceWith(RestResponse.<Void>ok())
      .onFailure(RegisterResource::isDuplicateError).transform(RegisterResource::toConflict);
  }

  private static boolean isDuplicateError(Throwable err) {
    if (err instanceof ConstraintViolationException) {
      ConstraintViolationException cve = (ConstraintViolationException) err;
      return "23505".equals(cve.getSQLState());
    }
    return false;
  }

  private static Throwable toConflict(Throwable err) {
    return new WebApplicationException(409);
  }
}
