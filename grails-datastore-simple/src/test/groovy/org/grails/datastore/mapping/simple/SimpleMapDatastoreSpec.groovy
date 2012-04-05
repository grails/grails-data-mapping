package org.grails.datastore.mapping.simple

import spock.lang.Specification
import org.grails.datastore.mapping.core.Session
import grails.gorm.tests.TestEntity
import grails.gorm.tests.GormDatastoreSpec

/**
 * @author Daniel Wiell
 */
class SimpleMapDatastoreSpec extends GormDatastoreSpec {
    def 'Test something'() {
        new TestEntity(age: 30, name: "Bob").save(failOnError: true)
        new TestEntity(age: 30, name: "Fred").save(failOnError: true)

        expect: TestEntity.findByAge(30, [sort: 'id', order: 'desc']).name == 'Fred'
    }
}
