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

  @Inject
  PgPool pgPool;

  @Path("/last-24-hours")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Uni<JsonArray> ranking() {
    Log.info("Ranking query");

    return pgPool.preparedQuery(SqlQueries.rankingLast24Hours())
      .execute()
      .onItem().transform(RankingResource::sendRanking);
  }

  private static JsonArray sendRanking(RowSet<Row> rs) {
    JsonArray data = new JsonArray();
    for (Row row : rs) {
      data.add(new JsonObject()
        .put("deviceId", row.getValue("device_id"))
        .put("stepsCount", row.getValue("steps")));
    }
    return data;
  }
}
