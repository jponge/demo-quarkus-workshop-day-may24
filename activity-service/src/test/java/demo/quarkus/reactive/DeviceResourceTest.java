package demo.quarkus.reactive;

import io.quarkus.test.junit.QuarkusTest;
import io.vertx.mutiny.pgclient.PgPool;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
class DeviceResourceTest {

  @Inject
  PgPool pgPool;

  @BeforeEach
  void setUp() {
    // FIXME - setup db
  }

  @Test
  void testHelloEndpoint() {
    given()
      .when().get("/device/uuid-string/total")
      .then()
      .statusCode(404);
  }
}
