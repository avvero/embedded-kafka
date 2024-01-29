# Native Embedded kafka in Docker Container

<div align="center">
    <img src="assets/image.png" width="400" height="auto">
</div>

Utilizes `org.springframework.kafka.test.EmbeddedKafkaBroker` and GraalVM to provide **native** kafka in docker.

## Article

In the article, I want to share my experience with creating a native image for EmbeddedKafka using GraalVM. Utilizing 
this image in integration tests not only speeds up the execution of test scenarios but also reduces memory consumption.
Interestingly, when compared to using confluentinc/cp-kafka in Testcontainers, there is a noticeable difference in both 
speed and memory usage — and it’s not in favor of the latter.

- [Как сократить потребление памяти в интеграционных тестах с Kafka с помощью GraalVM](https://habr.com/ru/articles/788812/)
- [How to Reduce Memory Consumption in Integration Tests with Kafka Using GraalVM](https://medium.com/@avvero.abernathy/how-to-reduce-memory-consumption-in-integration-tests-with-kafka-using-graalvm-de2393f7fe8a)

## Usage

Add dependency

```groovy
implementation 'pw.avvero:emk-testcontainers:1.0.0'
```

Create a `EmbeddedKafkaContainer` to use it in your tests:
```java
EmbeddedKafkaContainer kafka = new EmbeddedKafkaContainer("avvero/emk-native:1.0.0"); // OR avvero/emk:latest
```

### Improved Testcontainers Support in Spring Boot 3.1

The new `@ServiceConnection` annotation can be used on the container instance fields of your tests. See 
[example](https://github.com/avvero/embedded-kafka/blob/sb3/example-testcontainers/src/test/java/pw/avvero/emk/KafkaContainerConfiguration.java) in tests.

Refer to [article in Spring blog](https://spring.io/blog/2023/06/23/improved-testcontainers-support-in-spring-boot-3-1) to get more information.

---

## Examples for using Kafka in integration tests

One can find examples for using Kafka in integration tests:
- EmbeddedKafka - module `example-spring-embedded-kafka`
- Testcontainers - module `example-testcontainers`
- Embedded Kafka Native - module `example-embedded-kafka-container`

---

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

Run command: `make emk-docker-build`

## Build container with native

Run command: `emk-docker-build-native`

## Native build details 

### Include Reachability Metadata

0. Setup graalvm: https://www.graalvm.org/latest/docs/getting-started
1. Include Reachability Metadata Using the Native Image Gradle Plugin
2. Run `make emk-run-with-agent`
3. Run activity over broker