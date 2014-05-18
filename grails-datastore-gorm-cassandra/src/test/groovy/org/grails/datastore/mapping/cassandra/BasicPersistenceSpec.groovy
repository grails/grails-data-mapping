package org.grails.datastore.mapping.cassandra

import grails.gorm.tests.GormDatastoreSpec
import grails.gorm.tests.TestEntity

import org.grails.datastore.mapping.cassandra.uuid.UUIDUtil

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class BasicPersistenceSpec extends GormDatastoreSpec {

    void testBasicPersistenceOperations() {
        when:
        def te = session.retrieve(TestEntity, UUIDUtil.getTimeUUID())

        then:
            te == null
        
        when:
            te = new TestEntity(name: "Bob", age: 45)
    
            session.persist(te)
            session.flush()
        then:
            te != null
            te.id != null
            te.id instanceof UUID
    
        when:
            session.clear()
            def t2 = session.retrieve(TestEntity, te.id)
    
        then:
    
            t2 != null
            t2.name == "Bob"
            t2.age == 45
            t2.id != null
            t2.id instanceof UUID
        
        when:
            te.age = 55
            session.persist(te)
            def tcached = session.retrieve(TestEntity, te.id)
        then:
            tcached == te
        
        when:    
            session.flush()
            session.clear()             
            te = session.retrieve(TestEntity, te.id)
        
        then:
            te != null
            te.id != null
            te.name == "Bob"
            te.age == 55
        
        when:
            session.delete(te)
            session.flush()            
            def deletedEntity = session.retrieve(TestEntity, te.id)
        then:
             deletedEntity == null
    }
}