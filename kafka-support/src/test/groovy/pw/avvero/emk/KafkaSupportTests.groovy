package pw.avvero.emk

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(profiles = "test")
@AutoConfigureMockMvc
@ContextConfiguration(classes = [TestApplication, KafkaContainerConfiguration])
@DirtiesContext
class KafkaSupportTests extends Specification {

    @Autowired
    RecordCaptor recordCaptor
    @Autowired
    KafkaTemplate<Object, Object> kafkaTemplate
    @Autowired
    ApplicationContext applicationContext

    def "Can send event to topic and receive event from it"() {
        setup:
        KafkaSupport.waitForPartitionAssignment(applicationContext)
        def key = IdGenerator.getNext()
        when:
        Message message = MessageBuilder
                .withPayload("value1")
                .setHeader(KafkaHeaders.TOPIC, "topic1")
                .setHeader(KafkaHeaders.KEY, key)
                .build()
        kafkaTemplate.send(message).get()
        KafkaSupport.waitForPartitionOffsetCommit(applicationContext)
        then:
        recordCaptor.getRecords("topic1", key) == ["value1"]
    }

    def "Can send many events to the same topic and receive them from it"() {
        setup:
        KafkaSupport.waitForPartitionAssignment(applicationContext)
        def key = IdGenerator.getNext()
        when:
        n.times {
            Message message = MessageBuilder
                    .withPayload("value" + it)
                    .setHeader(KafkaHeaders.TOPIC, "topic1")
                    .setHeader(KafkaHeaders.KEY, key)
                    .build()
            kafkaTemplate.send(message).get()
        }
        KafkaSupport.waitForPartitionOffsetCommit(applicationContext)
        then:
        recordCaptor.getRecords("topic1", key).size() == n
        where:
        n = 100
    }

    def "Can send many events to the different topics and receive them"() {
        setup:
        KafkaSupport.waitForPartitionAssignment(applicationContext)
        def key = IdGenerator.getNext()
        when:
        n.times {
            Message message = MessageBuilder
                    .withPayload("value" + it)
                    .setHeader(KafkaHeaders.TOPIC, "topic" + it)
                    .setHeader(KafkaHeaders.KEY, key)
                    .build()
            kafkaTemplate.send(message).get()
        }
        KafkaSupport.waitForPartitionOffsetCommit(applicationContext)
        then:
        n.times {
            assert recordCaptor.getRecords("topic" + it, key).size() == 1
        }
        where:
        n = 10
    }
}
