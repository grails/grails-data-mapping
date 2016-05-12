package org.grails.datastore.rx.mongodb.query

import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.UpdateResult
import com.mongodb.rx.client.FindObservable
import grails.gorm.rx.collection.RxUnidirectionalCollection
import grails.gorm.rx.proxy.ObservableProxy
import groovy.transform.CompileStatic
import org.bson.Document
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.grails.datastore.mapping.dirty.checking.DirtyCheckingSupport
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.ManyToMany
import org.grails.datastore.mapping.model.types.ManyToOne
import org.grails.datastore.mapping.model.types.ToMany
import org.grails.datastore.mapping.model.types.ToOne
import org.grails.datastore.mapping.mongo.MongoConstants
import org.grails.datastore.mapping.mongo.engine.MongoEntityPersister
import org.grails.datastore.mapping.mongo.query.MongoQuery
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.mapping.query.event.PreQueryEvent
import org.grails.datastore.mapping.reflect.EntityReflector
import org.grails.datastore.rx.mongodb.RxMongoDatastoreClient
import org.grails.datastore.rx.mongodb.engine.codecs.QueryStateAwareCodeRegistry
import org.grails.datastore.rx.mongodb.internal.CodecRegistryEmbeddedQueryEncoder
import org.grails.datastore.rx.query.QueryState
import org.grails.datastore.rx.query.RxQuery
import org.grails.datastore.rx.query.event.PostQueryEvent
import rx.Observable
import rx.Subscriber
import rx.functions.FuncN

import javax.persistence.FetchType

