package pw.avvero.emk;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Slf4j
@Component
public class Consumer {
    private final List<String> events = new ArrayList<>();
    @KafkaListener(id = "eventBucket", topics = {"topic1"}, groupId = "test")
    public void listener(@Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                         @Headers Map<String, Object> headers,
                         @Payload String body) {
        log.info("Message has come to test bucket.\n    Topic: {}\n    Headers: {}\n    Body: {}", topic, headers, body);
        events.add(body);
    }
}
