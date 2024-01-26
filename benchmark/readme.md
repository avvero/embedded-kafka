# TestContainers Benchmark

## Start and Ready Benchmarks

The TestContainersBenchmark class in the pw.avvero.emk package provides JMH benchmarks for evaluating the startup time 
and operational readiness of Kafka containers. This benchmarking aims to compare the performance and efficiency of 
different Kafka container configurations.

Benchmarks to measure time taken to start and stop:
- `testContainersKafkaStart`: Kafka container provided by `confluentinc/cp-kafka:7.3.3`.
- `emkJvmKafkaStart`: custom Kafka container `avvero/emk:1.0.0`.
- `emkNativeKafkaStart`: custom Kafka container `avvero/emk-native:1.0.0`, optimized for native execution.

Benchmarks for Startup and Readiness (accordingly):
- `testContainersKafkaStartAndReady`
- `emkJvmKafkaStartAndReady`
- `emkNativeKafkaStartAndReady`

Each benchmark involves starting the Kafka container, checking its operational readiness by listing Kafka topics using 
the AdminClient, and then stopping the container.

Why Focus on Start and Ready Benchmarks? Just starting a Kafka container doesn't necessarily mean it's ready for 
operations. The readiness check simulates a real-world scenario where Kafka is not only started but also fully operational.

## Results
```
Benchmark                                                 Mode  Cnt  Score   Error  Units
TestContainersBenchmark.testContainersKafkaStart            ss   10  3,036 ± 0,152   s/op
TestContainersBenchmark.emkJvmKafkaStart                    ss   10  0,282 ± 0,029   s/op
TestContainersBenchmark.emkNativeKafkaStart                 ss   10  0,276 ± 0,014   s/op

TestContainersBenchmark.testContainersKafkaStartAndReady    ss   10  3,091 ± 0,354   s/op
TestContainersBenchmark.emkJvmKafkaStartAndReady            ss   10  2,659 ± 0,708   s/op
TestContainersBenchmark.emkNativeKafkaStartAndReady         ss   10  0,521 ± 0,055   s/op
```

## Memory analyzes

See files with gc logs:
- [cp-kafka-gc.log](cp-kafka-gc.log)
- [emk-jvm-gc.log](emk-jvm-gc.log)
- [emk-native.gc.log](emk-native.gc.log)

## JMX confluentinc/cp-kafka

https://docs.confluent.io/platform/current/installation/docker/config-reference.html#confluent-ak-configuration

```bash
docker run -d \
--name=kafka-jmx \
-h kafka-jmx \
-p 9101:9101 \
-p 9092:9092 \
-e KAFKA_AUTO_CREATE_TOPICS_ENABLE=true \
-e KAFKA_NODE_ID=1 \
-e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP='CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT' \
-e KAFKA_ADVERTISED_LISTENERS='PLAINTEXT://kafka-jmx:29092,PLAINTEXT_HOST://localhost:9092' \
-e KAFKA_JMX_PORT=9101 \
-e KAFKA_JMX_HOSTNAME=localhost \
-e KAFKA_PROCESS_ROLES='broker,controller' \
-e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
-e KAFKA_CONTROLLER_QUORUM_VOTERS='1@kafka-jmx:29093' \
-e KAFKA_LISTENERS='PLAINTEXT://kafka-jmx:29092,CONTROLLER://kafka-jmx:29093,PLAINTEXT_HOST://0.0.0.0:9092' \
-e KAFKA_INTER_BROKER_LISTENER_NAME='PLAINTEXT' \
-e KAFKA_CONTROLLER_LISTENER_NAMES='CONTROLLER' \
-e CLUSTER_ID='MkU3OEVBNTcwNTJENDM2Qk' \
confluentinc/cp-kafka:7.5.3
```

## GC EMK

```groovy
bootRun {
    jvmArgs = [
            '-Xloggc:gc.log',
            '-XX:+PrintGCDetails',
    ]
}
```

## GC EMK Native

```bash
./emk-application/build/native/nativeCompile/emk-application -XX:+PrintGC -XX:+VerboseGC
```