/**
 * Reactive query implementation for MongoDB
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class RxMongoQuery extends MongoQuery implements RxQuery {

    final RxMongoDatastoreClient datastoreClient
    final QueryState queryState

    RxMongoQuery(RxMongoDatastoreClient client, PersistentEntity entity, QueryState queryState = null) {
        super(null, entity)

        this.datastoreClient = client
        this.queryState = queryState ? queryState : new QueryState()
    }

    @Override
    protected List executeQuery(PersistentEntity entity, Query.Junction criteria) {
        throw new UnsupportedOperationException("Blocking queries not supported")
    }

    @Override
    Observable findAll() {
        firePreQueryEvent()
        final List<Query.Projection> projectionList = projections().getProjectionList()
        Observable observable
        if(projectionList.isEmpty()) {
            FindObservable findObservable = executeQuery()

            observable = findObservable.toObservable()
        }
        else {
            observable = executeQuery(projectionList)
        }

        if(!fetchStrategies.isEmpty()) {

            EntityReflector entityReflector = entity.mappingContext.getEntityReflector(entity)
            List<String> joinedProperties = []
            observable = observable.switchMap { Object o ->

                List<Observable> observables = [Observable.just(o)]
                if(entity.isInstance(o)) {

                    for(fetch in fetchStrategies) {
                        PersistentProperty property = entity.getPropertyByName(fetch.key)
                        FetchType fetchType = fetch.value
                        if(fetchType == FetchType.EAGER) {
                            def propertyName = property.name
                            def currentValue = entityReflector.getProperty(o, propertyName)

                            if(property instanceof ToOne && (currentValue instanceof ObservableProxy)) {
                                ToOne toOne = (ToOne)property
                                if(!toOne.isEmbedded()) {
                                    if(!toOne.isForeignKeyInChild()) {
                                        joinedProperties.add(propertyName)
                                        observables.add datastoreClient.get(toOne.associatedEntity.javaClass, ((ObservableProxy)currentValue).getProxyKey(), queryState)
                                    }
                                    else {
                                        joinedProperties.add(propertyName)
                                        def associationQuery = datastoreClient.createQuery(toOne.associatedEntity.javaClass, queryState)
                                        RxQuery rxQuery = (RxQuery)associationQuery.eq(toOne.inverseSide.name, o)
                                                .max(1)

                                        observables.add rxQuery.singleResult()
                                    }
                                }
                            }
                            else if(property instanceof ToMany) {
                                ToMany toMany = (ToMany)property
                                if(toMany.isBidirectional() && !(toMany instanceof ManyToMany)) {
                                    def inverseSide = toMany.inverseSide
                                    if(inverseSide instanceof ManyToOne) {
                                        joinedProperties.add(propertyName)


                                        RxQuery rxQuery = (RxQuery)datastoreClient.createQuery(inverseSide.owner.javaClass, queryState)
                                                                                   .eq(inverseSide.name, o)

                                        observables.add rxQuery.findAll().toList()
                                    }
                                }
                                else if(currentValue instanceof RxUnidirectionalCollection) {
                                    RxUnidirectionalCollection ruc = (RxUnidirectionalCollection)currentValue
                                    def associationKeys = ruc.associationKeys
                                    joinedProperties.add(propertyName)
                                    if(associationKeys) {
                                        def inverseEntity = toMany.associatedEntity
                                        RxQuery rxQuery = (RxQuery)datastoreClient.createQuery(inverseEntity.javaClass, queryState)
                                                                                   .in(inverseEntity.identity.name, associationKeys)

                                        observables.add rxQuery.findAll().toList()
                                    }
                                    else {
                                        observables.add Observable.just([])
                                    }
                                }
                            }
                        }
                    }
                }

                return Observable.zip(observables, new FuncN() {
                    @Override
                    Object call(Object... args) {
                        return Arrays.asList(args)
                    }
                })
            }.map { List<Object> result ->
                // first result is the entity
                def entity = result.get(0)
                if(result.size() > 1) {
                    int i = 0
                    for(o in result[1..-1]) {
                        String name = joinedProperties.get(i++)
                        if(o instanceof Collection) {
                            def propertyWriter = entityReflector.getPropertyWriter(name)
                            o = o.asType(propertyWriter.propertyType())
                            propertyWriter.write(entity, DirtyCheckingSupport.wrap((Collection)o, (DirtyCheckable)entity, name))
                        }
                        else {
                            entityReflector.setProperty(entity, name, o)
                        }
                    }
                }
                return entity
            }

        }
        return firePostQueryEvent(observable)
    }

    protected Observable firePostQueryEvent(Observable observable) {
        def postQueryEvent = new PostQueryEvent(datastoreClient, this, observable)
        def eventMulticaster = datastoreClient.eventPublisher
        if (eventMulticaster != null) {
            eventMulticaster.publishEvent(postQueryEvent)
        }
        return postQueryEvent.observable
    }

    protected boolean firePreQueryEvent() {
        def eventMulticaster = datastoreClient.eventPublisher
        if (eventMulticaster != null) {
            def event = new PreQueryEvent(datastoreClient, this)
            eventMulticaster.publishEvent(event)
        }
        eventMulticaster
    }

    @Override
    Observable singleResult() {
        if(projections.isEmpty()) {
            max(1)
        }
        return findAll()
    }

    @Override
    Observable<Number> updateAll(Map properties) {
        Document query = prepareQuery()
        def mongoCollection = datastoreClient.getCollection(entity, entity.javaClass)
        mongoCollection = mongoCollection.withCodecRegistry(
                new QueryStateAwareCodeRegistry(datastoreClient.codecRegistry, queryState, datastoreClient)
        )
        def options = new UpdateOptions()
        mongoCollection.updateMany(query, new Document(MongoConstants.SET_OPERATOR, properties), options.upsert(false))
                .map({ UpdateResult result ->
            if(result.wasAcknowledged()) {
                return result.modifiedCount
            }
            else {
                return 0
            }
        }).defaultIfEmpty(0L)
    }

    @Override
    Observable<Number> deleteAll() {
        Document query = prepareQuery()
        def mongoCollection = datastoreClient.getCollection(entity, entity.javaClass)
        mongoCollection.deleteMany(query)
                       .map({ DeleteResult result ->
            if(result.wasAcknowledged()) {
                return result.deletedCount
            }
            else {
                return 0L
            }
        }).defaultIfEmpty(0L)
    }

    protected Observable executeQuery(List<Query.Projection> projectionList) {
        Document query = prepareQuery()
        def aggregatePipeline = buildAggregatePipeline(entity, query, projectionList)
        List<Document> aggregationPipeline = aggregatePipeline.getAggregationPipeline()
        boolean singleResult = aggregatePipeline.isSingleResult()
        List<MongoQuery.ProjectedProperty> projectedKeys = aggregatePipeline.getProjectedKeys()


        def mongoCollection = datastoreClient.getCollection(entity, entity.javaClass)
        def aggregateObservable = mongoCollection.aggregate(aggregationPipeline)
        def observable = aggregateObservable.toObservable()
        observable = observable.map { Document d ->
            List projectedResults = []
            for(def property in projectedKeys) {
                Object value = getProjectedValue(d, property.projectionKey);
                if(value != null) {
                    projectedResults.add(value)
                }
                else {
                    if (property.projection instanceof Query.CountProjection) {
                        projectedResults.add(0)
                    }
                }
            }
            if(projectedResults.size() == 1) {
                return projectedResults[0]
            }
            else {
                return projectedResults
            }
        }

        if(singleResult) {
            return observable.switchIfEmpty(Observable.create({ Subscriber s ->
                for(def property in projectedKeys) {
                    if (property.projection instanceof Query.CountProjection) {
                        s.onNext(0)
                    }
                }

                s.onCompleted()
            } as Observable.OnSubscribe))
        }
        else {
            return observable
        }
    }

    private Object getProjectedValue(Document dbo, String projectionKey) {
        Object value;
        if (projectionKey.startsWith("id.")) {
            projectionKey = projectionKey.substring(3)
            Document id = (Document) dbo.get(MongoEntityPersister.MONGO_ID_FIELD)
            value = id.get(projectionKey)
        } else {
            value = dbo.get(projectionKey)
        }
        return value;
    }
    protected Document prepareQuery() {
        def query = createQueryObject(entity)
        populateMongoQuery(new CodecRegistryEmbeddedQueryEncoder(datastoreClient), query, criteria, entity)
        return query
    }

    protected FindObservable executeQuery() {
        Document query = prepareQuery()

        def clazz = entity.javaClass
        def mongoCollection = datastoreClient.getCollection(entity, clazz)
        mongoCollection = mongoCollection.withCodecRegistry(
                new QueryStateAwareCodeRegistry(datastoreClient.codecRegistry, queryState, datastoreClient)
        )
        def findObservable = mongoCollection.find(query, clazz)
        if(max > -1) {
            findObservable.limit(max)
        }
        if(offset > 0) {
            findObservable.skip(offset)
        }

        if (!orderBy.isEmpty()) {
            def orderObject = new Document()
            for (Query.Order order in orderBy) {
                String property = order.property
                property = getPropertyName(entity, property)
                orderObject.put(property, order.getDirection() == Query.Order.Direction.DESC ? -1 : 1)
            }
            findObservable.sort orderObject
        }
        return findObservable
    }
}

