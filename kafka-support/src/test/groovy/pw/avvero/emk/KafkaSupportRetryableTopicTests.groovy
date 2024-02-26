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
class KafkaSupportRetryableTopicTests extends Specification {

    @Autowired
    RecordCaptor recordCaptor
    @Autowired
    KafkaTemplate<Object, Object> kafkaTemplate
    @Autowired
    ApplicationContext applicationContext

    def "Can send message to topic and receive message from it"() {
        setup:
        KafkaSupport.waitForPartitionAssignment(applicationContext)
        def key = IdGenerator.getNext()
        when:
        Message message = MessageBuilder
                .withPayload("value")
                .setHeader(KafkaHeaders.TOPIC, "topicBroken")
                .setHeader(KafkaHeaders.KEY, key)
                .build()
        kafkaTemplate.send(message).get()
        KafkaSupport.waitForPartitionOffsetCommit(applicationContext)
        then:
        recordCaptor.getRecords("topicBroken", key) == ["value"]
        recordCaptor.getRecords("topicBroken-retry", key).size() == 2
//        recordCaptor.awaitAtMost(1, 200).getRecords("topicBroken-dlt", key).size() == 1
        recordCaptor.getRecords("topicBroken-dlt", key).size() == 1
    }
}
