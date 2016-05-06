package org.grails.datastore.rx

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.engine.event.PostInsertEvent
import org.grails.datastore.mapping.engine.event.PostLoadEvent
import org.grails.datastore.mapping.engine.event.PreInsertEvent
import org.grails.datastore.mapping.engine.event.PreLoadEvent
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.mapping.reflect.EntityReflector
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


    /**
     * Persist an instance
     * @param instance The instance
     *
     * @return An observable
     */
    @Override
    final <T1> Observable<T1> persist(T1 instance, Map<String, Object> arguments) {

        if(instance == null) throw new IllegalArgumentException("Cannot persist null instance")

        Class<T1> type = mappingContext.getProxyHandler().getProxiedClass(instance)

        def entity = mappingContext.getPersistentEntity(type.name)
        if(entity == null) {
            throw new IllegalArgumentException("Type [$type.name] is not a persistent type")
        }

        def reflector = mappingContext.getEntityReflector(entity)
        def identifier = reflector.getIdentifier(instance)
        if(identifier == null) {
            generateIdentifier(entity, instance, reflector)

            def ea = mappingContext.createEntityAccess(entity, instance)
            def preInsertEvent = new PreInsertEvent(this, entity, ea)

            eventPublisher?.publishEvent(preInsertEvent)

            if(!preInsertEvent.isCancelled()) {
                def observable = saveEntity(entity, type, instance, arguments)
                if(eventPublisher != null) {
                    def rxClient = this

                    observable.subscribe(new Subscriber() {
                        @Override
                        void onCompleted() {
                            eventPublisher.publishEvent(new PostInsertEvent(rxClient, entity, ea))
                        }

                        @Override
                        void onError(Throwable e) {

                        }

                        @Override
                        void onNext(Object o) {

                        }
                    })
                }
                return observable
            }
            else {
                return Observable.just(null)
            }
        }
        else {
            // handle update
            if(instance instanceof DirtyCheckable) {
                if( !((DirtyCheckable)instance).hasChanged() ) {
                    return Observable.just(instance)
                }
            }
            return updateEntity(entity, type, identifier, instance, arguments)
        }
    }

    @Override
    final <T1> Observable<T1> persist(T1 instance) {
        return persist(instance, Collections.<String,Object>emptyMap())
    }

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
