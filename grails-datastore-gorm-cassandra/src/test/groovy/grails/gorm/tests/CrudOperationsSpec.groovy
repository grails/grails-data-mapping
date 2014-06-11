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
			def p
        when:
            t = TestEntity.get("1")
			p = PersonAssignedId.get("none")
        then:
            t == null
			p == null
        when: 
            t = TestEntity.get(UUIDUtil.randomTimeUUID)
			p = PersonAssignedId.get([firstName:"none"])
        then:
            t == null
			p == null
    }

    void "Test basic CRUD operations"() {
        given:

            def t = new TestEntity(name:"Bob")
			def p = new PersonAssignedId(firstName: "Bob", lastName: "Wilson", age: 25)
            t.save()
			p.save()
        when:
            def results = TestEntity.list()
            t = TestEntity.get(t.id)
			def results2 = PersonAssignedId.list()
			p = PersonAssignedId.get([firstName:p.firstName, lastName: p.lastName])

        then:
            t != null
            t.id != null
            "Bob" == t.name
            1 == results.size()
            "Bob" == results[0].name
			
			p != null
			"Bob" == p.firstName
			"Wilson" == p.lastName
			25 == p.age
			1 == results2.size()
			"Bob" == results2[0].firstName
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
