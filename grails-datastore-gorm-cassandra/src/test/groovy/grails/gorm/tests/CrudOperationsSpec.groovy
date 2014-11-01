package grails.gorm.tests

import grails.persistence.Entity
import grails.validation.ValidationException

import org.grails.datastore.mapping.cassandra.utils.UUIDUtil

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
            p = PersonLastNamePartitionKey.get("none")
        then:
            t == null
            p == null
        when: 
            t = TestEntity.get(UUIDUtil.randomTimeUUID)
            p = PersonLastNamePartitionKey.get([lastName:"none"])
        then:
            t == null
            p == null
    }

    void "Test basic CRUD operations"() {
        given:
            def t = new TestEntity(name:"Bob")
			def p = new PersonLastNamePartitionKey(firstName: "Bob", lastName: "Wilson", age: 25)            
			
        when: "create"
    		t.save()
    		p.save(flush:true)
            def results = TestEntity.list()
            t = TestEntity.get(t.id)
            def results2 = PersonLastNamePartitionKey.list()
            p = PersonLastNamePartitionKey.get([firstName:p.firstName, lastName: p.lastName, age: 25])

        then: "read"
            t != null
            t.id != null
            "Bob" == t.name
            1 == results.size()
            "Bob" == results[0].name
			
            p != null
            "Bob" == p.firstName
            "Wilson" == p.lastName
            25 == p.age
			null == p.location
            1 == results2.size()
            "Bob" == results2[0].firstName
		
		when: "update"			
			t.name = "Jim"
			t.save()
			p.location = "UK"
			p.save(flush:true)
			session.clear()
			results = TestEntity.list()
			t = TestEntity.get(t.id)
			results2 = PersonLastNamePartitionKey.list()
			p = PersonLastNamePartitionKey.get([firstName:p.firstName, lastName: p.lastName, age: 25])
		
		then:
    		t != null
    		t.id != null
    		"Jim" == t.name
    		1 == results.size()
    		"Jim" == results[0].name
    		
    		p != null
    		"Bob" == p.firstName
    		"Wilson" == p.lastName
    		25 == p.age
			"UK" == p.location
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

	void "Test insert method"() {		
		given:
			def t = new TestEntity(name:"Bob")
			t.save(param:"one", flush: true)	
			t.discard()		
		when:
			def t2 = TestEntity.get(t.id)
			t2.discard()
			t2.insert(flush:true)
			
		then:
			!t.is(t2)
			t.id == t2.id
	}
	
	void "Test entity with string id"() {
		given:
			def s = new StringIdEntity(name:"Bob")
			s.save(flush:true)
			session.clear()
		when:
			s = StringIdEntity.get(s.id)
		then:
			s != null
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
	
	@Override
	public List getDomainClasses() {
		[StringIdEntity]
	}
}

@Entity
class StringIdEntity {
	String id
	String name	
}