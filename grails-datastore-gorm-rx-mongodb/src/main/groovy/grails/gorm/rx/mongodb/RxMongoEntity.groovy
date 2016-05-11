package grails.gorm.rx.mongodb

import grails.gorm.rx.RxEntity
import org.bson.types.ObjectId
import org.grails.datastore.gorm.schemaless.DynamicAttributes

/**
 * Represents a reactive MongoDB document
 *
 * @author Graeme Rocher
 * @since 6.0
 */
trait RxMongoEntity<D> implements RxEntity<D>, DynamicAttributes {

    /**
     * The id of the document
     */
    ObjectId id
}