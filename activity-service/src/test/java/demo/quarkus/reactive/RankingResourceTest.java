package demo.quarkus.reactive;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.vertx.mutiny.pgclient.PgPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@TestHTTPEndpoint(RankingResource.class)
@QuarkusTestResource(PgPoolTestResource.class)
class RankingResourceTest {

  PgPool pgPool;

  @BeforeEach
  void prepareDb() {
    TestDbSetup.execute(pgPool);
  }

  @Test
  @DisplayName("Fetch the ranking over the last 24 hours")
  void checkRanking24Hours() {
    JsonPath jsonPath = given()
      .accept(ContentType.JSON)
      .get("/last-24-hours")
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
