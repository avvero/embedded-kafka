package pw.avvero.emk;

import org.apache.kafka.clients.admin.AdminClient;
import org.openjdk.jmh.annotations.Benchmark;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.ExecutionException;

import static java.util.Collections.singletonMap;
import static org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG;

public class TestContainersBenchmark {

    @Benchmark
    public void testContainersKafka() throws ExecutionException, InterruptedException {
        KafkaContainer container = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.3.3"));
        container.start();
        checkKafkaReadiness(container.getBootstrapServers());
        container.stop();
    }

    @Benchmark
    public void emkJvmKafka() throws ExecutionException, InterruptedException {
        EmbeddedKafkaContainer container = new EmbeddedKafkaContainer("avvero/emk:1.0.0");
        container.start();
        checkKafkaReadiness(container.getBootstrapServers());
        container.stop();
    }
    @Benchmark
    public void emkNativeKafka() throws ExecutionException, InterruptedException {
        EmbeddedKafkaContainer container = new EmbeddedKafkaContainer("avvero/emk-native:1.0.0");
        container.start();
        checkKafkaReadiness(container.getBootstrapServers());
        container.stop();
    }

    /**
     * Checks if Kafka is ready for operations by attempting to list topics.
     * This method is designed to ensure that Kafka has successfully started and is operational.
     * It uses the Kafka AdminClient to list the names of the topics, which is a basic operation
     * that can indicate if Kafka is responding correctly.
     *
     * The motivation behind this check is to handle scenarios where the Kafka container might be
     * running but Kafka itself is not fully operational (e.g., service inside the container hasn't
     * fully started). This method helps in determining the operational status of Kafka.
     *
     * @param bootstrapServers the Kafka bootstrap servers string.
     * @throws ExecutionException if there is an issue executing the topic listing.
     * @throws InterruptedException if the thread is interrupted while waiting for Kafka response.
     */
    private void checkKafkaReadiness(String bootstrapServers) throws ExecutionException, InterruptedException {
        AdminClient admin = AdminClient.create(singletonMap(BOOTSTRAP_SERVERS_CONFIG, bootstrapServers));
        admin.listTopics().names().get();
        admin.close();
    }
}
