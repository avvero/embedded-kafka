package pw.avvero.emk;

import com.github.dockerjava.api.command.InspectContainerResponse;
import lombok.SneakyThrows;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

/**
 * Utilizes `org.springframework.kafka.test.EmbeddedKafkaBroker` to have opportunity to provide kafka in docker.
 * Works in two steps:
 * - start container with http server
 * - call http method with advertisedListeners configuration to start broker
 */
public class EmbeddedKafkaContainer extends GenericContainer<EmbeddedKafkaContainer> {

    public static final int HTTP_PORT = 8080;
    public static final int KAFKA_PORT = 9093;
    public static final int ZOOKEEPER_PORT = 2181;
    private static final String STARTER_SCRIPT = "/testcontainers_start.sh";

    private final String imageName;

    public EmbeddedKafkaContainer(String imageName) {
        super(DockerImageName.parse(imageName));
        this.imageName = imageName;
        addExposedPort(HTTP_PORT);
        addExposedPort(KAFKA_PORT);
        addExposedPort(ZOOKEEPER_PORT);
        setCommand("/bin/sh", "-c", "while [ ! -f " + STARTER_SCRIPT + " ]; do echo 'Waiting for start kafka'; sleep 0.1; done; " + STARTER_SCRIPT);
        waitingFor(Wait.forLogMessage(".*Waiting for start kafka.*", 1));
    }

    @Override
    @SneakyThrows
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        // Start broker on demand  with specified advertised listeners config
        String brokerAdvertisedListener = brokerAdvertisedListener(containerInfo);
        String advertisedListeners = String.join(",", getBootstrapServers(), brokerAdvertisedListener);
        //
        String command = "#!/bin/bash\n"
                + (imageName.contains("native") ? "/app/emk-application" : "./emk-application-boot/bin/emk-application")
                + " --app.kafka.advertised.listeners="
                + advertisedListeners;
        copyFileToContainer(Transferable.of(command, 0777), STARTER_SCRIPT);
    }

    public String getBootstrapServers() {
        return String.format("PLAINTEXT://%s:%s", getHost(), getMappedPort(KAFKA_PORT));
    }

    protected String brokerAdvertisedListener(InspectContainerResponse containerInfo) {
        return String.format("BROKER://%s:%s", containerInfo.getConfig().getHostName(), "9092");
    }
}