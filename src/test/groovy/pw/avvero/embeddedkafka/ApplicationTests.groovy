package pw.avvero.embeddedkafka

import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest
class ApplicationTests extends Specification {

    def "Context loads"() {
        expect:
        1 == 1
    }

}
