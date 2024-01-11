package pw.avvero.emk_container;

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
        super(DockerImageName.parse("avvero/emk:latest"));
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