# Embedded kafka in Docker Container

Utilizes `org.springframework.kafka.test.EmbeddedKafkaBroker` to have opportunity to provide kafka in docker.

> Disclaimer: a little bit dirty, but works, think about it as POC

## How to use

1 Build project: `./gradlewe installBootDist`
2 Build docker image: `docker build -t embedded-kafka_emk .`
3 Use

## Configuration 

It's possible to provide `advertised.listeners` over configuration, use property `app.kafka.advertised.listeners`, like
```app.kafka.advertised.listeners=PLAINTEXT://localhost:9093,BROKER://localhost:9092```.

Nice article with explanation why do may you need is here - https://www.confluent.io/blog/kafka-listeners-explained/

## Example of `GenericContainer` implementation
```java
package com.fxclub.test.spock;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * This container wraps Confluent Kafka and Zookeeper (optionally)
 */
public class KafkaEmbeddedContainer extends GenericContainer<KafkaEmbeddedContainer> {

    public static final int KAFKA_PORT = 9093;
    public static final int ZOOKEEPER_PORT = 2181;

    public KafkaEmbeddedContainer() {
        super(DockerImageName.parse("embedded-kafka_emk:latest"));
        addFixedExposedPort(KAFKA_PORT, KAFKA_PORT);
        addFixedExposedPort(ZOOKEEPER_PORT, ZOOKEEPER_PORT);
        String host = getNetwork() != null ? getNetworkAliases().get(0) : "localhost";
        withEnv("app.kafka.advertised.listeners", "PLAINTEXT://" + host + ":" + KAFKA_PORT + ",BROKER://localhost:9092");
    }

    public String getBootstrapServers() {
        return String.format("PLAINTEXT://%s:%s", getHost(), getMappedPort(KAFKA_PORT));
    }
}
```

Please take into account that we are forced to fix exposed ports (`#addFixedExposedPort`) because kafka start before 
container gets host and mapped port, check `org.testcontainers.containers.KafkaContainer#brokerAdvertisedListener` to 
get more details.