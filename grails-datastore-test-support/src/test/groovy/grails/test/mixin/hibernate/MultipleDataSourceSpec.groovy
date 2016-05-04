package grails.test.mixin.hibernate

import grails.persistence.Entity
import spock.lang.Specification

/**
 * Created by graemerocher on 04/04/16.
 */
import grails.test.mixin.Mock
import grails.test.mixin.TestMixin
import grails.test.mixin.hibernate.HibernateTestMixin

@TestMixin(HibernateTestMixin)
class MultipleDataSourceSpec extends Specification{


    void setupSpec() {
        def config = new ConfigSlurper().parse '''
dataSources {
    dataSource{
        username = 'sa'
    }
    ds2 {
        username = 'sa'
    }
}
'''
        hibernateDomain(config,[FooOne, FooTwo])
    }

    def "test hibernate store works with domains mapped to multiple datasources"() {
        def foo1 = new FooOne(fooNameOne: "foo 1")
        def foo2 = new FooTwo(fooNameTwo: "foo 2")

        when: "foos are saved"
        foo1.save flush: true, failOnError: true
        foo2.save flush: true, failOnError: true

        then: "everything ok"
        FooOne.first().fooNameOne == "foo 1"
        FooTwo.first().fooNameTwo == "foo 2"
    }
}

@Entity
class FooOne {
    String fooNameOne
}


@Entity
class FooTwo {
    String fooNameTwo

    static mapping = {
        datasource "ds2"
    }
}
