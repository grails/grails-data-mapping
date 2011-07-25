package org.grails.datastore.gorm.gemfire

import grails.gorm.tests.GormDatastoreSpec

/**
 * @author Burt Beckwith
 */
class GemfireUuidSpec extends GormDatastoreSpec {

    void "Test UUID"() {
        given:
            session.datastore.mappingContext.addPersistentEntity UsesUuid
            def u = new UsesUuid(name: 'Bob').save(flush: true)
            session.clear()

        when:
            u = UsesUuid.get(u.id)

        then:
            u.name == 'Bob'
            u.id instanceof String
            u.id ==~ /[a-f\d]{8}\-[a-f\d]{4}\-[a-f\d]{4}\-[a-f\d]{4}\-[a-f\d]{12}/

        when:
            u = UsesUuid.get(u.id)
            u.name = 'Not Bob'
            u.save(flush: true)
            session.clear()
            u = UsesUuid.get(u.id)

        then:
            u.name == 'Not Bob'
    }
}

class UsesUuid {

    String id
    String name

    static mapping = {
        id generator: 'uuid'    
    }
}
