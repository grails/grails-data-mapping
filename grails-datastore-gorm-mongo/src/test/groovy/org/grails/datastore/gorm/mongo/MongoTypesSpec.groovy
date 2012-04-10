package org.grails.datastore.gorm.mongo

import grails.gorm.tests.GormDatastoreSpec
import grails.persistence.Entity
import org.bson.types.ObjectId
import com.mongodb.DBObject
import org.bson.types.Binary
import com.mongodb.BasicDBObject

/**
 */
class MongoTypesSpec extends GormDatastoreSpec{
    
    void "Test that an entity can save and load native mongo types"() {
        when:"A domain class with mongodb types is saved and read"
            def mt = new MongoTypes()
            mt.bson = new BasicDBObject(foo:"bar")
            mt.binary = new Binary("foo".bytes)
            def otherId = new ObjectId()
            mt.otherId = otherId
            mt.save flush:true
            session.clear()
            mt = MongoTypes.get(mt.id)
        then:"Then it is in the correct state"
            mt != null
            mt.bson != null
            mt.bson.foo == 'bar'
            mt.binary.data == 'foo'.bytes
            mt.otherId == otherId
    }

    @Override
    List getDomainClasses() {
        [MongoTypes]
    }


}

@Entity
class MongoTypes {
    ObjectId id
    DBObject bson
    Binary binary
    ObjectId otherId
}
