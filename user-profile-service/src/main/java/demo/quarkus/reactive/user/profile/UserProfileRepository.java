package demo.quarkus.reactive.user.profile;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserProfileRepository implements PanacheRepository<UserProfile> {

  public Uni<UserProfile> save(UserProfile userProfile) {
    if (userProfile.makePublic == null) {
      userProfile.makePublic = Boolean.FALSE;
    }
    // Persist and flush in one method call
    return persistAndFlush(userProfile);
  }

  public Uni<UserProfileFetch> findByUsername(String username) {
    // Panache query: select user profiles filtering by username
    return find("username", username)
      // Projection class: only the necessary columns will be fetched
      .project(UserProfileFetch.class)
      .singleResult();
  }

  @WithTransaction
  public Uni<Void> update(String username, UserProfileUpdate update) {
    return find("username", username).singleResult()
      .onItem().ifNotNull().invoke(entity -> {
        // Managed entity update
        update.applyTo(entity);
      })
      .replaceWithVoid();
  }

  public Uni<UserProfileOwner> findByDeviceId(String deviceId) {
    return find("deviceId", deviceId).project(UserProfileOwner.class).singleResult();
  }

  public Uni<Boolean> authenticate(Credentials credentials) {
    return find("username", credentials.username()).singleResult()
      // Awful, don't persist clear text password in a production database
      .onItem().transform(entity -> entity.password.equals(credentials.password()))
      .onFailure().recoverWithItem(false);
  }
}
