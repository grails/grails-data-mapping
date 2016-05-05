package grails.gorm.rx.mongodb

import grails.gorm.rx.RxEntity
import org.bson.types.ObjectId

/**
 * Represents a reactive MongoDB document
 *
 * @author Graeme Rocher
 * @since 6.0
 */
trait RxMongoEntity<D> extends RxEntity<D> {

    /**
     * The id of the document
     */
    ObjectId id
}