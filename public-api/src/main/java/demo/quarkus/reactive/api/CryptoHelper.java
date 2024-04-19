package demo.quarkus.reactive.api;

import io.vertx.mutiny.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class CryptoHelper {

  @Inject
  Vertx vertx;

  public String publicKey() {
    return read("public_key.pem");
  }

  public String privateKey() {
    return read("private_key.pem");
  }

  private String read(String file) {
    return vertx.fileSystem().readFileAndAwait(file).toString(StandardCharsets.UTF_8);
  }
}
