package pw.avvero.emk;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
@Component
public class RecordCaptor {

    private final Map<String, Map<Object, List<Object>>> topicKeyRecords = new HashMap<>();

    public void capture(ConsumerRecord<Object, Object> record) {
        String topic = record.topic();
        Object key = record.key();
        Map<String, Object> headers = new HashMap<>();
        for (Header header : record.headers()) {
            headers.put(header.key(), new String(header.value(), UTF_8));
        }
        Object value = record.value();
        log.debug("[EMK] Record captured for topic {} for key {}\n    Headers: {}\n    Value: {}", topic, key, headers, value);
        topicKeyRecords.computeIfAbsent(topic, k -> new HashMap<>())
                .computeIfAbsent(key, k -> new ArrayList<>())
                .add(value);
    }

    public List<Object> getRecords(String topic, Object key) {
        return topicKeyRecords.getOrDefault(topic, Collections.emptyMap())
                .getOrDefault(key, Collections.emptyList());
    }

    public List<Object> getRecords(String topic) {
        return topicKeyRecords.getOrDefault(topic, Collections.emptyMap())
                .values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }
}
