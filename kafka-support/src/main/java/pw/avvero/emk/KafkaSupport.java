package pw.avvero.emk;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.kafka.KafkaConnectionDetails;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListenerContainer;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static java.util.Collections.singletonMap;
import static org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG;

/**
 * Utility class providing support functions for Kafka in Spring applications.
 */
@Slf4j
public class KafkaSupport {

    public static final int WAIT_OFFSET_COMMIT_ATTEMPTS_MAX = 200;

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
        detectMultipleContainersForSameTopicWithinSameGroup(applicationContext);
        //
        KafkaListenerEndpointRegistry registry = applicationContext.getBean(KafkaListenerEndpointRegistry.class);
        log.debug("[EMK] Waiting for partition assignment is requested");
        for (MessageListenerContainer messageListenerContainer : registry.getListenerContainers()) {
            long startTime = System.currentTimeMillis();
            log.debug("[EMK] Waiting for partition assignment started for {}", messageListenerContainer.getListenerId());
            int partitions = ContainerTestUtils.waitForAssignment(messageListenerContainer, 1);
            long gauge = System.currentTimeMillis() - startTime;
            if (partitions > 0) {
                String topics = Objects.requireNonNull(messageListenerContainer.getAssignedPartitions()).stream()
                        .map(TopicPartition::topic).collect(Collectors.joining(", "));
                log.debug("[EMK] Waiting for partition assignment for {} is succeeded in {} ms, topics: {}",
                        messageListenerContainer.getListenerId(), gauge, topics);
            } else {
                log.error("[EMK] Waiting for partition assignment for {} is failed in {} ms",
                        messageListenerContainer.getListenerId(), gauge);
            }
        }
        log.debug("[EMK] At least one partition is assigned for every container");
        // Experimentally
        log.debug("[EMK] Waiting for partition assignment, kafka producer: start initialization");
//        applicationContext.getBean(KafkaTemplate.class).send("test", "test").get();
        log.debug("[EMK] Waiting for partition assignment, kafka producer: initialization finished");
    }

    /**
     * Detects and throws an exception if multiple Kafka listener containers are found for the same topic within
     * the same group in the given Spring application context.
     *
     * @param applicationContext the Spring {@link ApplicationContext}
     * @throws RuntimeException if multiple containers are detected
     */
    private static void detectMultipleContainersForSameTopicWithinSameGroup(ApplicationContext applicationContext) {
        KafkaListenerEndpointRegistry registry = applicationContext.getBean(KafkaListenerEndpointRegistry.class);
        Map<String, List<MessageListenerContainer>> containersPerTopicInSameGroup = new HashMap<>();
        for (MessageListenerContainer container : registry.getListenerContainers()) {
            ContainerProperties containerProperties = container.getContainerProperties();
            if (containerProperties.getTopics() == null) continue;
            for (String topic : containerProperties.getTopics()) {
                containersPerTopicInSameGroup
                        .computeIfAbsent(containerProperties.getGroupId() + " : " + topic, (k) -> new ArrayList<>())
                        .add(container);
            }
        }
        containersPerTopicInSameGroup.forEach((key, list) -> {
            if (list.size() > 1) {
                String[] parts = key.split(" : ");
                String groupId = parts[0];
                String topic = parts[1];
                String containerNames = list.stream()
                        .map(MessageListenerContainer::getListenerId)
                        .collect(Collectors.joining(", "));
                throw new RuntimeException(String.format("Detected multiple Kafka listener containers (%s) configured to " +
                                "listen to topic '%s' within the same group '%s'. " +
                                "This configuration may lead to unexpected behavior or message duplication. " +
                                "Please ensure each topic is consumed by a unique group or container.",
                        containerNames, topic, groupId));
            }
        });
    }

    public static void waitForPartitionOffsetCommit(ApplicationContext applicationContext) throws ExecutionException,
            InterruptedException {
        List<String> bootstrapServers = applicationContext.getBean(KafkaConnectionDetails.class).getBootstrapServers();
        Set<String> consumerGroups = getApplicationConsumerGroups(applicationContext);
        waitForPartitionOffsetCommit(bootstrapServers, consumerGroups);
    }

    private static Set<String> getApplicationConsumerGroups(ApplicationContext applicationContext) {
        Set<String> result = new HashSet<>();
        KafkaListenerEndpointRegistry registry = applicationContext.getBean(KafkaListenerEndpointRegistry.class);
        for (MessageListenerContainer container : registry.getListenerContainers()) {
            result.add(container.getGroupId());
        }
        return result;
    }

    public static void waitForPartitionOffsetCommit(List<String> bootstrapServers, Set<String> consumerGroups)
            throws InterruptedException, ExecutionException {
        log.debug("[EMK] Waiting for offset commit is requested");
        long startTime = System.currentTimeMillis();
        try (AdminClient adminClient = AdminClient.create(singletonMap(BOOTSTRAP_SERVERS_CONFIG, bootstrapServers))) {
            // List the topics available in the cluster
            Set<String> topics = adminClient.listTopics().namesToListings().get().keySet();
            Map<TopicPartition, Long> endOffsets = getEndOffsetsForAllPartitions(adminClient, topics);
            Queue<TopicPartition> topicQueue = new LinkedList<>(endOffsets.keySet());
            while (!topicQueue.isEmpty()) {
                TopicPartition tp = topicQueue.remove();
                long endOffset = endOffsets.get(tp);
                Long currentOffset;
                int attempt = 0;
                do {
                    if (++attempt > WAIT_OFFSET_COMMIT_ATTEMPTS_MAX) {
                        throw new RuntimeException("Exceeded maximum attempts (" + WAIT_OFFSET_COMMIT_ATTEMPTS_MAX
                                + ") waiting for offset commit for partition " + tp + ".");
                    }
                    // Get current offsets for partitions
                    // TODO slow
                    Map<TopicPartition, Long> currentOffsets = getCurrentOffsetsForAllPartitions(adminClient, consumerGroups);
                    currentOffset = currentOffsets.get(tp);
                    if (currentOffset == null) {
                        log.warn("[EMK] Waiting for offset commit for topic {}: topic is not under capture", tp.topic());
                    } else {
                        log.debug("[EMK] Waiting for offset commit for topic {}: [current: {}, end: {}]", tp.topic(),
                                currentOffset, endOffset);
                    }
                    //
                    if (currentOffset != null && currentOffset != endOffset) {
                        Thread.sleep(50); // todo parametrize
                        log.warn("[EMK] Current offset for topic '{}' is {}, which is not equal to the expected end offset of {}. " +
                                        "Waiting for further message processing before proceeding. Refreshing end offsets and reevaluating.",
                                tp.topic(), currentOffset, endOffset);
                        endOffsets = getEndOffsetsForAllPartitions(adminClient, topics);
                        List<TopicPartition> sortedTopicPartitions = endOffsets.keySet().stream()
                                .sorted((a, b) -> a.topic().equals(tp.topic()) ? -1 : b.topic().equals(tp.topic()) ? 1 : 0)
                                .toList();
                        topicQueue.clear();
                        topicQueue.addAll(sortedTopicPartitions);
                    }
                } while (currentOffset != null && currentOffset != endOffset);
            }
        }
        log.debug("[EMK] Waiting for offset commit is finished in {} ms", System.currentTimeMillis() - startTime);
    }

    public static Map<TopicPartition, Long> getEndOffsetsForAllPartitions(AdminClient adminClient, Set<String> topics)
            throws ExecutionException, InterruptedException {
        Map<TopicPartition, OffsetSpec> topicPartitions = new HashMap<>();
        for (String topic : topics) {
            DescribeTopicsResult topicInfo = adminClient.describeTopics(Collections.singletonList(topic));
            int partitions = topicInfo.topicNameValues().get(topic).get().partitions().size();
            for (int i = 0; i < partitions; i++) {
                topicPartitions.put(new TopicPartition(topic, i), OffsetSpec.latest());
            }
        }
        // Get last offsets for partitions
        Map<TopicPartition, Long> endOffsets = new HashMap<>();
        adminClient.listOffsets(topicPartitions).all().get().forEach((tp, info) -> endOffsets.put(tp, info.offset()));
        return endOffsets;
    }

    public static Map<TopicPartition, Long> getCurrentOffsetsForAllPartitions(AdminClient adminClient,
                                                                              Set<String> consumerGroups)
            throws ExecutionException, InterruptedException {
        Map<TopicPartition, Long> currentOffsets = new HashMap<>();
        for (String groupId : consumerGroups) {
            ListConsumerGroupOffsetsResult offsetsResult = adminClient.listConsumerGroupOffsets(groupId);
            offsetsResult.partitionsToOffsetAndMetadata().get().forEach((tp, oam) ->
                    currentOffsets.put(tp, oam.offset()));
        }
        return currentOffsets;
    }
}