package demo.quarkus.reactive.api;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.RoutingContext;
import io.vertx.mutiny.ext.web.handler.BodyHandler;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.vertx.mutiny.core.http.HttpHeaders.CONTENT_TYPE;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@TestMethodOrder(OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Integration tests for the public API")
class PublicApiRoutesTest {

  private static final Map<String, JsonObject> registrations = Map.of(
    "Foo", new JsonObject()
      .put("username", "Foo")
      .put("password", "foo-123")
      .put("email", "foo@email.me")
      .put("city", "Lyon")
      .put("deviceId", "a1b2c3")
      .put("makePublic", true),
    "Bar", new JsonObject()
      .put("username", "Bar")
      .put("password", "bar-#$69")
      .put("email", "bar@email.me")
      .put("city", "Tassin-La-Demi-Lune")
      .put("deviceId", "def1234")
      .put("makePublic", false));

  static Vertx vertx;

  @BeforeAll
  static void setUp() {
    vertx = Vertx.vertx();
    vertx.deployVerticle(new MockUserProfileServer()).await().indefinitely();
    vertx.deployVerticle(new MockActivityServer()).await().indefinitely();
  }

  @AfterAll
  void tearDown() {
    vertx.close().await().indefinitely();
  }

  @Test
  @Order(1)
  @DisplayName("Register some users")
  void registerUsers() {
    registrations.forEach((key, registration) -> {
      given()
        .contentType(ContentType.JSON)
        .body(registration.encode())
        .post("/register")
        .then()
        .assertThat()
        .statusCode(200);
    });
  }

  private final Map<String, String> tokens = new HashMap<>();

  @Test
  @Order(2)
  @DisplayName("Get JWT tokens to access the API")
  void obtainToken() {
    registrations.forEach((key, registration) -> {

      JsonObject login = new JsonObject()
        .put("username", key)
        .put("password", registration.getString("password"));

      String token = given()
        .contentType(ContentType.JSON)
        .body(login.encode())
        .post("/token")
        .then()
        .assertThat()
        .statusCode(200)
        .contentType("application/jwt")
        .extract()
        .asString();

      assertThat(token)
        .isNotNull()
        .isNotBlank();

      tokens.put(key, token);
    });
  }

  @Test
  @Order(3)
  @DisplayName("Fetch a user data")
  void fetchSomeUser() {
    JsonPath jsonPath = given()
      .headers("Authorization", "Bearer " + tokens.get("Foo"))
      .get("/Foo")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath();

    JsonObject foo = registrations.get("Foo");
    List<String> props = asList("username", "email", "city", "deviceId");
    props.forEach(prop -> assertThat(jsonPath.getString(prop)).isEqualTo(foo.getString(prop)));
    assertThat(jsonPath.getBoolean("makePublic")).isEqualTo(foo.getBoolean("makePublic"));
  }

  @Test
  @Order(4)
  @DisplayName("Fail at fetching another user data")
  void failToFatchAnotherUser() {
    given()
      .headers("Authorization", "Bearer " + tokens.get("Foo"))
      .get("/Bar")
      .then()
      .assertThat()
      .statusCode(403);
  }

  @Test
  @Order(5)
  @DisplayName("Update some user data")
  void updateSomeUser() {
    String originalCity = registrations.get("Foo").getString("city");
    boolean originalMakePublic = registrations.get("Foo").getBoolean("makePublic");
    JsonObject updates = new JsonObject()
      .put("city", "Nevers")
      .put("makePublic", false);

    given()
      .headers("Authorization", "Bearer " + tokens.get("Foo"))
      .contentType(ContentType.JSON)
      .body(updates.encode())
      .put("/Foo")
      .then()
      .assertThat()
      .statusCode(200);

    JsonPath jsonPath = given()
      .headers("Authorization", "Bearer " + tokens.get("Foo"))
      .get("/Foo")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath();

    assertThat(jsonPath.getString("city")).isEqualTo(updates.getString("city"));
    assertThat(jsonPath.getBoolean("makePublic")).isEqualTo(updates.getBoolean("makePublic"));

    updates
      .put("city", originalCity)
      .put("makePublic", originalMakePublic);

    given()
      .headers("Authorization", "Bearer " + tokens.get("Foo"))
      .contentType(ContentType.JSON)
      .body(updates.encode())
      .put("/Foo")
      .then()
      .assertThat()
      .statusCode(200);

    jsonPath = given()
      .headers("Authorization", "Bearer " + tokens.get("Foo"))
      .get("/Foo")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath();

    assertThat(jsonPath.getString("city")).isEqualTo(originalCity);
    assertThat(jsonPath.getBoolean("makePublic")).isEqualTo(originalMakePublic);
  }

  @Test
  @Order(6)
  @DisplayName("Check some user stats")
  void checkSomeUserStats() {
    JsonPath jsonPath = given()
      .headers("Authorization", "Bearer " + tokens.get("Foo"))
      .get("/Foo/total")
      .then()
      .assertThat()
      .statusCode(200)
      .contentType(ContentType.JSON)
      .extract()
      .jsonPath();

    assertThat(jsonPath.getInt("count")).isNotNull().isEqualTo(6255);

    jsonPath = given()
      .headers("Authorization", "Bearer " + tokens.get("Foo"))
      .get("/Foo/2019/06")
      .then()
      .assertThat()
      .statusCode(200)
      .contentType(ContentType.JSON)
      .extract()
      .jsonPath();

    assertThat(jsonPath.getInt("count")).isNotNull().isEqualTo(6255);

    jsonPath = given()
      .headers("Authorization", "Bearer " + tokens.get("Foo"))
      .get("/Foo/2019/06/15")
      .then()
      .assertThat()
      .statusCode(200)
      .contentType(ContentType.JSON)
      .extract()
      .jsonPath();

    assertThat(jsonPath.getInt("count")).isNotNull().isEqualTo(5005);
  }

  @Test
  @Order(7)
  @DisplayName("Check that you cannot access somebody else's stats")
  void cannotAccessSomebodyElseStats() {
    given()
      .headers("Authorization", "Bearer " + tokens.get("Foo"))
      .get("/Bar/total")
      .then()
      .assertThat()
      .statusCode(403);
  }

  private static class MockUserProfileServer extends AbstractVerticle {

    private final Map<String, JsonObject> registrations = new HashMap<>();

    @Override
    public Uni<Void> asyncStart() {
      Router router = Router.router(vertx);
      BodyHandler bodyHandler = BodyHandler.create();
      router.post("/register").handler(bodyHandler).handler(this::register);
      router.post("/authenticate").handler(bodyHandler).handler(this::authenticate);
      router.get("/:username").handler(this::getUser);
      router.put("/:username").handler(bodyHandler).handler(this::replaceUser);
      return vertx.createHttpServer()
        .requestHandler(router)
        .listen(3000)
        .replaceWithVoid();
    }

    private void register(RoutingContext rc) {
      JsonObject jsonObject = rc.body().asJsonObject();
      String username = jsonObject.getString("username");
      if (registrations.containsKey(username)) {
        rc.response().setStatusCode(409).endAndForget();
      } else {
        registrations.put(username, jsonObject);
        rc.response().setStatusCode(200).endAndForget();
      }
    }

    private void authenticate(RoutingContext rc) {
      JsonObject jsonObject = rc.body().asJsonObject();
      String username = jsonObject.getString("username");
      JsonObject user = registrations.get(username);
      if (user != null && user.getString("password").equals(jsonObject.getString("password"))) {
        rc.response().setStatusCode(200).endAndForget();
      } else {
        rc.response().setStatusCode(401).endAndForget();
      }
    }

    private void getUser(RoutingContext rc) {
      String username = rc.pathParam("username");
      JsonObject jsonObject = registrations.get(username);
      if (jsonObject == null) {
        rc.response().setStatusCode(204).endAndForget();
      } else {
        rc.response().endAndForget(jsonObject.encode());
      }
    }

    private void replaceUser(RoutingContext rc) {
      String username = rc.pathParam("username");
      JsonObject jsonObject = registrations.get(username);
      if (jsonObject == null) {
        rc.response().setStatusCode(404).endAndForget();
      } else {
        jsonObject.mergeIn(rc.body().asJsonObject());
        rc.response().endAndForget();
      }
    }
  }

  private static class MockActivityServer extends AbstractVerticle {

    @Override
    public Uni<Void> asyncStart() {
      Router router = Router.router(vertx);
      router.get("/:deviceId/total").handler(this::total);
      router.get("/:deviceId/:year/:month").handler(this::stepsOnMonth);
      router.get("/:deviceId/:year/:month/:day").handler(this::stepsOnDay);
      return vertx.createHttpServer()
        .requestHandler(router)
        .listen(3001)
        .replaceWithVoid();
    }

    private void total(RoutingContext rc) {
      JsonObject jsonObject = new JsonObject().put("count", 6255);
      rc.response()
        .putHeader(CONTENT_TYPE, "application/json")
        .endAndForget(jsonObject.encode());
    }

    private void stepsOnMonth(RoutingContext rc) {
      JsonObject jsonObject = new JsonObject().put("count", 6255);
      rc.response()
        .putHeader(CONTENT_TYPE, "application/json")
        .endAndForget(jsonObject.encode());
    }

    private void stepsOnDay(RoutingContext rc) {
      JsonObject jsonObject = new JsonObject().put("count", 5005);
      rc.response()
        .putHeader(CONTENT_TYPE, "application/json")
        .endAndForget(jsonObject.encode());
    }
  }
}
