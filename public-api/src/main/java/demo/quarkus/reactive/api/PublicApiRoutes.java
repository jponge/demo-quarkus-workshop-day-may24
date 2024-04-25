package demo.quarkus.reactive.api;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.http.HttpClient;
import io.vertx.mutiny.ext.auth.jwt.JWTAuth;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.RoutingContext;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.mutiny.ext.web.client.predicate.ResponsePredicate;
import io.vertx.mutiny.ext.web.codec.BodyCodec;
import io.vertx.mutiny.ext.web.handler.BodyHandler;
import io.vertx.mutiny.ext.web.handler.JWTAuthHandler;
import io.vertx.mutiny.ext.web.proxy.handler.ProxyHandler;
import io.vertx.mutiny.httpproxy.HttpProxy;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class PublicApiRoutes {

  @Inject
  Logger logger;

  @Inject
  Vertx vertx;

  @Inject
  CryptoHelper cryptoHelper;

  JWTAuth jwtAuth;

  HttpClient httpClient;
  ProxyHandler userProfileProxy;
  ProxyHandler activityServiceProxy;

  WebClient webClient;

  @PostConstruct
  void init() {
    jwtAuth = JWTAuth.create(vertx, new JWTAuthOptions()
      .addPubSecKey(new PubSecKeyOptions()
        .setAlgorithm("RS256")
        .setBuffer(cryptoHelper.publicKey()))
      .addPubSecKey(new PubSecKeyOptions()
        .setAlgorithm("RS256")
        .setBuffer(cryptoHelper.privateKey())));

    httpClient = vertx.createHttpClient();
    userProfileProxy = ProxyHandler.create(HttpProxy.reverseProxy(httpClient).origin(3000, "localhost"));
    activityServiceProxy = ProxyHandler.create(HttpProxy.reverseProxy(httpClient).origin(3001, "localhost"));

    webClient = WebClient.wrap(httpClient);
  }

  public void init(@Observes Router router) {

    // Minimally log incoming requests
    router.route().handler(ctx -> {
      Log.info(ctx.request().method() + " " + ctx.request().path());
      ctx.next();
    });

    // Account
    router.post("/register").handler(userProfileProxy);
    router.post("/token").handler(BodyHandler.create()).handler(this::token);

    JWTAuthHandler jwtHandler = JWTAuthHandler.create(jwtAuth);

    // Profile
    router.get("/:username")
      .handler(jwtHandler)
      .handler(this::checkUser)
      .handler(userProfileProxy);

    router.put("/:username")
      .handler(jwtHandler)
      .handler(this::checkUser)
      .handler(userProfileProxy);

    // Data
    router.get("/:username/total")
      .handler(jwtHandler)
      .handler(this::checkUser)
      .handler(activityServiceProxy);

    router.get("/:username/:year/:month")
      .handler(jwtHandler)
      .handler(this::checkUser)
      .handler(activityServiceProxy);

    router.get("/:username/:year/:month/:day")
      .handler(jwtHandler)
      .handler(this::checkUser)
      .handler(activityServiceProxy);
  }

  private void token(RoutingContext rc) {
    JsonObject payload = rc.body().asJsonObject();
    String username = payload.getString("username");

    webClient.post(3000, "localhost", "/authenticate")
      .expect(ResponsePredicate.SC_SUCCESS)
      .sendJsonObject(payload)
      .replaceWithVoid()
      .onItem().transformToUni(v -> fetchDeviceId(username))
      .onItem().transform(deviceId -> makeJwtToken(username, deviceId))
      .subscribe().with(
        token -> sendToken(rc, token),
        err -> handleAuthError(rc, err)
      );
  }

  private Uni<String> fetchDeviceId(String username) {
    return webClient.get(3000, "localhost", "/" + username)
      .expect(ResponsePredicate.SC_OK)
      .as(BodyCodec.jsonObject())
      .send()
      .onItem().transform(resp -> resp.body().getString("deviceId"));
  }

  private Buffer makeJwtToken(String username, String deviceId) {
    JsonObject claims = new JsonObject()
      .put("deviceId", deviceId);
    JWTOptions jwtOptions = new JWTOptions()
      .setAlgorithm("RS256")
      .setExpiresInMinutes(10_080) // 7 days
      .setIssuer("10k-steps-api")
      .setSubject(username);
    return Buffer.buffer(jwtAuth.generateToken(claims, jwtOptions));
  }

  private void sendToken(RoutingContext rc, Buffer token) {
    rc.response().putHeader("Content-Type", "application/jwt").endAndForget(token);
  }

  private void handleAuthError(RoutingContext rc, Throwable err) {
    logger.error("Authentication error", err);
    rc.fail(401);
  }

  private void checkUser(RoutingContext rc) {
    String subject = rc.user().principal().getString("sub");
    if (!rc.pathParam("username").equals(subject)) {
      rc.response().setStatusCode(403).endAndForget();
    } else {
      rc.next();
    }
  }

  @PreDestroy
  void destroy() {
    httpClient.close().await().indefinitely();
  }
}
