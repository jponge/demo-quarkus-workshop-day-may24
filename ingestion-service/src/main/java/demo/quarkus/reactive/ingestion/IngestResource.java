package demo.quarkus.reactive.ingestion;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import io.smallrye.reactive.messaging.kafka.OutgoingKafkaRecord;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;

import static org.eclipse.microprofile.reactive.messaging.OnOverflow.Strategy.FAIL;

@Path("ingest")
public class IngestResource {

  @Inject
  Logger logger;

  @Inject
  @Channel("steps")
  @OnOverflow(FAIL)
  MutinyEmitter<Payload> stepsEmitter;

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Uni<RestResponse<Void>> httpIngest(@Valid Payload payload) {
    OutgoingKafkaRecord<String, Payload> record = KafkaRecord.of(payload.deviceId, payload);
    return stepsEmitter.sendMessage(record).replaceWith(RestResponse.ok());
  }
}
