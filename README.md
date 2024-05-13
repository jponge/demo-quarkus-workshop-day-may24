# 10k steps challenge application / Reactive Quarkus + Vert.x edition

[![CI with Maven](https://github.com/jponge/demo-quarkus-workshop-day-may24/actions/workflows/maven.yml/badge.svg)](https://github.com/jponge/demo-quarkus-workshop-day-may24/actions/workflows/maven.yml)

## Overview

This is a port of the [Vert.x in Action 10k steps challenge application](https://github.com/jponge/vertx-in-action/tree/master/part2-steps-challenge) to Quarkus.

Some code parts have been ported to Quarkus APIs (e.g., the Kafka code uses Quarkus Messaging APIs rather than the Vert.x Kafka client) while other parts take advantage of Vert.x clients and APIs running in Quarkus.

This project is not just a good showcase of reactive Quarkus: it also shows how to take advantage of the Vert.x ecosystem in Quarkus.

## How to build and run it

You will need:

- We recommend Java 21+
- Native compilation works with a GraalVM distribution such as [Mandrel](https://github.com/graalvm/mandrel)
- You will need to run containers: [Podman Desktop](https://podman-desktop.io/) is your friend

We also recommend:

- [just](https://github.com/casey/just) to simplify command line to build and run
- [hivemind](https://github.com/DarthSim/hivemind) to easily run all processes at once

You can run each individual service in Quarkus "dev" mode.
Pick the directory name, and use `just` as in:

```
just congrats-service
```

There is a simple activity generator that you can run with:

```
just run-native-generator
```

Edit the `activity-generator/generator.json` file to customize the workload.

You can compile all services to native executables in `native-apps/`:

```
just native-build
```

Note: this will take _several_ minutes.

Before running the services you will need a PostgreSQL database and Apache Kafka.
Podman can create a _Pod_ for you:

```
just create-demo-pod
```

and you can clean this _Pod_ and its associated containers with:

```
just delete-demo-pod
```

Running each service individually can be tedious and will require many tabs.
You can instead use `hivemind` to start then all at once:

```
just run-all-native
```

## Licensing

```
Copyright 2024 Red Hat, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
