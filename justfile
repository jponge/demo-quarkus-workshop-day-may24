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
