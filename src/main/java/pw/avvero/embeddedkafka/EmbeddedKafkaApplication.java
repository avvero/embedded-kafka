package pw.avvero.embeddedkafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.test.EmbeddedKafkaBroker;

@Slf4j
@SpringBootApplication
public class EmbeddedKafkaApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmbeddedKafkaApplication.class, args);
    }

    public static class It {

    }

    @Bean
    public It embeddedKafkaBroker() {
        long start = System.currentTimeMillis();
        log.info("[KT] Kafka from testcontainers is going to start");
        String[] topics = new String[]{"topic1"};
        EmbeddedKafkaBroker broker = new EmbeddedKafkaBroker(1, true, 1, topics)
                .zkPort(55901)
                .kafkaPorts(55900)
                .zkConnectionTimeout(EmbeddedKafkaBroker.DEFAULT_ZK_CONNECTION_TIMEOUT)
                .zkSessionTimeout(EmbeddedKafkaBroker.DEFAULT_ZK_SESSION_TIMEOUT);
        broker.afterPropertiesSet();
//        System.setProperty("spring.kafka.bootstrap-servers", broker.getBrokersAsString());
        long finish = System.currentTimeMillis() - start;
        log.info("[KT] Kafka from testcontainers is started on: {} (zookeeper: {}) in {} millis",
                broker.getBrokersAsString(), broker.getZookeeperConnectionString(), finish);
        return new It();
    }

}
