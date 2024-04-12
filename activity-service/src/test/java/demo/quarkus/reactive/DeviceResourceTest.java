package demo.quarkus.reactive;

import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class DeviceResourceTest {
  @Test
  void testHelloEndpoint() {
    given()
      .when().get("/device/uuid-string/total")
      .then()
      .statusCode(200)
      .body(is(new JsonObject().toString()));
  }

}
