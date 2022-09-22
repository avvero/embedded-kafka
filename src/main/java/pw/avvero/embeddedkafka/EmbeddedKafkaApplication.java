package pw.avvero.embeddedkafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.test.EmbeddedKafkaBrokerExtended;

@Slf4j
@SpringBootApplication
public class EmbeddedKafkaApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmbeddedKafkaApplication.class, args);
    }

    public static final int KAFKA_PORT = 9093;
    public static final int ZK_PORT = 2181;

    public static class It {

    }

    @Bean
    public It embeddedKafkaBroker(@Value("${app.kafka.advertised.listeners}") String advertisedListeners) {
        long start = System.currentTimeMillis();
        log.info("[KT] Kafka from testcontainers is going to start");
        String[] topics = new String[]{"topic1"};

        EmbeddedKafkaBrokerExtended broker = new EmbeddedKafkaBrokerExtended(1, true, 1, topics)
                .zkPort(ZK_PORT)
                .kafkaPorts(KAFKA_PORT)
                .brokerProperty("listeners", "PLAINTEXT://0.0.0.0:" + KAFKA_PORT + ",BROKER://0.0.0.0:9092")
                .brokerProperty("listener.security.protocol.map", "BROKER:PLAINTEXT,PLAINTEXT:PLAINTEXT")
                .brokerProperty("inter.broker.listener.name", "BROKER")
                .brokerProperty("advertised.listeners", advertisedListeners)
                .zkConnectionTimeout(EmbeddedKafkaBrokerExtended.DEFAULT_ZK_CONNECTION_TIMEOUT)
                .zkSessionTimeout(EmbeddedKafkaBrokerExtended.DEFAULT_ZK_SESSION_TIMEOUT);

        broker.afterPropertiesSet();
//        System.setProperty("spring.kafka.bootstrap-servers", broker.getBrokersAsString());
        long finish = System.currentTimeMillis() - start;
        log.info("[KT] Kafka from testcontainers is started on: {} (zookeeper: {}, advertised.listeners: {}) in {} millis",
                broker.getBrokersAsString(), broker.getZookeeperConnectionString(), advertisedListeners, finish);
        return new It();
    }

}
