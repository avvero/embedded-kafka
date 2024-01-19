# Native Embedded kafka in Docker Container

<div align="center">
    <img src="assets/image.png" width="400" height="auto">
</div>

Utilizes `org.springframework.kafka.test.EmbeddedKafkaBroker` to have opportunity to provide **native** kafka in docker.

## Usage

Add dependency

```groovy
implementation 'pw.avvero:emk-testcontainers:0.1.0'
```

Create a `EmbeddedKafkaContainer` to use it in your tests:
```java
EmbeddedKafkaContainer kafka = new EmbeddedKafkaContainer("avvero/emk-native:latest"); // OR avvero/emk:latest
```

### Improved Testcontainers Support in Spring Boot 3.1

The new `@ServiceConnection` annotation can be used on the container instance fields of your tests. See 
[example](https://github.com/avvero/embedded-kafka/blob/sb3/example-testcontainers/src/test/java/pw/avvero/emk/KafkaContainerConfiguration.java) in tests.

Refer to [article in Spring blog](https://spring.io/blog/2023/06/23/improved-testcontainers-support-in-spring-boot-3-1) to get more information.

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