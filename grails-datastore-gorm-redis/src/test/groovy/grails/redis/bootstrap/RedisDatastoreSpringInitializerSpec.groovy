package grails.redis.bootstrap

import grails.persistence.Entity
import spock.lang.Specification

/**
 * Created by graemerocher on 17/11/15.
 */
class RedisDatastoreSpringInitializerSpec extends Specification {

    void "Test init redis data store"() {
        when:
        def init = new RedisDatastoreSpringInitializer(Person)
        init.configure()

        then:
        Person.count() == 0
    }
}

@Entity
class Person {
    Long id
    Long version
    String name

    static constraints = {
        name blank:false
    }
}
