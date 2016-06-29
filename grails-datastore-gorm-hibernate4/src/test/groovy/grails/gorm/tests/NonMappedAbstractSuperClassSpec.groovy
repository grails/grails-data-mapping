package grails.gorm.tests

import grails.persistence.Entity
import org.grails.datastore.gorm.GormEntity
import org.grails.orm.hibernate.GormSpec
import spock.lang.Issue

/**
 *
 */
@Issue('Test for https://github.com/grails/grails-data-mapping/issues/621')
class NonMappedAbstractSuperClassSpec extends GormSpec {

    void "Test that it is possible to extend a non-GORM abstract class"()  {

        when:
        def c = new Concrete(name: "Bob")
        c.save(flush:true)

        then:
        !GormEntity.isAssignableFrom(Normal)
        GormEntity.isAssignableFrom(Concrete )
        !c.errors.hasErrors()
        Concrete.count() == 1
        Concrete.findByName('Bob').myDate

    }
    @Override
    List getDomainClasses() {
        [Concrete]
    }
}

@Entity
class Concrete extends Normal {
    String name
}

abstract class Normal {
    UUID id
    Date myDate


    boolean beforeInsert() {
        myDate = new Date()
        return true
    }

    static mapping = {
        id(generator: "uuid2", type: "uuid-binary")
    }

    static constraints = {
        myDate nullable:true
    }
}


