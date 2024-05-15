package demo.quarkus.reactive.activity;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/ranking")
public class RankingResource {

  // Inject PgPool managed by the reactive-pg-client extension
  // Configured in application properties (and other sources)
  @Inject
  PgPool pgPool;

  @Path("/last-24-hours")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Uni<JsonArray> ranking() {
    Log.info("Ranking query");

    // No ceremony: execute the query on any free connection in the pool
    // Prepared queries can be cached for performance
    return pgPool.preparedQuery(SqlQueries.rankingLast24Hours())
      .execute()
      .onItem().transform(RankingResource::sendRanking);
  }

  // RowSet contains all the data returned by the DB
  private static JsonArray sendRanking(RowSet<Row> rs) {
    JsonArray data = new JsonArray();
    // Iterate over the RowSet to build the response
    for (Row row : rs) {
      data.add(new JsonObject()
        .put("deviceId", row.getValue("device_id"))
        .put("stepsCount", row.getValue("steps")));
    }
    return data;
  }
}
