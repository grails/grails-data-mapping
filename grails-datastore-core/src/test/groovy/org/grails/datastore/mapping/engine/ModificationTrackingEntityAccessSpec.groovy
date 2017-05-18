package org.grails.datastore.mapping.engine

import spock.lang.Specification

class ModificationTrackingEntityAccessSpec extends Specification {

    void "test getProperty"() {
        given:
        def target = Mock(EntityAccess)
        def entityAccess = new ModificationTrackingEntityAccess(target)

        when:
        entityAccess.getProperty('foo')

        then:
        1 * target.getProperty('foo')
    }
}
