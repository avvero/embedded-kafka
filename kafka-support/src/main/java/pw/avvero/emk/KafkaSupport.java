package pw.avvero.emk;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;

import static java.lang.String.format;

/**
 * Provides method for waiting partition assignment
 * Copy of org.springframework.kafka.test.utils.ContainerTestUtils except one moment - this method do not provide
 * assertion at the end
 */
@Slf4j
public class KafkaSupport {

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
                String message = format("[EMK] Waiting for partition assignment for %s is failed in %s ms",
                        messageListenerContainer.getListenerId(), gauge);
                log.error(message);
            }
        }
        log.trace("[EMK] At least one partition is assigned for every container");
        // Experimentally
        log.trace("[EMK] Waiting for partition assignment, kafka producer: start initialization");
        applicationContext.getBean(KafkaTemplate.class).send("test", "test").get();
        log.trace("[EMK] Waiting for partition assignment, kafka producer: initialization finished");
    }
}