package org.grails.orm.hibernate

import grails.core.GrailsDomainClass
import grails.core.GrailsDomainClassProperty
import grails.persistence.Entity

import org.hibernate.id.IdentifierGenerationException
import org.springframework.orm.hibernate3.HibernateSystemException

class CustomIdSpec extends GormSpec {

    void 'Test saving an object with a custom id'() {
        when:
        def o = new ClassWithCustomId(name: 'Jeff')
        o.validate()
        then:
        !o.errors.hasErrors()
        o.save()
    }

    void 'Test saving an object with a custom id that uses the assigned generator'() {
        when:
        new ClassWithAssignedCustomId(name: 'Jeff').save()

        then:
        HibernateSystemException ex = thrown()
        ex.rootCause instanceof IdentifierGenerationException

        when:
        def o = new ClassWithAssignedCustomId(name: 'Jeff', myId: 42)

        then:
        o.save()
    }

    @Override
    List getDomainClasses() {
        [ClassWithCustomId, ClassWithAssignedCustomId]
    }
}

@Entity
class ClassWithCustomId {
    Long id
    Long version

    String name
    Long myId

    static mapping = {
        id name: 'myId'
    }
}

@Entity
class ClassWithAssignedCustomId {
    Long id
    Long version

    String name
    Long myId

    static mapping = {
        id name: 'myId', generator: 'assigned'
    }
}
