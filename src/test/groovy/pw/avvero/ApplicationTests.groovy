package pw.avvero

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(profiles = "test")
@ContextConfiguration
class ApplicationTests extends Specification {

    @Autowired
    ApplicationContext applicationContext

    def "Context loads"() {
        expect:
        applicationContext != null
    }

}
