#!/usr/bin/env just --justfile

# Use `just mode=run (...)` for quarkus:run instead of quarkus:dev
mode := "dev"

_run module:
  mvn quarkus:{{mode}} -f {{module}}

activity-service: (_run "activity-service")
congrats-service: (_run "congrats-service")
event-stats-service: (_run "event-stats-service")
ingestion-service: (_run "ingestion-service")
public-api: (_run "public-api")
user-profile-service: (_run "user-profile-service")
user-webapp: (_run "user-webapp")
dashboard-webapp: (_run "dashboard-webapp")

activity-generator:
  cd activity-generator ; mvn quarkus:{{mode}}

rebuild:
  mvn clean install

package:
  mvn clean install -DskipTests -T4

native-build:
  mvn clean install -DskipTests -Pnative
  just copy-native

copy-native:
  find . -type f -name \*-runner | xargs -I{} cp {} native-apps

clean-native:
  find native-apps -type f -name \*-runner | xargs rm

create-demo-pod:
  podman pod create --name quarkus-demo-pod -p 5432:5432 -p 9092:9092
  podman run --pod quarkus-demo-pod -dt --name postgres -e POSTGRES_PASSWORD=1234 -e POSTGRES_USER=quarkus docker.io/library/postgres:14
  podman run --pod quarkus-demo-pod -dt --name kafka -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 quay.io/ogunalp/kafka-native:latest

delete-demo-pod:
  podman pod stop quarkus-demo-pod
  podman pod rm quarkus-demo-pod

run-all-native:
  cd native-apps && hivemind

run-native-generator:
  cd native-apps && ./activity-generator-1.0-runner
