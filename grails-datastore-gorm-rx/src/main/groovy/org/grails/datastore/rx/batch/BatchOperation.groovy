package org.grails.datastore.rx.batch

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import org.grails.datastore.mapping.model.PersistentEntity

/**
 * Represents a batch operation
 *
 * @author Graeme Rocher
 * @since 6.0
 */

@CompileStatic
class BatchOperation {

    final Map<PersistentEntity, List<EntityOperation>> deletes = [:].withDefault { [] }
    final Map<PersistentEntity, List<EntityOperation>> updates = [:].withDefault { [] }
    final Map<PersistentEntity, List<EntityOperation>> inserts = [:].withDefault { [] }


    void addDelete(PersistentEntity entity, Serializable id, Object object) {
        deletes.get(entity).add(new EntityOperation(id, object))
    }

    void addUpdate(PersistentEntity entity, Serializable id, Object object) {
        updates.get(entity).add(new EntityOperation(id, object))
    }

    void addInsert(PersistentEntity entity, Object object) {
        inserts.get(entity).add(new EntityOperation(null, object))
    }


    @Canonical
    static class EntityOperation {
        final Serializable identity
        final Object object
    }
}
