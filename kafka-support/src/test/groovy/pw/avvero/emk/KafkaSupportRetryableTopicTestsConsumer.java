package pw.avvero.emk;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.SameIntervalTopicReuseStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Data
@Slf4j
@Component
@AllArgsConstructor
public class KafkaSupportRetryableTopicTestsConsumer {
    @KafkaListener(id = "KafkaSupportRetryableTopicTestsConsumer", topics = "topicBroken",
            groupId = "KafkaSupportRetryableTopicTestsConsumer")
    @RetryableTopic(
            dltTopicSuffix = "-dlt",
            retryTopicSuffix = "-retry",
            sameIntervalTopicReuseStrategy = SameIntervalTopicReuseStrategy.SINGLE_TOPIC,
            backoff = @Backoff(delayExpression = "100"),
            attempts = "3")
    @Transactional
    public void consume(ConsumerRecord<Object, Object> record) {
        throw new RuntimeException("Can't process event, i'm broken");
    }
}
