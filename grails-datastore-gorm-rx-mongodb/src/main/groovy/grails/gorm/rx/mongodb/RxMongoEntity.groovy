package grails.gorm.rx.mongodb

import grails.gorm.rx.RxGormEntity
import org.bson.types.ObjectId

/**
 * Represents a reactive MongoDB document
 *
 * @author Graeme Rocher
 * @since 6.0
 */
trait RxMongoEntity<D> extends RxGormEntity<D> {

    /**
     * The id of the document
     */
    ObjectId id
}