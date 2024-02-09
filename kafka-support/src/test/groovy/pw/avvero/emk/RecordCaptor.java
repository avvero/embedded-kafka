package pw.avvero.emk;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.awaitility.Awaitility;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
@Component
public class RecordCaptor implements RecordCaptorAccess {

    private final Map<String, Map<Object, List<Object>>> topicKeyRecords = new ConcurrentHashMap<>();

    public void capture(ConsumerRecord<Object, Object> record) {
        String topic = record.topic();
        Object key = record.key();
        Map<String, Object> headers = new HashMap<>();
        for (Header header : record.headers()) {
            headers.put(header.key(), new String(header.value(), UTF_8));
        }
        Object value = record.value();
        log.debug("[EMK] Record captured for topic {} for key {}\n    Headers: {}\n    Value: {}", topic, key, headers, value);
        topicKeyRecords.computeIfAbsent(topic, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(key, k -> new CopyOnWriteArrayList<>())
                .add(value);
    }

    @Override
    public List<Object> getRecords(String topic, Object key) {
        return topicKeyRecords.getOrDefault(topic, Collections.emptyMap())
                .getOrDefault(key, Collections.emptyList());
    }

    public RecordCaptorAccess awaitAtMost(int numberOrRecords, long millis) {
        RecordCaptor recordCaptor = this;
        return (topic, key) -> {
            Supplier<List<Object>> supplier = () -> recordCaptor.getRecords(topic, key);
            Awaitility.await()
                    .atMost(millis, TimeUnit.MILLISECONDS)
                    .pollInterval(50, TimeUnit.MILLISECONDS)
                    .until(() -> supplier.get().size() != numberOrRecords);
            return supplier.get();
        };
    }

    public List<Object> getRecords(String topic) {
        return topicKeyRecords.getOrDefault(topic, Collections.emptyMap())
                .values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }
}
