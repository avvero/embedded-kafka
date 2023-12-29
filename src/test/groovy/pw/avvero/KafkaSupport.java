package pw.avvero;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
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

    public static int waitForAssignment(Object container, int partitions) {
        if (container.getClass().getSimpleName().contains("KafkaMessageListenerContainer")) {
            return waitForSingleContainerAssignment(container, partitions);
        } else {
            List<?> containers = (List)KafkaTestUtils.getPropertyValue(container, "containers", List.class);
            int n = 0;
            int count = 0;
            Method getAssignedPartitions = null;

            while(n++ < 600 && count < partitions) {
                count = 0;
                Iterator var6 = containers.iterator();

                while(var6.hasNext()) {
                    Object aContainer = var6.next();
                    if (getAssignedPartitions == null) {
                        getAssignedPartitions = getAssignedPartitionsMethod(aContainer.getClass());
                    }

                    Collection assignedPartitions;
                    try {
                        assignedPartitions = (Collection)getAssignedPartitions.invoke(aContainer);
                    } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException var11) {
                        throw new ContainerTestUtilsException("Failed to invoke container method", var11);
                    }

                    if (assignedPartitions != null) {
                        count += assignedPartitions.size();
                    }
                }

                if (count < partitions) {
                    try {
                        Thread.sleep(100L);
                    } catch (InterruptedException var10) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            return count;
        }
    }

    private static int waitForSingleContainerAssignment(Object container, int partitions) {
        int n = 0;
        int count = 0;
        Method getAssignedPartitions = getAssignedPartitionsMethod(container.getClass());

        while(n++ < 600 && count < partitions) {
            count = 0;

            Collection assignedPartitions;
            try {
                assignedPartitions = (Collection)getAssignedPartitions.invoke(container);
            } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException var8) {
                throw new ContainerTestUtilsException("Failed to invoke container method", var8);
            }

            if (assignedPartitions != null) {
                count = assignedPartitions.size();
            }

            if (count < partitions) {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException var7) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        return count;
    }

    private static Method getAssignedPartitionsMethod(Class<?> clazz) {
        AtomicReference<Method> theMethod = new AtomicReference();
        ReflectionUtils.doWithMethods(clazz, (method) -> {
            theMethod.set(method);
        }, (method) -> {
            return method.getName().equals("getAssignedPartitions") && method.getParameterTypes().length == 0;
        });
        if (theMethod.get() == null) {
            throw new IllegalStateException("" + clazz + " has no getAssignedPartitions() method");
        } else {
            return (Method)theMethod.get();
        }
    }

    private static class ContainerTestUtilsException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        ContainerTestUtilsException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}