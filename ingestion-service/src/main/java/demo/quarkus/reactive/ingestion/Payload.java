package demo.quarkus.reactive.ingestion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class Payload {

  @NotBlank
  public String deviceId;

  @NotNull
  public Long deviceSync;

  @NotNull
  @PositiveOrZero
  public Integer stepsCount;

  @Override
  public String toString() {
    return "Payload{" +
           "deviceId='" + deviceId + '\'' +
           ", deviceSync=" + deviceSync +
           ", stepsCount=" + stepsCount +
           '}';
  }
}
