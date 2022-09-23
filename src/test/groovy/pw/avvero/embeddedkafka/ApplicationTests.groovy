package pw.avvero.embeddedkafka

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import spock.lang.Specification

@SpringBootTest
@DirtiesContext
class ApplicationTests extends Specification {

    def "Context loads"() {
        expect:
        1 == 1
    }

}
