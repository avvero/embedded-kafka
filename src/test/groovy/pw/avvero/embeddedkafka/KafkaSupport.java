package pw.avvero.embeddedkafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
        log.trace("[KT] Wait for partition assignment is provided by setup method interceptor");
        for (MessageListenerContainer messageListenerContainer : registry.getListenerContainers()) {
            long startTime = System.currentTimeMillis();
            log.trace("[KT] Partition assignment started for {}", messageListenerContainer.getListenerId());
            int partitions = waitForAssignment(messageListenerContainer, 1);

            long gauge = System.currentTimeMillis() - startTime;
            if (partitions > 0) {
                log.trace("[KT] Partition assignment for {} is succeeded in {} ms",
                        messageListenerContainer.getListenerId(), gauge);
            } else {
                String message = format("[KT] Partition assignment for %s is failed in %s ms",
                        messageListenerContainer.getListenerId(), gauge);
                log.error(message);
//                throw new RuntimeException(message);
            }
        }
        log.trace("[KT] At least one partition is assigned for every container");

        // Experimentally
        log.trace("[KT] Kafka producer: start initialization");
        applicationContext.getBean(KafkaTemplate.class).send("test", "test").get();
        log.trace("[KT] Kafka producer: initialization finished");
    }

    public static int waitForAssignment(Object container, int partitions) throws Exception {
        if (container.getClass().getSimpleName().contains("KafkaMessageListenerContainer")) {
            return waitForSingleContainerAssignment(container, partitions);
        }
        List<?> containers = KafkaTestUtils.getPropertyValue(container, "containers", List.class);
        int n = 0;
        int count = 0;
        Method getAssignedPartitions = null;
        while (n++ < 200 && count < partitions) {
            count = 0;
            for (Object aContainer : containers) {
                if (getAssignedPartitions == null) {
                    getAssignedPartitions = getAssignedPartitionsMethod(aContainer.getClass());
                }
                Collection<?> assignedPartitions = (Collection<?>) getAssignedPartitions.invoke(aContainer);
                if (assignedPartitions != null) {
                    count += assignedPartitions.size();
                }
                KafkaMessageListenerContainer kafkaContainer = (KafkaMessageListenerContainer) aContainer;
                log.debug("[KT] Wait for partition assignment, consumer {} is connected to {} with partitions: {}",
                        kafkaContainer.getBeanName(), kafkaContainer.getContainerProperties().getTopics(),
                        assignedPartitions.size());
            }
            if (count < partitions) {
                Thread.sleep(100);
            }
        }
        return count;
    }

    private static int waitForSingleContainerAssignment(Object container, int partitions)
            throws Exception {
        int n = 0;
        int count = 0;
        Method getAssignedPartitions = getAssignedPartitionsMethod(container.getClass());
        while (n++ < 200 && count < partitions) {
            count = 0;
            Collection<?> assignedPartitions = (Collection<?>) getAssignedPartitions.invoke(container);
            if (assignedPartitions != null) {
                count = assignedPartitions.size();
            }
            if (count < partitions) {
                Thread.sleep(100);
            }
        }
        return count;
    }

    private static Method getAssignedPartitionsMethod(Class<?> clazz) {
        final AtomicReference<Method> theMethod = new AtomicReference<Method>();
        ReflectionUtils.doWithMethods(clazz, new ReflectionUtils.MethodCallback() {

            @Override
            public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
                theMethod.set(method);
            }
        }, new ReflectionUtils.MethodFilter() {

            @Override
            public boolean matches(Method method) {
                return method.getName().equals("getAssignedPartitions") && method.getParameterTypes().length == 0;
            }
        });
        return theMethod.get();
    }
}