package pw.avvero.embeddedkafka

import org.awaitility.Awaitility
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import spock.lang.Specification

import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

@SpringBootTest
class TopicTests extends Specification {

    @Autowired
    Consumer consumer
    @Autowired
    KafkaTemplate<Object, Object> kafkaTemplate
    @Autowired
    ApplicationContext applicationContext

    def "Can send event to topic and receive event from it"() {
        setup:
        KafkaSupport.waitForPartitionAssignment(applicationContext)
        when:
        Message message = MessageBuilder
                .withPayload("value1")
                .setHeader(KafkaHeaders.TOPIC, "topic1")
                .build()
        kafkaTemplate.send(message).get()
        Thread.sleep(2000) // TODO
        then:
        consumer.events == ["value1"]
    }

}
