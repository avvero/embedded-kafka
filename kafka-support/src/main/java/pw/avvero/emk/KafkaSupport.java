package pw.avvero.emk;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.kafka.KafkaConnectionDetails;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static java.util.Collections.singletonMap;
import static org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG;

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

    public static void waitForPartitionOffsetCommit(ApplicationContext applicationContext) throws InterruptedException,
            ExecutionException {
        KafkaConnectionDetails kafkaConnectionDetails = applicationContext.getBean(KafkaConnectionDetails.class);
        try (AdminClient adminClient = AdminClient.create(singletonMap(BOOTSTRAP_SERVERS_CONFIG, kafkaConnectionDetails.getBootstrapServers()))) {
            // Get partitions and topics
            Map<String, TopicListing> topics = adminClient.listTopics().namesToListings().get();
            Map<TopicPartition, OffsetSpec> topicPartitions = new HashMap<>();
            for (String topic : topics.keySet()) {
                DescribeTopicsResult topicInfo = adminClient.describeTopics(Collections.singletonList(topic));
                int partitions = topicInfo.topicNameValues().get(topic).get().partitions().size();
                for (int i = 0; i < partitions; i++) {
                    topicPartitions.put(new TopicPartition(topic, i), OffsetSpec.latest());
                }
            }

            // Get last offsets for partitions
            Map<TopicPartition, Long> endOffsets = new HashMap<>();
            adminClient.listOffsets(topicPartitions).all().get().forEach((tp, info) -> endOffsets.put(tp, info.offset()));

            for (Map.Entry<TopicPartition, Long> entry : endOffsets.entrySet()) {
                TopicPartition tp = entry.getKey();
                long endOffset = entry.getValue();
                Long currentOffset;
                do {
                    // Get current offsets for partitions
                    // TODO slow
                    Map<TopicPartition, Long> currentOffsets = getCurrentOffsetsForAllPartitions(applicationContext, adminClient);
                    currentOffset = currentOffsets.get(tp);
                    if (currentOffset == null) {
                        log.debug("[EMK] Waiting for offset commit: TODO no offsets for {}", tp.topic());
                    } else {
                        log.debug("[EMK] Waiting for offset commit for topic {}: [current: {}, end: {}]", tp.topic(),
                                currentOffset, endOffset);
                    }
                    //
                    if (currentOffset != null && currentOffset < endOffset) {
                        Thread.sleep(100);
                    }
                } while (currentOffset != null && currentOffset < endOffset);
            }
        }
    }

    public static Map<TopicPartition, Long> getCurrentOffsetsForAllPartitions(ApplicationContext applicationContext,
                                                                              AdminClient adminClient) throws ExecutionException,
            InterruptedException {
        Map<TopicPartition, Long> currentOffsets = new HashMap<>();
        KafkaListenerEndpointRegistry registry = applicationContext.getBean(KafkaListenerEndpointRegistry.class);
        for (MessageListenerContainer container : registry.getListenerContainers()) {
            String groupId = container.getGroupId();
            if (groupId != null) {
                ListConsumerGroupOffsetsResult offsetsResult = adminClient.listConsumerGroupOffsets(groupId);
                offsetsResult.partitionsToOffsetAndMetadata().get().forEach((tp, oam) ->
                        currentOffsets.put(tp, oam.offset()));
            }
        }
        return currentOffsets;
    }
}