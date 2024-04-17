package demo.quarkus.reactive.activity;

import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.impl.PgPoolOptions;

import java.util.Map;

public class PgPoolTestResource implements QuarkusTestResourceLifecycleManager, DevServicesContext.ContextAware {

  PgPool pgPool;

  @Override
  public Map<String, String> start() {
    return Map.of();
  }

  @Override
  public void stop() {
    pgPool.close().await().indefinitely();
  }

  @Override
  public void inject(TestInjector testInjector) {
    testInjector.injectIntoFields(pgPool, new TestInjector.MatchesType(PgPool.class));
  }

  @Override
  public void setIntegrationTestContext(DevServicesContext context) {
    Map<String, String> props = context.devServicesProperties();
    String connectionUri = props.get("quarkus.datasource.reactive.url");
    if (connectionUri.startsWith("vertx-reactive:")) {
      connectionUri = connectionUri.substring("vertx-reactive:".length());
    }
    PgConnectOptions pgConnectOptions = PgConnectOptions.fromUri(connectionUri)
      .setUser(props.get("quarkus.datasource.username"))
      .setPassword(props.get("quarkus.datasource.password"));
    pgPool = PgPool.pool(pgConnectOptions, new PgPoolOptions());
  }
}
