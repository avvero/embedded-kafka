# TestContainers Benchmark
The TestContainersBenchmark class in the pw.avvero.emk package provides JMH benchmarks for evaluating the startup time 
and operational readiness of Kafka containers. This benchmarking aims to compare the performance and efficiency of 
different Kafka container configurations.

Benchmarks
- `testContainersKafka`: Tests the startup time and readiness of the standard Kafka container provided by `confluentinc/cp-kafka:7.3.3`.
- `emkJvmKafka`: custom Kafka container (`avvero/emk:1.0.0`)
- `emkNativeKafka`: custom Kafka container (`avvero/emk-native:1.0.0`), optimized for native execution.

Each benchmark involves starting the Kafka container, checking its operational readiness by listing Kafka topics using 
the AdminClient, and then stopping the container.

## Results

Benchmark                                    Mode  Cnt  Score   Error  Units
TestContainersBenchmark.emkJvmKafka            ss   10  2,751 ± 0,657   s/op
TestContainersBenchmark.emkNativeKafka         ss   10  0,592 ± 0,068   s/op
TestContainersBenchmark.testContainersKafka    ss   10  2,994 ± 0,162   s/op