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
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import static org.springframework.http.MediaType.APPLICATION_JSON
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@ActiveProfiles(profiles = "test")
@ContextConfiguration
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.kafka.startup-mode=on-demand")
@DirtiesContext
class KafkaStartOnDemandTests extends Specification {

    @Autowired
    Consumer consumer
    @Autowired
    KafkaTemplate<Object, Object> kafkaTemplate
    @Autowired
    ApplicationContext applicationContext
    @Autowired
    MockMvc mockMvc

    def "Can send event to topic and receive event from it"() {
        setup:
        mockMvc.perform(post("/kafka/start")
                .contentType(APPLICATION_JSON)
                .content('{"advertisedListeners": "PLAINTEXT://localhost:9093,BROKER://localhost:9092"}')
                .accept(APPLICATION_JSON))
                .andExpect(status().isOk())
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
        expect: "Can start on demand if it's already started"
        mockMvc.perform(post("/kafka/start")
                .contentType(APPLICATION_JSON)
                .content('{"advertisedListeners": "PLAINTEXT://localhost:9093,BROKER://localhost:9092"}')
                .accept(APPLICATION_JSON))
                .andExpect(status().isOk())
        when:
        Message message2 = MessageBuilder
                .withPayload("value2")
                .setHeader(KafkaHeaders.TOPIC, "topic1")
                .build()
        kafkaTemplate.send(message2).get()
        Thread.sleep(2000) // TODO
        then:
        consumer.events == ["value1", "value2"]
    }

}
