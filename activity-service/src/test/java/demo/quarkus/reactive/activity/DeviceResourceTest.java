package demo.quarkus.reactive.activity;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.vertx.mutiny.pgclient.PgPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@TestHTTPEndpoint(DeviceResource.class)
@QuarkusTestResource(PgPoolTestResource.class)
class DeviceResourceTest {

  PgPool pgPool;

  @BeforeEach
  void prepareDb() {
    TestDbSetup.cleanDb(pgPool);
    TestDbSetup.insertTestData(pgPool);
  }

  @Test
  @DisplayName("Operate a few successful steps count queries over the dataset")
  void stepsCountQueries() {
    JsonPath jsonPath = given()
      .accept(ContentType.JSON)
      .get("/456/total")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath();

    assertThat(jsonPath.getInt("count")).isEqualTo(123);

    jsonPath = given()
      .accept(ContentType.JSON)
      .get("/123/total")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath();

    assertThat(jsonPath.getInt("count")).isEqualTo(7161);

    jsonPath = given()
      .accept(ContentType.JSON)
      .get("/123/2023/04")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath();

    assertThat(jsonPath.getInt("count")).isEqualTo(6541);

    jsonPath = given()
      .accept(ContentType.JSON)
      .get("/123/2023/05")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath();

    assertThat(jsonPath.getInt("count")).isEqualTo(620);

    jsonPath = given()
      .accept(ContentType.JSON)
      .get("/123/2023/05/20")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath();

    assertThat(jsonPath.getInt("count")).isEqualTo(200);
  }

  @Test
  @DisplayName("Check for HTTP 404 when there is no activity in the dataset")
  void check404() {
    given()
      .accept(ContentType.JSON)
      .get("/123/2023/05/18")
      .then()
      .assertThat()
      .statusCode(404);

    given()
      .accept(ContentType.JSON)
      .get("/123/2023/03")
      .then()
      .assertThat()
      .statusCode(404);

    given()
      .accept(ContentType.JSON)
      .get("/122/total")
      .then()
      .assertThat()
      .statusCode(404);
  }

  @Test
  @DisplayName("Check for bad requests (HTTP 404)")
  void check400() {
    given()
      .accept(ContentType.JSON)
      .get("/123/2023/15/68")
      .then()
      .assertThat()
      .statusCode(400);

    given()
      .accept(ContentType.JSON)
      .get("/123/210/15")
      .then()
      .assertThat()
      .statusCode(400);
  }
}
