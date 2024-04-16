package demo.quarkus.reactive;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@QuarkusTestResource(PgPoolTestResource.class)
class DeviceResourceTest {

  PgPool pgPool;

  @BeforeEach
  void prepareDb() {
    String insertQuery = "INSERT INTO stepevent VALUES($1, $2, $3::timestamp, $4)";
    LocalDateTime now = LocalDateTime.now();
    List<Tuple> data = Arrays.asList(
      Tuple.of("123", 1, LocalDateTime.of(2023, 4, 1, 23, 0), 6541),
      Tuple.of("123", 2, LocalDateTime.of(2023, 5, 20, 10, 0), 200),
      Tuple.of("123", 3, LocalDateTime.of(2023, 5, 21, 10, 10), 100),
      Tuple.of("456", 1, LocalDateTime.of(2023, 5, 21, 10, 15), 123),
      Tuple.of("123", 4, LocalDateTime.of(2023, 5, 21, 11, 0), 320),
      Tuple.of("abc", 1, now.minusHours(1), 1000),
      Tuple.of("def", 1, now.minusHours(2), 100),
      Tuple.of("def", 2, now.minusMinutes(30), 900),
      Tuple.of("abc", 2, now, 1500)
    );

    pgPool.query("TRUNCATE TABLE stepevent").execute()
      .chain(() -> pgPool.preparedQuery(insertQuery).executeBatch(data))
      .await()
      .indefinitely();
  }

  @Test
  @DisplayName("Operate a few successful steps count queries over the dataset")
  void stepsCountQueries() {
    JsonPath jsonPath = given()
      .accept(ContentType.JSON)
      .get("/device/456/total")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath();

    assertThat(jsonPath.getInt("count")).isEqualTo(123);

    jsonPath = given()
      .accept(ContentType.JSON)
      .get("/device/123/total")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath();

    assertThat(jsonPath.getInt("count")).isEqualTo(7161);

    jsonPath = given()
      .accept(ContentType.JSON)
      .get("/device/123/2023/04")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath();

    assertThat(jsonPath.getInt("count")).isEqualTo(6541);

    jsonPath = given()
      .accept(ContentType.JSON)
      .get("/device/123/2023/05")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath();

    assertThat(jsonPath.getInt("count")).isEqualTo(620);

    jsonPath = given()
      .accept(ContentType.JSON)
      .get("/device/123/2023/05/20")
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
      .get("/device/123/2023/05/18")
      .then()
      .assertThat()
      .statusCode(404);

    given()
      .accept(ContentType.JSON)
      .get("/device/123/2023/03")
      .then()
      .assertThat()
      .statusCode(404);

    given()
      .accept(ContentType.JSON)
      .get("/device/122/total")
      .then()
      .assertThat()
      .statusCode(404);
  }

  @Test
  @DisplayName("Check for bad requests (HTTP 404)")
  void check400() {
    given()
      .accept(ContentType.JSON)
      .get("/device/123/2023/15/68")
      .then()
      .assertThat()
      .statusCode(400);

    given()
      .accept(ContentType.JSON)
      .get("/device/123/210/15")
      .then()
      .assertThat()
      .statusCode(400);
  }

  @Test
  @DisplayName("Fetch the ranking over the last 24 hours")
  void checkRanking24Hours() {
    JsonPath jsonPath = given()
      .accept(ContentType.JSON)
      .get("/ranking/last-24-hours")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath();
    List<HashMap<String, Object>> data = jsonPath.getList("$");
    assertThat(data.size()).isEqualTo(2);
    assertThat(data.get(0))
      .containsEntry("deviceId", "abc")
      .containsEntry("stepsCount", 2500);
    assertThat(data.get(1))
      .containsEntry("deviceId", "def")
      .containsEntry("stepsCount", 1000);
  }
}
