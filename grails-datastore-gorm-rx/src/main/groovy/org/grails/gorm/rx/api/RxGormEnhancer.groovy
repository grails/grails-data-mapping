package org.grails.gorm.rx.api

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.core.connections.ConnectionSource
import org.grails.datastore.mapping.core.connections.ConnectionSourcesSupport
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.rx.RxDatastoreClient
import org.grails.datastore.rx.internal.RxDatastoreClientImplementor

import java.util.concurrent.ConcurrentHashMap
/**
 * Enhances {@link grails.gorm.rx.RxEntity} instances with behaviour necessary at runtime
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class RxGormEnhancer {

    private static final Map<String, Map<String,RxGormStaticApi>> STATIC_APIS = new ConcurrentHashMap<String, Map<String,RxGormStaticApi>>().withDefault { String key ->
        return new ConcurrentHashMap<String, RxGormStaticApi>()
    }
    private static final Map<String, Map<String, RxGormInstanceApi>> INSTANCE_APIS = new ConcurrentHashMap<String, Map<String, RxGormInstanceApi>>().withDefault { String key ->
        return new ConcurrentHashMap<String, RxGormInstanceApi>()
    }
    private static final Map<String, Map<String, RxGormValidationApi>> VALIDATION_APIS = new ConcurrentHashMap<String, Map<String, RxGormValidationApi>>().withDefault { String key ->
        return new ConcurrentHashMap<String, RxGormValidationApi>()
    }


    private RxGormEnhancer() {
    }

    static void close() {
        STATIC_APIS.clear()
        INSTANCE_APIS.clear()
        VALIDATION_APIS.clear()
    }

    static void registerEntity(PersistentEntity entity, RxDatastoreClient client) {
        List<String> connectionSourceNames = ConnectionSourcesSupport.getConnectionSourceNames(entity)
        String defaultConnectionSource = ConnectionSourcesSupport.getDefaultConnectionSourceName(entity)
        RxDatastoreClientImplementor rxDatastoreClientImplementor = (RxDatastoreClientImplementor) client

        if(defaultConnectionSource == ConnectionSource.ALL) {
            for(ConnectionSource cs in client.getConnectionSources()) {
                registerEntityWithConnectionSource(entity, cs.name, cs.name, rxDatastoreClientImplementor)
            }
        }
        else {
            registerEntityWithConnectionSource(entity, ConnectionSource.DEFAULT, defaultConnectionSource, rxDatastoreClientImplementor)
            for(String connectionSourceName in connectionSourceNames) {
                if(connectionSourceName == ConnectionSource.DEFAULT) continue
                registerEntityWithConnectionSource(entity, connectionSourceName, connectionSourceName, rxDatastoreClientImplementor)
            }
        }
    }

    protected static void registerEntityWithConnectionSource(PersistentEntity entity, String qualifierName, String connectionSourceName, RxDatastoreClientImplementor rxDatastoreClientImplementor) {
        String entityName = entity.getName()
        STATIC_APIS.get(qualifierName).put(entityName, rxDatastoreClientImplementor.createStaticApi(entity, connectionSourceName))
        INSTANCE_APIS.get(qualifierName).put(entityName, rxDatastoreClientImplementor.createInstanceApi(entity, connectionSourceName))
        VALIDATION_APIS.get(qualifierName).put(entityName, rxDatastoreClientImplementor.createValidationApi(entity, connectionSourceName))
    }


    static <T> RxGormStaticApi<T> findStaticApi(Class<T> type, String qualifier = ConnectionSource.DEFAULT) {
        def api = STATIC_APIS.get(qualifier).get(type.name)
        if(api == null) {
            throw stateException(type)
        }
        return api
    }

    static <T> RxGormInstanceApi<T> findInstanceApi(Class<T> type, String qualifier = ConnectionSource.DEFAULT) {
        def api = INSTANCE_APIS.get(qualifier).get(type.name)
        if(api == null) {
            throw stateException(type)
        }
        return api
    }

    static <T> RxGormValidationApi<T> findValidationApi(Class<T> type, String qualifier = ConnectionSource.DEFAULT) {
        def api = VALIDATION_APIS.get(qualifier).get(type.name)
        if(api == null) {
            throw stateException(type)
        }
        return api
    }

    private static IllegalStateException stateException(Class entity) {
        new IllegalStateException("Either class [$entity.name] is not a domain class or GORM has not been initialized correctly or has already been shutdown. If you are unit testing your entities using the mocking APIs")
    }

}
