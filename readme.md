# Embedded kafka in Docker Container

Utilizes `org.springframework.kafka.test.EmbeddedKafkaBroker` to have opportunity to provide kafka in docker.

> Disclaimer: a little bit dirty, but works, think about it as POC

## Docker Image 

Ready to use Docker image is hosted on Docker Hub and can be pulled using the following command:

```bash
docker pull avvero/emk
```

or native image for linux/arm64
```bash
docker pull avvero/emk-native
```

## Build container with java

1. Build project: `make docker-build`

## Build container with native

0. Setup graalvm: https://www.graalvm.org/latest/docs/getting-started
1. Build project: `docker-build-native`

## Using

There are two modes: at-once, on-demand (default)

### Mode at-once 

Starts broker on container start.

It's possible to provide `advertised.listeners` over configuration, use property `app.kafka.advertised.listeners`, like
```app.kafka.advertised.listeners=PLAINTEXT://localhost:9093,BROKER://localhost:9092```.

> **_Please take into account:_**  that we are forced to fix exposed ports (`#addFixedExposedPort`) because kafka start before
container gets host and mapped port, check `org.testcontainers.containers.KafkaContainer#brokerAdvertisedListener` to
get more details.
>
> It means that port would **fixed** for the host.
> 
> Nice article with explanation why do may you need is here - https://www.confluent.io/blog/kafka-listeners-explained/

### Mode on-demand

Does not start broker on container start. To start broker it's required to call http method /kafka/start and provide
advertised listeners:
```http
POST http://localhost:8080/kafka/start HTTP/1.1
Content-Type: application/json

{
  "advertisedListeners": "PLAINTEXT://localhost:9093,BROKER://localhost:9092"
}
```

To enable please provide property for container
```properties
app.kafka.startup-mode=on-demand
```

## Example of `GenericContainer` implementation

Please see module `emk-application-testcontainer` to get insight.

## Include Reachability Metadata

0. Setup graalvm: https://www.graalvm.org/latest/docs/getting-started
1. Include Reachability Metadata Using the Native Image Gradle Plugin
2. Run `make run-with-agent`
3. Run activity over broker