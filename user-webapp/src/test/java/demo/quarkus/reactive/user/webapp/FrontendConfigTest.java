package demo.quarkus.reactive.user.webapp;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class FrontendConfigTest {

  @Test
  @DisplayName("Check that the user API endpoint configuration is provided")
  void checkUserApiEndpointConfiguration() {
    String conf = when()
      .get("/config/user-api-endpoint")
      .then()
      .assertThat()
      .statusCode(200)
      .extract().asString();

    assertThat(conf).isEqualTo("http://foo.bar:4444");
  }
}
