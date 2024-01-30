/*
 * Copyright 2016-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modified by Belyaev Anton - modified methods:
 * - pw.avvero.emk.ContainerTestUtils.waitForAssignment,
 * - pw.avvero.emk.ContainerTestUtils.waitForSingleContainerAssignment.
 */

package pw.avvero.emk;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Utilities for testing listener containers. No hard references to container
 * classes are used to avoid circular project dependencies.
 *
 * @author Gary Russell
 * @since 1.0.3
 */
public final class ContainerTestUtils {

    private ContainerTestUtils() {
        // private ctor
    }

    /**
     * Wait until the container has the required number of assigned partitions
     * and return the actual number of assigned partitions.
     * This method has been modified from the original version:
     * - The method now returns an integer representing the number of assigned partitions.
     * - The method no longer throws an IllegalStateException if the actual number of
     *   assigned partitions does not match the expected number. Instead, it returns the actual
     *   count of assigned partitions.
     *
     * @param container the container.
     * @param partitions the number of partitions.
     * @return an integer value representing [описание возвращаемого значения].
     * @throws IllegalStateException if the operation cannot be completed (since 2.3.4) as
     * expected.
     * @throws ContainerTestUtilsException if the call to the container's
     * getAssignedPartitions() method fails.
     */
    public static int waitForAssignment(Object container, int partitions) { // NOSONAR complexity
        if (container.getClass().getSimpleName().contains("KafkaMessageListenerContainer")) {
            return waitForSingleContainerAssignment(container, partitions);
        }
        List<?> containers = KafkaTestUtils.getPropertyValue(container, "containers", List.class);
        int n = 0;
        int count = 0;
        Method getAssignedPartitions = null;
        while (n++ < 600 && count < partitions) { // NOSONAR magic #
            count = 0;
            for (Object aContainer : containers) {
                if (getAssignedPartitions == null) {
                    getAssignedPartitions = getAssignedPartitionsMethod(aContainer.getClass());
                }
                Collection<?> assignedPartitions;
                try {
                    assignedPartitions = (Collection<?>) getAssignedPartitions.invoke(aContainer);
                }
                catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    throw new ContainerTestUtilsException("Failed to invoke container method", e);
                }
                if (assignedPartitions != null) {
                    count += assignedPartitions.size();
                }
            }
            if (count < partitions) {
                try {
                    Thread.sleep(100); // NOSONAR magic #
                }
                catch (@SuppressWarnings("unused") InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        if (count != partitions) {
            //throw new IllegalStateException(String.format("Expected %d but got %d partitions", partitions, count));
        }
        return count;
    }

    private static int waitForSingleContainerAssignment(Object container, int partitions) {
        int n = 0;
        int count = 0;
        Method getAssignedPartitions = getAssignedPartitionsMethod(container.getClass());
        while (n++ < 600 && count < partitions) { // NOSONAR magic #
            count = 0;
            Collection<?> assignedPartitions;
            try {
                assignedPartitions = (Collection<?>) getAssignedPartitions.invoke(container);
            }
            catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
                throw new ContainerTestUtilsException("Failed to invoke container method", e1);
            }
            if (assignedPartitions != null) {
                count = assignedPartitions.size();
            }
            if (count < partitions) {
                try {
                    Thread.sleep(100); // NOSONAR magic #
                }
                catch (@SuppressWarnings("unused") InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        if (count != partitions) {
            throw new IllegalStateException(String.format("Expected %d but got %d partitions", partitions, count));
        }
        return count;
    }

    private static Method getAssignedPartitionsMethod(Class<?> clazz) {
        final AtomicReference<Method> theMethod = new AtomicReference<Method>();
        ReflectionUtils.doWithMethods(clazz,
                method -> theMethod.set(method),
                method -> method.getName().equals("getAssignedPartitions") && method.getParameterTypes().length == 0);
        if (theMethod.get() == null) {
            throw new IllegalStateException(clazz + " has no getAssignedPartitions() method");
        }
        return theMethod.get();
    }

    private static class ContainerTestUtilsException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        ContainerTestUtilsException(String message, Throwable cause) {
            super(message, cause);
        }

    }

}
