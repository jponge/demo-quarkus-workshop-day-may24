package demo.quarkus.reactive.user.profile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;

public class UserProfileUpdate {

  // Email regexp from https://www.owasp.org/index.php/OWASP_Validation_Regex_Repository
  @Email(regexp = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$")
  public String email;

  @NotEmpty
  public String city;

  public Boolean makePublic;

  public void applyTo(UserProfile userProfile) {
    if (city != null) {
      userProfile.city = city;
    }
    if (email != null) {
      userProfile.email = email;
    }
    if (makePublic != null) {
      userProfile.makePublic = makePublic;
    }
  }
}
