package demo.quarkus.reactive.api;

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
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
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
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class PublicApiRoutes {

  @Inject
  Logger logger;

  // Inject Vert.x instance managed by Quarkus
  @Inject
  Vertx vertx;

  @Inject
  CryptoHelper cryptoHelper;

  @ConfigProperty(name = "backend.user-service.host")
  String userServiceHost;

  @ConfigProperty(name = "backend.user-service.port")
  int userServicePort;

  @ConfigProperty(name = "backend.activity-service.host")
  String activityServiceHost;

  @ConfigProperty(name = "backend.activity-service.port")
  int activityServicePort;

  JWTAuth jwtAuth;
  HttpClient httpClient;
  ProxyHandler userProfileProxy;
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

    // Create an HTTP client
    httpClient = vertx.createHttpClient();
    // Create a WebClient wrapping the HTTP Client
    webClient = WebClient.wrap(httpClient);
    userProfileProxy = ProxyHandler.create(HttpProxy.reverseProxy(httpClient).origin(userServicePort, userServiceHost));
  }

  public void init(@Observes Router router) {

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
      .handler(this::fetchTotalSteps);

    router.get("/:username/:year/:month")
      .handler(jwtHandler)
      .handler(this::checkUser)
      .handler(this::fetchMonthSteps);

    router.get("/:username/:year/:month/:day")
      .handler(jwtHandler)
      .handler(this::checkUser)
      .handler(this::fetchDaySteps);
  }

  private void fetchTotalSteps(RoutingContext rc) {
    String username = rc.pathParam("username");
    fetchDeviceId(username)
      .onItem().transformToUni(deviceId -> fetchSteps(deviceId, "/device/" + deviceId + "/total"))
      .subscribe().with(
        res -> forwardJsonPayload(rc, res),
        err -> handleError(rc, err)
      );
  }

  private void fetchMonthSteps(RoutingContext rc) {
    String username = rc.pathParam("username");
    String year = rc.pathParam("year");
    String month = rc.pathParam("month");
    fetchDeviceId(username)
      .onItem().transformToUni(deviceId -> fetchSteps(deviceId, "/device/" + deviceId + "/" + year + "/" + month))
      .subscribe().with(
        res -> forwardJsonPayload(rc, res),
        err -> handleError(rc, err)
      );
  }

  private void fetchDaySteps(RoutingContext rc) {
    String username = rc.pathParam("username");
    String year = rc.pathParam("year");
    String month = rc.pathParam("month");
    String day = rc.pathParam("day");
    fetchDeviceId(username)
      .onItem().transformToUni(deviceId -> fetchSteps(deviceId, "/device/" + deviceId + "/" + year + "/" + month + "/" + day))
      .subscribe().with(
        res -> forwardJsonPayload(rc, res),
        err -> handleError(rc, err)
      );
  }

  private static void forwardJsonPayload(RoutingContext rc, JsonObject res) {
    rc.response()
      .putHeader("Content-Type", "application/json")
      .endAndForget(res.encode());
  }

  private Uni<JsonObject> fetchSteps(String deviceId, String path) {
    return webClient.get(activityServicePort, activityServiceHost, path)
      .expect(ResponsePredicate.SC_OK)
      .as(BodyCodec.jsonObject())
      .send()
      .onItem().transform(HttpResponse::body);
  }

  private void token(RoutingContext rc) {
    JsonObject payload = rc.body().asJsonObject();
    String username = payload.getString("username");

    // POST request to the user profile service
    webClient.post(userServicePort, userServiceHost, "/authenticate")
      // Expect success (non-successful codes will be reported as exceptions)
      .expect(ResponsePredicate.SC_SUCCESS)
      // JsonObject payload (sets the content-type header)
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
    HttpRequest<Buffer> get = webClient.get(userServicePort, userServiceHost, "/" + username)
      .expect(ResponsePredicate.SC_OK);
    // Automatic decoding to JsonObject
    HttpRequest<JsonObject> getAsJson = get.as(BodyCodec.jsonObject());
    return getAsJson
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

  private void handleError(RoutingContext rc, Throwable err) {
    logger.error("Error", err);
    rc.fail(500);
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
