package pw.avvero.emk;

import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class KafkaContainerConfiguration {

    @Bean
    @RestartScope
    @ServiceConnection
    EmbeddedKafkaContainer kafkaEmbeddedContainer() {
        return new EmbeddedKafkaContainer("avvero/emk:latest"); // OR avvero/emk:latest
    }
}
