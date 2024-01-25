# TestContainers Benchmark
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