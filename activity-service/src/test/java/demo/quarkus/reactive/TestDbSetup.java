package demo.quarkus.reactive;

import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Tuple;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

class TestDbSetup {

  static void cleanDb(PgPool pgPool) {
    pgPool.query("TRUNCATE TABLE stepevent").execute()
      .await()
      .indefinitely();
  }

  static void insertTestData(PgPool pgPool) {
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

    pgPool.preparedQuery("INSERT INTO stepevent VALUES($1, $2, $3::timestamp, $4)").executeBatch(data)
      .await()
      .indefinitely();
  }

  private TestDbSetup() {
    // Utility
  }
}
