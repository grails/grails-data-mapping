package org.grails.datastore.rx

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.engine.event.PostInsertEvent
import org.grails.datastore.mapping.engine.event.PostLoadEvent
import org.grails.datastore.mapping.engine.event.PostUpdateEvent
import org.grails.datastore.mapping.engine.event.PreInsertEvent
import org.grails.datastore.mapping.engine.event.PreLoadEvent
import org.grails.datastore.mapping.engine.event.PreUpdateEvent
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.proxy.ProxyHandler
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.mapping.reflect.EntityReflector
import org.grails.datastore.rx.batch.BatchOperation
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import rx.Observable
import rx.Subscriber

/**
 * Abstract implementation the {@link RxDatastoreClient} interface
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
abstract class AbstractRxDatastoreClient<T> implements RxDatastoreClient<T> {

    final MappingContext mappingContext
    ApplicationEventPublisher eventPublisher

    AbstractRxDatastoreClient(MappingContext mappingContext) {
        this.mappingContext = mappingContext
    }

    @Override
    boolean isSchemaless() {
        return false
    }

    /**
     * Retrieve and instance of the given type and id
     * @param type The type
     * @param id The id
     * @return An observable
     */
    @Override
    final <T1> Observable<T1> get(Class<T1> type, Serializable id) {
        def entity = mappingContext.getPersistentEntity(type.name)
        if(entity == null) {
            throw new IllegalArgumentException("Type [$type.name] is not a persistent type")
        }

        def event = new PreLoadEvent(this, entity)
        eventPublisher?.publishEvent(event)
        if(event.isCancelled()) {
            return Observable.just(null)
        }
        else {
            def result = getEntity(entity, type, id)
            if(result != null) {
                eventPublisher?.publishEvent(new PostLoadEvent(this, entity, mappingContext.createEntityAccess(entity, result)))
            }
            return result
        }
    }

    @Override
    final Query createQuery(Class type) {

        def entity = mappingContext.getPersistentEntity(type.name)
        if(entity == null) {
            throw new IllegalArgumentException("Type [$type.name] is not a persistent type")
        }

        return createEntityQuery(entity)
    }

    @Override
    final Observable<Boolean> delete(Object instance) {
        if(instance == null) throw new IllegalArgumentException("Cannot persist null instance")

        Class type = mappingContext.getProxyHandler().getProxiedClass(instance)

        def entity = mappingContext.getPersistentEntity(type.name)
        if(entity == null) {
            throw new IllegalArgumentException("Type [$type.name] is not a persistent type")
        }

        def reflector = mappingContext.getEntityReflector(entity)
        def identifier = reflector.getIdentifier(instance)
        if(identifier == null) {
            throw new IllegalArgumentException("The passed instance has not yet been persisted (null identifier)")
        }
        else {
            return deleteEntity(entity, identifier, instance)
        }
    }

    @Override
    Observable<Number> deleteAll(Iterable instances) {
        def ctx = this.mappingContext
        def proxyHandler = ctx.getProxyHandler()
        if(instances != null) {
            def batchOperation = new BatchOperation()
            for(o in instances) {
                Class type = proxyHandler.getProxiedClass(o)
                def entity = ctx.getPersistentEntity(type.name)
                if(entity == null) {
                    throw new IllegalArgumentException("Type [$type.name] of instance [$o] is not a persistent type")
                }
                def reflector = mappingContext.getEntityReflector(entity)
                def id = reflector.getIdentifier(o)
                if(id != null) {
                    batchOperation.addDelete(entity, id, o)
                }
            }

            return batchDelete(batchOperation)
        }
        else {
            return Observable.just(0)
        }
    }

    /**
     * Persist an instance
     * @param instance The instance
     *
     * @return An observable
     */
    @Override
    final <T1> Observable<T1> persist(T1 instance, Map<String, Object> arguments) {

        if(instance == null) throw new IllegalArgumentException("Cannot persist null instance")


        def proxyHandler = mappingContext.getProxyHandler()
        Class<T1> type = proxyHandler.getProxiedClass(instance)

        def entity = mappingContext.getPersistentEntity(type.name)
        if(entity == null) {
            throw new IllegalArgumentException("Type [$type.name] is not a persistent type")
        }

        ApplicationEventPublisher persistenceEventPublisher = this.eventPublisher
        EntityReflector reflector = mappingContext.getEntityReflector(entity)
        def identifier = reflector.getIdentifier(instance)
        if(identifier == null) {
            generateIdentifier(entity, instance, reflector)

            def ea = mappingContext.createEntityAccess(entity, instance)
            def preInsertEvent = new PreInsertEvent(this, entity, ea)


            persistenceEventPublisher?.publishEvent(preInsertEvent)

            if(!preInsertEvent.isCancelled()) {
                def batchOperation = new BatchOperation()
                batchOperation.addInsert(entity, instance)

                return batchWrite(batchOperation).map { Number total ->
                    if(total > 0) {
                        persistenceEventPublisher?.publishEvent(new PostInsertEvent(this, entity, ea))
                    }
                    return instance
                }
            }
            else {
                return Observable.just(null)
            }
        }
        else {
            // handle update
            if(!proxyHandler.isInitialized(instance)) {
                return Observable.just(instance)
            }

            if(instance instanceof DirtyCheckable) {
                if( !((DirtyCheckable)instance).hasChanged() ) {
                    return Observable.just(instance)
                }
            }
            def ea = mappingContext.createEntityAccess(entity, instance)
            def preUpdateEvent = new PreUpdateEvent(this, entity, ea)


            persistenceEventPublisher?.publishEvent(preUpdateEvent)

            if(!preUpdateEvent.isCancelled()) {
                def batchOperation = new BatchOperation()
                batchOperation.addUpdate(entity, identifier, instance)

                return batchWrite(batchOperation).map { Number total ->
                    if(total > 0) {
                        persistenceEventPublisher?.publishEvent(new PostUpdateEvent(this, entity, ea))
                    }
                    return instance
                }
            }
            else {
                return Observable.just(null)
            }
        }
    }

    @Override
    final <T1> Observable<T1> persist(T1 instance) {
        return persist(instance, Collections.<String,Object>emptyMap())
    }

    @Override
    Observable<List<Serializable>> persistAll(Iterable instances) {
        MappingContext ctx = this.mappingContext
        ApplicationEventPublisher eventPublisher = this.eventPublisher

        def proxyHandler = ctx.getProxyHandler()
        if(instances != null) {
            def batchOperation = new BatchOperation()
            List<Serializable> identifiers = []
            List<ApplicationEvent> postEvents = []
            for(o in instances) {
                Class type = proxyHandler.getProxiedClass(o)
                PersistentEntity entity = ctx.getPersistentEntity(type.name)
                EntityReflector entityReflector = ctx.getEntityReflector(entity)
                EntityAccess entityAccess = ctx.createEntityAccess(entity, o)
                if(entity == null) {
                    throw new IllegalArgumentException("Type [$type.name] of instance [$o] is not a persistent type")
                }
                def id = entityReflector.getIdentifier(o)
                if(id != null) {
                    def preUpdateEvent = new PreUpdateEvent(this, entity, entityAccess)
                    eventPublisher?.publishEvent(preUpdateEvent)
                    if(!preUpdateEvent.isCancelled()) {
                        batchOperation.addUpdate(entity, id, o)
                        postEvents.add(new PostUpdateEvent(this, entity, entityAccess))
                    }
                }
                else {
                    id = generateIdentifier(entity, o, entityReflector)
                    def preInsertEvent = new PreInsertEvent(this, entity, entityAccess)
                    eventPublisher?.publishEvent(preInsertEvent)
                    if(!preInsertEvent.isCancelled()) {
                        batchOperation.addInsert(entity, o)
                        postEvents.add(new PostInsertEvent(this, entity, entityAccess))
                    }
                }
                identifiers.add(id)
            }

            return batchWrite(batchOperation).map({
                if(eventPublisher != null) {
                    for(event in postEvents) {
                        eventPublisher.publishEvent(event)
                    }
                }
                identifiers
            })
        }
        else {
            return Observable.just([])
        }
    }

    /**
     * Executes a batch write operation
     *
     * @param operation The batch operation
     *
     * @return The total number of records updated, deleted or inserted
     */
    abstract Observable<Number> batchWrite(BatchOperation operation)

    /**
     * Executes a batch delete operation
     *
     * @param operation The batch operation
     *
     * @return The total number of records deleted
     */
    abstract Observable<Number> batchDelete(BatchOperation operation)

    /**
     * Generates an identifier for the given entity and instance
     *
     * @param entity
     * @param instance
     * @param reflector
     * @return The generated identifier
     */
    abstract Serializable generateIdentifier(PersistentEntity entity, Object instance, EntityReflector reflector)
    /**
     * Obtain the given entity for the given id
     *
     * @param entity The entity object
     * @param type The persistent type
     * @param id The identifier
     * @return An observable with the result
     */
    abstract <T1> Observable<T1> getEntity(PersistentEntity entity, Class<T1> type, Serializable id)

    /**
     * Saves a new instance of the given entity for the given arguments
     *
     * @param entity The entity
     * @param type The persistent type
     * @param instance The instance
     * @param arguments The arguments
     * @return
     */
    abstract <T1> Observable<T1> saveEntity(PersistentEntity entity, Class<T1> type, T1 instance, Map<String, Object> arguments)


    /**
     * Updates an existing instance of the given entity for the given arguments
     *
     * @param entity The entity
     * @param type The persistent type
     * @param instance The instance
     * @param arguments The arguments
     * @return
     */
    abstract <T1> Observable<T1> updateEntity(PersistentEntity entity, Class<T1> type, Serializable id, T1 instance, Map<String, Object> arguments)

    /**
     * Deletes an instance
     *
     * @param entity The entity
     * @param id The id
     * @param instance The instance
     *
     * @return An observable
     */
    abstract Observable<Boolean> deleteEntity(PersistentEntity entity, Serializable id, Object instance)
    /**
     * Creates a query for the given entity
     *
     * @param entity The entity
     *
     * @return The query object
     */
    abstract Query createEntityQuery(PersistentEntity entity)
}
