package demo.quarkus.reactive.user.profile;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.pgclient.PgPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.with;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@QuarkusTestResource(PgPoolTestResource.class)
class UserProfileTest {

  PgPool pgPool;

  @BeforeEach
  void setUp() {
    pgPool.query("TRUNCATE TABLE UserProfile").execute().await().indefinitely();
  }

  private JsonObject basicUser() {
    return new JsonObject()
      .put("username", "abc")
      .put("password", "123")
      .put("email", "abc@email.me")
      .put("city", "Lyon")
      .put("deviceId", "a1b2c3")
      .put("makePublic", true);
  }

  @Test
  @DisplayName("Register a user")
  void register() {
    String response = given()
      .contentType(ContentType.JSON)
      .accept(ContentType.JSON)
      .body(basicUser().encode())
      .when()
      .post("/register")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .asString();
    assertThat(response).isEmpty();
  }

  @Test
  @DisplayName("Failing to register with an existing user name")
  void registerExistingUser() {
    given()
      .contentType(ContentType.JSON)
      .accept(ContentType.JSON)
      .body(basicUser().encode())
      .when()
      .post("/register")
      .then()
      .assertThat()
      .statusCode(200);

    given()
      .contentType(ContentType.JSON)
      .accept(ContentType.JSON)
      .body(basicUser().encode())
      .when()
      .post("/register")
      .then()
      .assertThat()
      .statusCode(409);
  }

  @Test
  @DisplayName("Failing to register a user with an already existing device id")
  void registerExistingDeviceId() {
    given()
      .contentType(ContentType.JSON)
      .accept(ContentType.JSON)
      .body(basicUser().encode())
      .when()
      .post("/register")
      .then()
      .assertThat()
      .statusCode(200);

    JsonObject user = basicUser()
      .put("username", "Bean");

    given()
      .contentType(ContentType.JSON)
      .accept(ContentType.JSON)
      .body(user.encode())
      .when()
      .post("/register")
      .then()
      .assertThat()
      .statusCode(409);
  }

  @Test
  @DisplayName("Failing to register with missing fields")
  void registerWIthMissingFields() {
    given()
      .contentType(ContentType.JSON)
      .accept(ContentType.JSON)
      .body(new JsonObject().encode())
      .when()
      .post("/register")
      .then()
      .assertThat()
      .statusCode(400);
  }

  @Test
  @DisplayName("Failing to register with incorrect field data")
  void registerWithWrongFields() {
    JsonObject user = basicUser().put("username", "a b c  ");
    given()
      .contentType(ContentType.JSON)
      .accept(ContentType.JSON)
      .body(user.encode())
      .when()
      .post("/register")
      .then()
      .assertThat()
      .statusCode(400);

    user = basicUser().put("deviceId", "@123");
    given()
      .contentType(ContentType.JSON)
      .accept(ContentType.JSON)
      .body(user.encode())
      .when()
      .post("/register")
      .then()
      .assertThat()
      .statusCode(400);

    user = basicUser().put("password", "    ");
    given()
      .contentType(ContentType.JSON)
      .accept(ContentType.JSON)
      .body(user.encode())
      .when()
      .post("/register")
      .then()
      .assertThat()
      .statusCode(400);
  }

  @Test
  @DisplayName("Register a user then fetch it")
  void registerThenFetch() {
    JsonObject user = basicUser();

    with()
      .contentType(ContentType.JSON)
      .accept(ContentType.JSON)
      .body(user.encode())
      .post("/register");

    JsonPath jsonPath = given()
      .accept(ContentType.JSON)
      .when()
      .get("/" + user.getString("username"))
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath();

    assertThat(jsonPath.getString("username")).isEqualTo(user.getString("username"));
    assertThat(jsonPath.getString("email")).isEqualTo(user.getString("email"));
    assertThat(jsonPath.getString("deviceId")).isEqualTo(user.getString("deviceId"));
    assertThat(jsonPath.getString("city")).isEqualTo(user.getString("city"));
    assertThat(jsonPath.getBoolean("makePublic")).isEqualTo(user.getBoolean("makePublic"));
    assertThat(jsonPath.getString("_id")).isNull();
    assertThat(jsonPath.getString("password")).isNull();
  }

  @Test
  @DisplayName("Fetching an unknown user")
  void fetchUnknownUser() {
    given()
      .accept(ContentType.JSON)
      .when()
      .get("/foo-bar-baz")
      .then()
      .statusCode(404);
  }

  @Test
  @DisplayName("Register then update a user data")
  void update() {
    JsonObject original = basicUser();

    with()
      .contentType(ContentType.JSON)
      .accept(ContentType.JSON)
      .body(original.encode())
      .post("/register");

    JsonObject updated = basicUser();
    updated
      .put("deviceId", "vertx-in-action-123")
      .put("email", "vertx@email.me")
      .put("city", "Nevers")
      .put("makePublic", false)
      .put("username", "Bean");

    with()
      .contentType(ContentType.JSON)
      .body(updated.encode())
      .put("/" + original.getString("username"))
      .then()
      .statusCode(200);

    JsonPath jsonPath = given()
      .accept(ContentType.JSON)
      .when()
      .get("/" + original.getString("username"))
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath();

    assertThat(jsonPath.getString("username")).isEqualTo(original.getString("username"));
    assertThat(jsonPath.getString("deviceId")).isEqualTo(original.getString("deviceId"));

    assertThat(jsonPath.getString("city")).isEqualTo(updated.getString("city"));
    assertThat(jsonPath.getString("email")).isEqualTo(updated.getString("email"));
    assertThat(jsonPath.getBoolean("makePublic")).isEqualTo(updated.getBoolean("makePublic"));
  }

  @Test
  @DisplayName("Authenticate an existing user")
  void authenticate() {
    JsonObject user = basicUser();
    JsonObject request = new JsonObject()
      .put("username", user.getString("username"))
      .put("password", user.getString("password"));

    with()
      .contentType(ContentType.JSON)
      .accept(ContentType.JSON)
      .body(user.encode())
      .post("/register");

    with()
      .contentType(ContentType.JSON)
      .body(request.encode())
      .post("/authenticate")
      .then()
      .assertThat()
      .statusCode(200);
  }

  @Test
  @DisplayName("Failing at authenticating an unknown user")
  void authenticateMissingUser() {
    JsonObject request = new JsonObject()
      .put("username", "Bean")
      .put("password", "abc");

    with()
      .contentType(ContentType.JSON)
      .body(request.encode())
      .post("/authenticate")
      .then()
      .assertThat()
      .statusCode(401);
  }

  @Test
  @DisplayName("Find who owns a device")
  void whoHas() {
    JsonObject user = basicUser();

    with()
      .contentType(ContentType.JSON)
      .accept(ContentType.JSON)
      .body(user.encode())
      .post("/register");

    JsonPath jsonPath = given()
      .accept(ContentType.JSON)
      .when()
      .get("/owns/" + user.getString("deviceId"))
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath();

    assertThat(jsonPath.getString("username")).isEqualTo(user.getString("username"));
    assertThat(jsonPath.getString("deviceId")).isEqualTo(user.getString("deviceId"));
    assertThat(jsonPath.getString("city")).isNull();

    given()
      .accept(ContentType.JSON)
      .when()
      .get("/owns/404")
      .then()
      .assertThat()
      .statusCode(404);
  }
}
