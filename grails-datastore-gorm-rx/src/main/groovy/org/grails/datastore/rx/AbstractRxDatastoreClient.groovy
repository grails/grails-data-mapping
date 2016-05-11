package org.grails.datastore.rx

import grails.gorm.rx.proxy.ObservableProxy
import groovy.transform.CompileStatic
import org.grails.datastore.mapping.collection.PersistentCollection
import org.grails.datastore.mapping.config.Property
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.grails.datastore.mapping.dirty.checking.DirtyCheckableCollection
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.engine.event.*
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.PropertyMapping
import org.grails.datastore.mapping.model.types.ToMany
import org.grails.datastore.mapping.model.types.ToOne
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.mapping.reflect.EntityReflector
import org.grails.datastore.rx.batch.BatchOperation
import org.grails.datastore.rx.internal.RxDatastoreClientImplementor
import org.grails.datastore.rx.proxy.ProxyFactory
import org.grails.datastore.rx.proxy.RxJavassistProxyFactory
import org.grails.datastore.rx.query.QueryState
import org.grails.datastore.rx.query.RxQuery
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import rx.Observable

import javax.persistence.CascadeType
/**
 * Abstract implementation the {@link RxDatastoreClient} interface
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
abstract class AbstractRxDatastoreClient<T> implements RxDatastoreClient<T>, RxDatastoreClientImplementor {

    protected MappingContext mappingContext
    ApplicationEventPublisher eventPublisher
    final ProxyFactory proxyFactory

    AbstractRxDatastoreClient(MappingContext mappingContext) {
        this.mappingContext = mappingContext
        this.proxyFactory = new RxJavassistProxyFactory()
        mappingContext.setProxyFactory(new RxJavassistProxyFactory())
    }

    MappingContext getMappingContext() {
        return mappingContext
    }

    @Override
    boolean isSchemaless() {
        return false
    }

    @Override
    def <T1> ObservableProxy<T1> proxy(Class<T1> type, Serializable id, QueryState queryState = new QueryState()) {
        (ObservableProxy)proxyFactory.createProxy(this, queryState, type, id)
    }

    @Override
    ObservableProxy proxy(Query query, QueryState queryState = new QueryState()) {
        (ObservableProxy)proxyFactory.createProxy(this, queryState, query)
    }

    @Override
    def <T> Observable get(Class<T> type, Serializable id, QueryState queryState) {
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
            def result = getEntity(entity, type, id, queryState)
            if(result != null) {
                eventPublisher?.publishEvent(new PostLoadEvent(this, entity, mappingContext.createEntityAccess(entity, result)))
            }
            return result
        }
    }
    /**
     * Retrieve and instance of the given type and id
     * @param type The type
     * @param id The id
     * @return An observable
     */
    @Override
    final <T1> Observable<T1> get(Class<T1> type, Serializable id) {
        get(type, id, new QueryState())
    }

    @Override
    final Query createQuery(Class type, QueryState queryState) {
        def entity = mappingContext.getPersistentEntity(type.name)
        if(entity == null) {
            throw new IllegalArgumentException("Type [$type.name] is not a persistent type")
        }

        return createEntityQuery(entity, queryState)
    }

    @Override
    final Query createQuery(Class type) {
        return createQuery(type, new QueryState())
    }

    @Override
    final Observable<Boolean> delete(Object instance) {
        deleteAll((Iterable)Arrays.asList(instance)).map { Number deleteCount ->
            deleteCount > 0
        }
    }

    @Override
    Observable<Number> deleteAll(Iterable instances) {
        def ctx = this.mappingContext
        def proxyHandler = ctx.getProxyHandler()
        if(instances != null) {
            def batchOperation = new BatchOperation()
            List<ApplicationEvent> postEvents = []
            for(o in instances) {
                Class type = proxyHandler.getProxiedClass(o)
                def entity = ctx.getPersistentEntity(type.name)
                if(entity == null) {
                    throw new IllegalArgumentException("Type [$type.name] of instance [$o] is not a persistent type")
                }
                def reflector = mappingContext.getEntityReflector(entity)
                def id = reflector.getIdentifier(o)
                if(id != null) {

                    def ea = ctx.createEntityAccess(entity, o)
                    def preDeleteEvent = new PreDeleteEvent(this, entity, ea)
                    eventPublisher?.publishEvent(preDeleteEvent)
                    if(!preDeleteEvent.isCancelled()) {
                        batchOperation.addDelete(entity, id, o)
                        postEvents.add new PostDeleteEvent(this, entity, ea)
                    }
                }
            }

            return batchDelete(batchOperation).map { Number deleteCount ->
                if(deleteCount > 0) {
                    if(eventPublisher != null) {
                        for(event in postEvents) {
                            eventPublisher.publishEvent(event)
                        }
                    }
                }
                return deleteCount
            }.defaultIfEmpty(0)
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
        persistAll((Iterable<T1>)Arrays.asList(instance)).map { List<Serializable> identifiers ->
            if(!identifiers.isEmpty()) {
                return instance
            }
            return null
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
                        processAssociations(entity, id, o, entityReflector, batchOperation, postEvents)
                        batchOperation.addUpdate(entity, id, o)
                        postEvents.add(new PostUpdateEvent(this, entity, entityAccess))

                    }
                }
                else {
                    id = generateIdentifier(entity, o, entityReflector)
                    def preInsertEvent = new PreInsertEvent(this, entity, entityAccess)
                    eventPublisher?.publishEvent(preInsertEvent)
                    if(!preInsertEvent.isCancelled()) {
                        processAssociations(entity, id, o, entityReflector, batchOperation, postEvents)
                        batchOperation.addInsert(entity,id, o)
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

    void processAssociations(PersistentEntity entity, Serializable id, Object instance, EntityReflector entityReflector, BatchOperation operation, List<ApplicationEvent> postEvents) {
        if(operation.isAlreadyPending(entity, id, instance)) {
            return
        }
        for(association in entity.associations) {
            if(association.doesCascade(CascadeType.PERSIST) && !association.isEmbedded() && !association.isBasic()) {
                def associatedEntity = association.associatedEntity

                if(association instanceof ToOne) {
                    DirtyCheckable associatedObject = (DirtyCheckable )entityReflector.getProperty(instance, association.name)
                    if(associatedObject != null && associatedObject.hasChanged()) {
                        scheduleInsertOrUpdate(associatedEntity, associatedObject, operation, postEvents)
                    }
                }
                else if(association instanceof ToMany) {
                    Iterable collection = (Iterable)entityReflector.getProperty(instance, association.name)
                    if(collection != null) {

                        if(collection instanceof PersistentCollection) {
                            if( !((PersistentCollection)collection).isInitialized() ) {
                                continue
                            }
                        }
                        if(collection instanceof DirtyCheckableCollection) {
                            if( !((DirtyCheckableCollection)collection).hasChanged() ) {
                                continue
                            }
                        }

                        for(obj in collection) {
                            scheduleInsertOrUpdate(associatedEntity, (DirtyCheckable)obj, operation, postEvents)
                        }
                    }
                }
            }
        }
    }

    protected void scheduleInsertOrUpdate(PersistentEntity associatedEntity, DirtyCheckable associatedObject, BatchOperation operation, List<ApplicationEvent> postEvents) {
        EntityReflector associationReflector = mappingContext.getEntityReflector(associatedEntity)
        def associationAccess = mappingContext.createEntityAccess(associatedEntity, associatedObject)
        def associatedId = associationReflector.getIdentifier(associatedObject)
        if (associatedId != null) {
            scheduleUpdate(associatedEntity, associationAccess, associatedId, associatedObject, associationReflector, operation, postEvents)
        } else {
            scheduleInsert(associatedEntity, associatedObject, associationReflector, associationAccess, operation, postEvents)
        }
    }

    protected void scheduleInsert(PersistentEntity associatedEntity, DirtyCheckable associatedObject, EntityReflector associationReflector, EntityAccess associationAccess, BatchOperation operation, List<ApplicationEvent> postEvents) {
        def associatedId
        associatedId = generateIdentifier(associatedEntity, associatedObject, associationReflector)
        def preInsertEvent = new PreInsertEvent(this, associatedEntity, associationAccess)
        eventPublisher?.publishEvent(preInsertEvent)
        if (!preInsertEvent.isCancelled()) {
            operation.addInsert(associatedEntity, associatedId, associatedObject)
            postEvents.add(new PostInsertEvent(this, associatedEntity, associationAccess))
            processAssociations(associatedEntity, associatedId, associatedObject, associationReflector, operation, postEvents)
        }
    }

    protected void scheduleUpdate(PersistentEntity associatedEntity, EntityAccess associationAccess, Serializable associatedId, DirtyCheckable associatedObject, EntityReflector associationReflector, BatchOperation operation, List<ApplicationEvent> postEvents) {
        def preUpdateEvent = new PreUpdateEvent(this, associatedEntity, associationAccess)
        eventPublisher?.publishEvent(preUpdateEvent)
        if (!preUpdateEvent.isCancelled()) {
            operation.addUpdate(associatedEntity, associatedId, associatedObject)
            postEvents.add(new PostUpdateEvent(this, associatedEntity, associationAccess))

            processAssociations(associatedEntity, associatedId, associatedObject, associationReflector, operation, postEvents)
        }
    }

    protected void activeDirtyChecking(object) {
        if (object instanceof DirtyCheckable) {
            def dirtyCheckable = (DirtyCheckable) object
            dirtyCheckable.trackChanges()
        }
    }

    protected boolean isIndexed(PersistentProperty property) {
        PropertyMapping<Property> pm = property.getMapping();
        final Property keyValue = pm.getMappedForm();
        return keyValue != null && keyValue.isIndex();
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
    public <T1> Observable<T1> getEntity(PersistentEntity entity, Class<T1> type, Serializable id, QueryState queryState) {
        def query = createQuery(type, queryState)
        query.idEq(id)
                .max(1)

        return ((RxQuery)query).singleResult()
    }


    /**
     * Creates a query for the given entity
     *
     * @param entity The entity
     *
     * @return The query object
     */
    abstract Query createEntityQuery(PersistentEntity entity, QueryState queryState)
}
