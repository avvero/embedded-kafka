package pw.avvero.emk_container;

import org.springframework.boot.autoconfigure.kafka.KafkaConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;

import java.util.List;

/**
 * See org.springframework.boot.testcontainers.service.connection.kafka.KafkaContainerConnectionDetailsFactory
 */
public class KafkaEmbeddedContainerConnectionDetailsFactory extends ContainerConnectionDetailsFactory<KafkaEmbeddedContainer, KafkaConnectionDetails> {

    @Override
    protected KafkaConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<KafkaEmbeddedContainer> source) {
        return new KafkaEmbeddedContainerConnectionDetailsFactory.KafkaContainerConnectionDetails(source);
    }

    /**
     * {@link KafkaConnectionDetails} backed by a {@link ContainerConnectionSource}.
     */
    private static final class KafkaContainerConnectionDetails extends ContainerConnectionDetails<KafkaEmbeddedContainer>
            implements KafkaConnectionDetails {

        private KafkaContainerConnectionDetails(ContainerConnectionSource<KafkaEmbeddedContainer> source) {
            super(source);
        }

        @Override
        public List<String> getBootstrapServers() {
            return List.of(getContainer().getBootstrapServers());
        }

    }

}