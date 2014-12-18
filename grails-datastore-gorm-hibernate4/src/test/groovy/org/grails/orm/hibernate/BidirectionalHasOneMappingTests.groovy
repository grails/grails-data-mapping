package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test
import spock.lang.Issue

class BidirectionalHasOneMappingTests extends AbstractGrailsHibernateTests {


    // test for GRAILS-5581
    @Test
    @Issue('GRAILS-5581')
    void testRefreshHasOneAssociation() {
        def foo = new BidirectionalHasOneFoo()
        assert foo.save(failOnError:true)
        foo.refresh()
    }

    @Override
    protected getDomainClasses() {
        [BidirectionalHasOneBar, BidirectionalHasOneFoo]
    }
}

@Entity
class BidirectionalHasOneFoo {
    Long id
    Long version

    BidirectionalHasOneBar bar
    static hasOne = [bar:BidirectionalHasOneBar]

    static constraints = {
        bar(nullable:true)
    }
}

@Entity
class BidirectionalHasOneBar {
    Long id
    Long version

    BidirectionalHasOneFoo foo
}
