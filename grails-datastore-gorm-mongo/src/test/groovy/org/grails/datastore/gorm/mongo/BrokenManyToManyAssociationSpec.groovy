package org.grails.datastore.gorm.mongo

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import grails.gorm.tests.GormDatastoreSpec
import grails.persistence.Entity

/**
 * @author Noam Y. Tenne
 */
class BrokenManyToManyAssociationSpec extends GormDatastoreSpec {

    def 'test'() {
        given:
        ReferencingEntity referencing = new ReferencingEntity()
        referencing = referencing.save(flush: true)
        referencing.addToReferencedEntities(new ReferencedEntity().save())
        referencing.addToReferencedEntities(new ReferencedEntity().save())

        referencing.save(flush: true)
        session.clear()

        when:
        ((DBCollection) ReferencedEntity.collection).remove(new BasicDBObject('_id', ReferencedEntity.find{}.id))
        session.clear()
        referencing = ReferencingEntity.find{}

        then:
        referencing.referencedEntities.size() == 2
        referencing.referencedEntities.any { it == null }

        and:
        when:
        referencing.delete(flush: true)
        session.clear()

        then:
        ReferencedEntity.count == 0
        ReferencingEntity.count == 0
    }

    @Override
    List getDomainClasses() {
        [ReferencingEntity, ReferencedEntity]
    }
}

@Entity
class ReferencingEntity {
    String id
    Set<ReferencedEntity> referencedEntities
    static hasMany = [referencedEntities: ReferencedEntity]
}

@Entity
class ReferencedEntity {
    String id
    static belongsTo = ReferencingEntity
    Set<ReferencingEntity> referencingEntities
    static hasMany = [referencingEntities: ReferencingEntity]
}
