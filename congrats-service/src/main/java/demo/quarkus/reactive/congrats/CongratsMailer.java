package demo.quarkus.reactive.congrats;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.mutiny.ext.web.client.predicate.ResponsePredicate;
import io.vertx.mutiny.ext.web.codec.BodyCodec;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import static java.time.temporal.ChronoUnit.SECONDS;

@ApplicationScoped
public class CongratsMailer {

  @Inject
  Vertx vertx;

  @Inject
  ReactiveMailer reactiveMailer;

  private WebClient webClient;

  @PostConstruct
  void init() {
    webClient = WebClient.create(vertx);
  }

  @Incoming("updates")
  @Retry(delay = 10, delayUnit = SECONDS)
  public Uni<Void> processEvent(JsonObject payload) {
    if (below10k(payload)) {
      return Uni.createFrom().voidItem();
    }
    String deviceId = payload.getString("deviceId");
    return fetchOwner(deviceId)
      .onItem().transformToUni(username -> fetchEmailAddress(username))
      .onItem().transformToUni(recipient -> sendEmail(recipient, payload));
  }

  private static boolean below10k(JsonObject payload) {
    return payload.getInteger("stepsCount") < 10_000;
  }

  private Uni<String> fetchOwner(String deviceId) {
    return webClient.get(3000, "localhost", "/owns/" + deviceId)
      .expect(ResponsePredicate.SC_OK)
      .expect(ResponsePredicate.JSON)
      .as(BodyCodec.jsonObject())
      .send()
      .onItem().transform(resp -> resp.body().getString("username"));
  }

  private Uni<String> fetchEmailAddress(String username) {
    return webClient.get(3000, "localhost", "/" + username)
      .expect(ResponsePredicate.SC_OK)
      .expect(ResponsePredicate.JSON)
      .as(BodyCodec.jsonObject())
      .send()
      .onItem().transform(resp -> resp.body().getString("email"));
  }

  private Uni<Void> sendEmail(String recipient, JsonObject payload) {
    Integer stepsCount = payload.getInteger("stepsCount");
    Mail mail = new Mail()
      .setFrom("noreply@tenksteps.tld")
      .addTo(recipient)
      .setSubject("You made it!")
      .setText("Congratulations on reaching " + stepsCount + " steps today!\n\n- The 10k Steps Team\n");
    return reactiveMailer.send(mail);
  }

  @PreDestroy
  void destroy() {
    webClient.close();
  }
}
