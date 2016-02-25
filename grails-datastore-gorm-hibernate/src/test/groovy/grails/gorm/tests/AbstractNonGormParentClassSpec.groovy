package grails.gorm.tests

import grails.persistence.Entity
import org.grails.orm.hibernate.GormSpec

/**
 * Created by graemerocher on 25/02/16.
 */
class AbstractNonGormParentClassSpec extends GormSpec{

    void "Test a concrete domain class that extends a common base class"() {
        expect:
        sessionFactory.currentSession.connection().prepareStatement("select * from concrete_foo").executeQuery()

    }
    @Override
    List getDomainClasses() {
        [ConcreteFoo]
    }
}

abstract class Common {
    String foo
}

@Entity
class ConcreteFoo extends Common {
    String bar
}
