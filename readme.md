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

```java
package test.spock;

import com.github.dockerjava.api.command.InspectContainerResponse;
import lombok.SneakyThrows;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;

import static java.lang.String.format;

/**
 * Utilizes `org.springframework.kafka.test.EmbeddedKafkaBroker` to have opportunity to provide kafka in docker.
 * Works in two steps:
 * - start container with http server
 * - call http method with advertisedListeners configuration to start broker
 */
public class KafkaEmbeddedContainer extends GenericContainer<KafkaEmbeddedContainer> {

    public static final int HTTP_PORT = 8080;
    public static final int KAFKA_PORT = 9093;
    public static final int ZOOKEEPER_PORT = 2181;

    public KafkaEmbeddedContainer() {
        super(DockerImageName.parse("embedded-kafka_emk:latest"));
        addExposedPort(HTTP_PORT);
        addExposedPort(KAFKA_PORT);
        addExposedPort(ZOOKEEPER_PORT);
        withEnv("app.kafka.startup-mode", "on-demand");
        waitingFor(Wait.forLogMessage(".*Started Application in.*\\n", 1));
    }

    @Override
    @SneakyThrows
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        // Start broker on demand  with specified advertised listeners config
        String brokerAdvertisedListener = brokerAdvertisedListener(containerInfo);
        String advertisedListeners = String.join(",", getBootstrapServers(), brokerAdvertisedListener);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest
                .newBuilder(URI.create(format("http://%s:%s/kafka/start", getHost(), getMappedPort(HTTP_PORT))))
                .headers("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(format("{\"advertisedListeners\": \"%s\"}", advertisedListeners)))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException(format("Can't start kafka on demand: http code %s", response.statusCode()));
        }
    }

    public String getBootstrapServers() {
        return format("PLAINTEXT://%s:%s", getHost(), getMappedPort(KAFKA_PORT));
    }

    protected String brokerAdvertisedListener(InspectContainerResponse containerInfo) {
        return format("BROKER://%s:%s", containerInfo.getConfig().getHostName(), "9092");
    }
}
```

## Include Reachability Metadata

0. Setup graalvm: https://www.graalvm.org/latest/docs/getting-started
1. Include Reachability Metadata Using the Native Image Gradle Plugin
2. Run `make run-with-agent`
3. Run activity over broker