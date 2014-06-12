package grails.gorm.tests

import grails.gorm.JpaEntity
import grails.persistence.Entity

class NotNullQuerySpec extends GormDatastoreSpec {

    static {
        GormDatastoreSpec.TEST_CLASSES << NullMe
    }

    void "Test query of null value with dynamic finder"() {
        given:
            new NullMe(name:"Bob", job:"Builder").save()
            new NullMe(name:"Fred").save()

        when:
            def results = NullMe.findAllByJobIsNull()

        then:
            results.size() == 1
            results[0].name == "Fred"

        when:
            results = NullMe.findAllByJobIsNotNull()

        then:
            results.size() == 1
            results[0].name == "Bob"
    }

    void "Test query of null value with criteria query"() {
        given:
            new NullMe(name:"Bob", job:"Builder").save()
            new NullMe(name:"Fred").save()

        when:
            def results = NullMe.withCriteria { isNull "job" }

        then:
            results.size() == 1
            results[0].name == "Fred"

        when:
            results = NullMe.withCriteria { isNotNull "job" }

        then:
            results.size() == 1
            results[0].name == "Bob"
    }
}

@JpaEntity
class NullMe {
    String name
    String job

    static constraints = {
        job nullable:true
    }

    static mapping = {
        job index:true
    }
}

