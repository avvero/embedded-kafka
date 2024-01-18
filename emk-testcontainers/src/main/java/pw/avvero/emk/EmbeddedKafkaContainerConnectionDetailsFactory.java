package pw.avvero.emk;

import org.springframework.boot.autoconfigure.kafka.KafkaConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;

import java.util.List;

/**
 * See org.springframework.boot.testcontainers.service.connection.kafka.KafkaContainerConnectionDetailsFactory
 */
public class EmbeddedKafkaContainerConnectionDetailsFactory extends ContainerConnectionDetailsFactory<EmbeddedKafkaContainer, KafkaConnectionDetails> {

    @Override
    protected KafkaConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<EmbeddedKafkaContainer> source) {
        return new EmbeddedKafkaContainerConnectionDetailsFactory.KafkaContainerConnectionDetails(source);
    }

    /**
     * {@link KafkaConnectionDetails} backed by a {@link ContainerConnectionSource}.
     */
    private static final class KafkaContainerConnectionDetails extends ContainerConnectionDetails<EmbeddedKafkaContainer>
            implements KafkaConnectionDetails {

        private KafkaContainerConnectionDetails(ContainerConnectionSource<EmbeddedKafkaContainer> source) {
            super(source);
        }

        @Override
        public List<String> getBootstrapServers() {
            return List.of(getContainer().getBootstrapServers());
        }

    }

}