package org.grails.gorm.rx.api

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.reflect.EntityReflector
import org.grails.datastore.rx.RxDatastoreClient
import rx.Observable
import rx.Single

/**
 * Bridge to the implementation of the instance method level operations
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class RxGormInstanceApi<D> {

    final PersistentEntity entity
    final RxDatastoreClient datastoreClient
    final EntityReflector entityReflector

    RxGormInstanceApi(PersistentEntity entity, RxDatastoreClient datastoreClient) {
        this.entity = entity
        this.datastoreClient = datastoreClient
        this.entityReflector = datastoreClient.mappingContext.getEntityReflector(entity)
    }

    Observable<D> save(D instance) {
        save(instance, Collections.<String,Object>emptyMap())
    }

    Observable<D> save(D instance, Map<String, Object> arguments) {
        datastoreClient.persist(instance, arguments)
    }

    Serializable ident(D instance) {
        entityReflector.getIdentifier(instance)
    }

    Observable<Boolean> delete(D instance) {
        datastoreClient.delete(instance)
    }
}
