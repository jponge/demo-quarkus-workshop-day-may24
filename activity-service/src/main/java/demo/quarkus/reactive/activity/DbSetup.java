package demo.quarkus.reactive.activity;

import io.quarkus.runtime.StartupEvent;
import io.vertx.mutiny.pgclient.PgPool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class DbSetup {

  @Inject
  PgPool client;

  void config(@Observes StartupEvent ev) {
    initDb();
  }

  private void initDb() {
    client.query(SqlQueries.initDb()).execute()
      .await().indefinitely();
  }
}
