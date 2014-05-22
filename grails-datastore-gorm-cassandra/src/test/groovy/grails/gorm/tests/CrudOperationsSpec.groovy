package grails.gorm.tests

import grails.validation.ValidationException

import org.grails.datastore.mapping.cassandra.uuid.UUIDUtil

/**
 * @author graemerocher
 */
class CrudOperationsSpec extends GormDatastoreSpec {
	
    void "Test get using a string-based key"() {
        given:

            def t = new TestEntity(name:"Bob")
            t.save(flush:true)
            session.clear()
        when:
            t = TestEntity.get("${t.id}")

        then:
            t != null
    }

    void "Test get returns null of non-existent entity"() {
        given:
            def t
        when:
            t = TestEntity.get("1")
        then:
            t == null
        when: 
            t = TestEntity.get(UUIDUtil.randomTimeUUID)
        then:
            t == null
    }

    void "Test basic CRUD operations"() {
        given:

            def t = new TestEntity(name:"Bob")
            t.save()

        when:
            def results = TestEntity.list()
            t = TestEntity.get(t.id)

        then:
            t != null
            t.id != null
            "Bob" == t.name
            1 == results.size()
            "Bob" == results[0].name
    }

    void "Test save method that takes a map"() {

        given:
            def t = new TestEntity(name:"Bob")
            t.save(param:"one", flush: true)
            
        when:
            t = TestEntity.get(t.id)
        then:
            t.id != null
    }

    void "Test failOnError"() {
        given:
            def t = new TestEntity()

        when:
            t.save(failOnError: true)

        then:
            thrown ValidationException
            t.id == null
    }
}
