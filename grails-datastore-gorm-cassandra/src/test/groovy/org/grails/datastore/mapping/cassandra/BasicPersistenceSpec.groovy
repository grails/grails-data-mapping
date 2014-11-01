package org.grails.datastore.mapping.cassandra

import grails.gorm.tests.GormDatastoreSpec
import grails.gorm.tests.TestEntity

import org.grails.datastore.mapping.cassandra.utils.UUIDUtil

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class BasicPersistenceSpec extends GormDatastoreSpec {

    void testBasicPersistenceOperations() {
        when: "read non existent"
        	def te = session.retrieve(TestEntity, UUIDUtil.getRandomTimeUUID())

        then:
            te == null
        
        when: "save and flush"
            te = new TestEntity(name: "Bob", age: 45)    
            session.persist(te)
            session.flush()
			
        then:
            te != null
            te.id != null
            te.id instanceof UUID
    
        when: "read"
            session.clear()
            def t2 = session.retrieve(TestEntity, te.id)
    
        then:    
            t2 != null
            t2.name == "Bob"
            t2.age == 45
            t2.id != null
            t2.id instanceof UUID
        
        when: "update no flush"
            te.age = 55
            session.persist(te)
            def tcached = session.retrieve(TestEntity, te.id)
        then:
            tcached == te
        
        when: "flush and persist to db and read again"    
            session.flush()
            session.clear()             
            te = session.retrieve(TestEntity, te.id)
        
        then:
            te != null
            te.id != null
            te.name == "Bob"
            te.age == 55
        
        when: "delete"
            session.delete(te)
            session.flush()            
            def deletedEntity = session.retrieve(TestEntity, te.id)
        then:
             deletedEntity == null
    }
}