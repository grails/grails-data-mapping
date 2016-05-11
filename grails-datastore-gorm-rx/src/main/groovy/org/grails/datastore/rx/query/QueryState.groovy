package org.grails.datastore.rx.query

import groovy.transform.CompileStatic

/**
 *
 * Used to maintain query state and avoid hitting the database again when loading associations
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class QueryState {

    private final Map<Class, Map<Serializable, Object>> loadedEntities

    QueryState() {
        this.loadedEntities = [:].withDefault { [:] }
    }

    void addLoadedEntity(Class type, Serializable id, Object object) {
        loadedEntities.get(type).put(id, object)
    }

    public <T> T getLoadedEntity(Class<T> type, Serializable id) {
        (T)loadedEntities.get(type)?.get(id)
    }
}
