package demo.quarkus.reactive;

class SqlQueries {

  static String initDb() {
    // language=postgresql
    return """
      CREATE TABLE IF NOT EXISTS stepevent
      (
          device_id      VARCHAR,
          device_sync    BIGINT,
          sync_timestamp timestamptz,
          steps_count    INTEGER,
          PRIMARY KEY (device_id, device_sync)
      )""";
  }

  static String insertStepEvent() {
    // language=postgresql
    return """
      INSERT INTO stepevent
      VALUES ($1, $2, current_timestamp, $3)""";
  }

  static String stepsCountForToday() {
    // language=postgresql
    return """
      SELECT current_timestamp, coalesce(sum(steps_count), 0)
      FROM stepevent
      WHERE (device_id = $1)
        AND (date_trunc('day', sync_timestamp) = date_trunc('day', current_timestamp))""";
  }

  static String totalStepsCount() {
    // language=postgresql
    return """
      SELECT sum(steps_count)
      FROM stepevent
      WHERE (device_id = $1)""";
  }

  static String monthlyStepsCount() {
    // language=postgresql
    return """
      SELECT sum(steps_count)
      FROM stepevent
      WHERE (device_id = $1)
        AND (date_trunc('month', sync_timestamp) = $2::timestamp)""";
  }

  static String dailyStepsCount() {
    // language=postgresql
    return """
      SELECT sum(steps_count)
      FROM stepevent
      WHERE (device_id = $1)
        AND (date_trunc('day', sync_timestamp) = $2::timestamp)""";
  }

  static String rankingLast24Hours() {
    // language=postgresql
    return """
      SELECT device_id, SUM(steps_count) as steps
      FROM stepevent
      WHERE (now() - sync_timestamp <= (interval '24 hours'))
      GROUP BY device_id
      ORDER BY steps DESC""";
  }

  private SqlQueries() {
    // Constants
  }
}
