package pw.avvero.emk;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;

/**
 * Utility class providing support functions for Kafka in Spring applications.
 */
@Slf4j
public class KafkaSupport {

    /**
     * Waits for the partition assignment for all Kafka listener containers in the application context.
     * This method ensures that each Kafka listener container is assigned at least one partition
     * before proceeding. It also initializes Kafka producer by sending a test message.
     *
     * <p>This method is useful in scenarios where the application needs to wait for the Kafka
     * consumers to be fully set up and ready before performing certain operations.</p>
     *
     * @param applicationContext the Spring application context containing the Kafka listener containers.
     * @throws Exception if an error occurs during the process.
     */
    public static void waitForPartitionAssignment(ApplicationContext applicationContext) throws Exception {
        KafkaListenerEndpointRegistry registry = applicationContext.getBean(KafkaListenerEndpointRegistry.class);
        log.trace("[EMK] Waiting for partition assignment is requested");
        for (MessageListenerContainer messageListenerContainer : registry.getListenerContainers()) {
            long startTime = System.currentTimeMillis();
            log.trace("[EMK] Waiting for partition assignment started for {}", messageListenerContainer.getListenerId());
            int partitions = ContainerTestUtils.waitForAssignment(messageListenerContainer, 1);
            long gauge = System.currentTimeMillis() - startTime;
            if (partitions > 0) {
                log.trace("[EMK] Waiting for partition assignment for {} is succeeded in {} ms",
                        messageListenerContainer.getListenerId(), gauge);
            } else {
                log.error("[EMK] Waiting for partition assignment for {} is failed in {} ms",
                        messageListenerContainer.getListenerId(), gauge);
            }
        }
        log.trace("[EMK] At least one partition is assigned for every container");
        // Experimentally
        log.trace("[EMK] Waiting for partition assignment, kafka producer: start initialization");
        applicationContext.getBean(KafkaTemplate.class).send("test", "test").get();
        log.trace("[EMK] Waiting for partition assignment, kafka producer: initialization finished");
    }
}