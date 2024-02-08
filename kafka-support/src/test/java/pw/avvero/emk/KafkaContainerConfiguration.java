package pw.avvero.emk;

import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class KafkaContainerConfiguration {

//    @Bean
//    @RestartScope
//    @ServiceConnection
//    EmbeddedKafkaContainer kafkaContainer() {
//        return new EmbeddedKafkaContainer("avvero/emk-native:1.0.0");
//    }

//    @Bean
//    @RestartScope
//    @ServiceConnection
//    KafkaContainer kafkaContainer() {
//        return new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.3.3"));
//    }
}
