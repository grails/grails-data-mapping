package org.grails.datastore.mapping.cassandra

import grails.gorm.CassandraEntity
import grails.gorm.tests.GormDatastoreSpec

import org.grails.datastore.mapping.cassandra.uuid.UUIDUtil

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class BasicPersistenceSpec extends GormDatastoreSpec {
    
	List getDomainClasses() {
		[TestEntity]
	}
	
    void testBasicPersistenceOperations() {
		given:
        def te = session.retrieve(TestEntity, UUIDUtil.getTimeUUID())

        assert te == null

        te = new TestEntity(name: "Bob", age: 45)

        session.persist(te)
        session.flush()
        
        assert te != null
        assert te.id != null
        assert te.id instanceof UUID

        session.clear()
        def t2 = session.retrieve(TestEntity, te.id)

        println t2.id.toString() + " - " + t2.name

        assert t2 != null
        assert t2.name == "Bob" 
        assert t2.age == 45 
        assert t2.id != null
        assert t2.id instanceof UUID

        te.age = 55
        session.persist(te)  
        def tcached = session.retrieve(TestEntity, te.id)
        assert tcached == te  
        session.flush()
        session.clear()
        
        te = session.retrieve(TestEntity, te.id)

        assert te != null
        assert te.id != null
        assert te.name == "Bob"
        assert te.age == 55 
        
        session.delete(te)
        session.flush()

        def deletedEntity = session.retrieve(TestEntity, te.id)
        assert deletedEntity == null
    }
}

@CassandraEntity
class TestEntity {
    
    String name
    int age
    
    static mapping = {
        version false
    }
    
}
