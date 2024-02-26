package pw.avvero.emk;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.*;
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

    public static final int OFFSET_COMMIT_WAIT_ATTEMPTS_MAX = 200;
    public static final int OFFSET_COMMIT_WAIT_TIME = 50;

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

    /**
     * Waits for the offset commit for a given list of bootstrap servers retrieved from the application context.
     *
     * @param applicationContext The Spring application context from which to retrieve Kafka connection details.
     * @throws ExecutionException if an error occurs during the fetching of consumer group or topic information.
     * @throws InterruptedException if the current thread is interrupted while waiting.
     */
    public static void waitForPartitionOffsetCommit(ApplicationContext applicationContext) throws ExecutionException,
            InterruptedException {
        List<String> bootstrapServers = applicationContext.getBean(KafkaConnectionDetails.class).getBootstrapServers();
        waitForPartitionOffsetCommit(bootstrapServers);
    }

    /**
     * Waits for the offset commit across all consumer groups for all topics in the provided list of bootstrap servers.
     * This method checks the offset commit for each partition of each topic and ensures that all consumer groups have
     * committed their offsets. It continuously checks the offsets until they are committed or until a maximum number of
     * attempts is reached.
     *
     * @param bootstrapServers The list of bootstrap servers for the Kafka cluster.
     * @throws InterruptedException if the thread is interrupted while waiting for the offsets to commit.
     * @throws ExecutionException if an error occurs during the fetching of consumer group or topic information.
     */
    public static void waitForPartitionOffsetCommit(List<String> bootstrapServers)
            throws InterruptedException, ExecutionException {
        log.debug("[EMK] Waiting for offset commit is requested");
        long startTime = System.currentTimeMillis();
        try (AdminClient adminClient = AdminClient.create(singletonMap(BOOTSTRAP_SERVERS_CONFIG, bootstrapServers))) {
            Set<String> consumerGroups = adminClient.listConsumerGroups().all().get()
                    .stream().map(ConsumerGroupListing::groupId).collect(Collectors.toSet());
            // List the topics available in the cluster
            Set<String> topics = adminClient.listTopics().namesToListings().get().keySet();
            Map<TopicPartition, Long> topicsOffsets = getOffsetsForTopics(adminClient, topics);
            Queue<TopicPartition> topicQueue = new LinkedList<>(topicsOffsets.keySet());
            int attempt = 0;
            while (!topicQueue.isEmpty()) {
                TopicPartition tp = topicQueue.remove();
                long topicOffset = topicsOffsets.get(tp);
                if (++attempt > OFFSET_COMMIT_WAIT_ATTEMPTS_MAX) {
                    throw new RuntimeException("Exceeded maximum attempts (" + OFFSET_COMMIT_WAIT_ATTEMPTS_MAX
                            + ") waiting for offset commit for partition " + tp + ".");
                }
                // Get current offsets for partitions
                // TODO slow
                Map<String, Long> consumerGroupsOffsets = getOffsetsForConsumerGroups(adminClient, consumerGroups, tp);
                for (String consumerGroup : consumerGroups) {
                    Long consumerGroupOffset = consumerGroupsOffsets.get(consumerGroup);
                    if (consumerGroupOffset == null) {
                        log.trace("[EMK] Waiting for offset commit for topic {} in group {}: topic is not under capture",
                                tp.topic(), consumerGroup);
                    } else {
                        log.trace("[EMK] Waiting for offset commit for topic {} in group {}: [topic offset: {} != group offset: {}]",
                                tp.topic(), consumerGroup, consumerGroupOffset, topicOffset);
                    }
                    if (consumerGroupOffset != null && consumerGroupOffset != topicOffset) {
                        try {
                            Thread.sleep(OFFSET_COMMIT_WAIT_TIME); // NOSONAR magic #
                        }
                        catch (@SuppressWarnings("unused") InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        log.warn("[EMK] Consumer group {} offset for topic '{}' is {}, which is not equal to the topic offset {}. " +
                                        "Waiting for further message processing before proceeding. Refreshing end offsets and reevaluating.",
                                consumerGroup, tp.topic(), consumerGroupOffset, topicOffset);
                        topicsOffsets = getOffsetsForTopics(adminClient, topics);
                        List<TopicPartition> sortedTopicPartitions = topicsOffsets.keySet().stream()
                                .sorted((a, b) -> a.topic().equals(tp.topic()) ? -1 : b.topic().equals(tp.topic()) ? 1 : 0)
                                .toList();
                        topicQueue.clear();
                        topicQueue.addAll(sortedTopicPartitions);
                    }
                }
            }
        }
        log.debug("[EMK] Waiting for offset commit is finished in {} ms", System.currentTimeMillis() - startTime);
    }

    public static Map<TopicPartition, Long> getOffsetsForTopics(AdminClient adminClient, Set<String> topics)
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

    public static Map<String, Long> getOffsetsForConsumerGroups(AdminClient adminClient,
                                                                Set<String> consumerGroups,
                                                                TopicPartition topicPartition)
            throws ExecutionException, InterruptedException {
        Map<String, ListConsumerGroupOffsetsSpec> groupSpecs = new HashMap<>();
        for (String consumerGroup : consumerGroups) {
            ListConsumerGroupOffsetsSpec spec = new ListConsumerGroupOffsetsSpec();
            spec.topicPartitions(List.of(topicPartition));
            groupSpecs.put(consumerGroup, spec);
        }
        ListConsumerGroupOffsetsResult offsetsResult = adminClient.listConsumerGroupOffsets(groupSpecs);
        Map<String, Long> currentOffsets = new HashMap<>();
        for (String consumerGroup : consumerGroups) {
            offsetsResult.partitionsToOffsetAndMetadata(consumerGroup).get().forEach((tp, oam) -> {
                if (!topicPartition.equals(tp)) throw new RuntimeException("Wrong topic partition in response");
                currentOffsets.put(consumerGroup, oam != null ? oam.offset() : null);
            });
        }
        return currentOffsets;
    }
}