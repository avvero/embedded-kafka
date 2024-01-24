package pw.avvero.emk;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaZKBroker;

import java.util.Map;

@Slf4j
@SpringBootApplication(scanBasePackages = "pw.avvero.emk")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    public static final int KAFKA_PORT = 9093;
    public static final int ZK_PORT = 2181;

    @Bean
    public EmbeddedKafkaBroker embeddedKafkaBroker(@Value("${app.kafka.advertised.listeners}") String advertisedListeners) {
        long start = System.currentTimeMillis();
        log.info("[EMK] Kafka from EmbeddedKafkaZKBroker is going to start");
        EmbeddedKafkaZKBroker broker = new EmbeddedKafkaBrokerObservable(1, true, 1) {
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                long finish = System.currentTimeMillis() - start;
                log.info("[EMK] Kafka from EmbeddedKafkaZKBroker is started on: {} (zookeeper: {}, advertised.listeners: {}) in {} millis",
                        this.getBrokersAsString(), this.getZookeeperConnectionString(), advertisedListeners, finish);
                return bean;
            }
        };
        broker.zkPort(ZK_PORT)
                .kafkaPorts(KAFKA_PORT)
                .zkConnectionTimeout(EmbeddedKafkaZKBroker.DEFAULT_ZK_CONNECTION_TIMEOUT)
                .zkSessionTimeout(EmbeddedKafkaZKBroker.DEFAULT_ZK_SESSION_TIMEOUT)
                .brokerProperties(Map.of(
                        "listeners", "PLAINTEXT://0.0.0.0:" + KAFKA_PORT + ",BROKER://0.0.0.0:9092",
                        "listener.security.protocol.map", "BROKER:PLAINTEXT,PLAINTEXT:PLAINTEXT",
                        "inter.broker.listener.name", "BROKER",
                        "advertised.listeners", advertisedListeners
                ));
        return broker;
    }

    public static class EmbeddedKafkaBrokerObservable extends EmbeddedKafkaZKBroker implements BeanPostProcessor {
        public EmbeddedKafkaBrokerObservable(int count, boolean controlledShutdown, int partitions, String... topics) {
            super(count, controlledShutdown, partitions, topics);
        }
    }
}
