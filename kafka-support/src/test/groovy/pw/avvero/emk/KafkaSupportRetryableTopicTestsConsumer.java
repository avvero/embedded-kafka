package pw.avvero.emk;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Data
@Slf4j
@Component
@AllArgsConstructor
public class KafkaSupportRetryableTopicTestsConsumer {
    @KafkaListener(id = "KafkaSupportRetryableTopicTestsConsumer", topics = "topicBroken",
            groupId = "KafkaSupportRetryableTopicTestsConsumer")
    public void consume(ConsumerRecord<Object, Object> record) {
//        throw new RuntimeException("Can't process event, i'm broken");
    }
}
