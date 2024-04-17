package demo.quarkus.reactive.user.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Entity
public class UserProfile {

  @GeneratedValue
  @Id
  private Long id;

  @NotNull
  @Pattern(regexp = "\\w[\\w+|-]*")
  @Column(unique = true)
  public String username;

  @NotNull
  @NotBlank
  public String password;

  @NotNull
  // Email regexp from https://www.owasp.org/index.php/OWASP_Validation_Regex_Repository
  @Email(regexp = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$")
  public String email;

  @NotNull
  @Pattern(regexp = "\\w[\\w+|-]*")
  @Column(unique = true)
  public String deviceId;

  @NotNull
  @NotBlank
  public String city;

  @Column(nullable = false, columnDefinition = "boolean default false")
  public Boolean makePublic;
}
