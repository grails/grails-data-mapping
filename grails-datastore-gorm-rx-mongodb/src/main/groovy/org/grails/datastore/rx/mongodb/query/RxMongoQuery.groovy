package org.grails.datastore.rx.mongodb.query

import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.UpdateResult
import com.mongodb.rx.client.AggregateObservable
import com.mongodb.rx.client.FindObservable
import groovy.transform.CompileStatic
import org.bson.Document
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.mongo.MongoConstants
import org.grails.datastore.mapping.mongo.query.MongoQuery
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.rx.mongodb.RxMongoDatastoreClient
import org.grails.datastore.rx.mongodb.internal.CodecRegistryEmbeddedQueryEncoder
import org.grails.datastore.rx.query.RxQuery
import rx.Observable
import rx.Subscriber

/**
 * Reactive query implementation for MongoDB
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class RxMongoQuery extends MongoQuery implements RxQuery {

    final RxMongoDatastoreClient datastoreClient

    RxMongoQuery(RxMongoDatastoreClient client, PersistentEntity entity) {
        super(null, entity)

        datastoreClient = client
    }

    @Override
    protected List executeQuery(PersistentEntity entity, Query.Junction criteria) {
        throw new UnsupportedOperationException("Blocking queries not supported")
    }

    @Override
    Observable findAll() {

        final List<Query.Projection> projectionList = projections().getProjectionList()
        if(projectionList.isEmpty()) {
            FindObservable findObservable = executeQuery()
            return findObservable.toObservable()
        }
        else {
            return executeQuery(projectionList)
        }
    }

    @Override
    Observable singleResult() {
        final List<Query.Projection> projectionList = projections().getProjectionList()
        if(projectionList.isEmpty()) {
            FindObservable findObservable = executeQuery()
            findObservable.limit(1)
            return findObservable.toObservable()
        }
        else {
            return executeQuery(projectionList).first()
        }
    }

    @Override
    Observable<Number> updateAll(Map properties) {
        Document query = prepareQuery()
        def mongoCollection = datastoreClient.getCollection(entity, entity.javaClass)

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
        if(singleResult) {
            return observable.map { Document d ->
                List projectedResults = []
                for(def property in projectedKeys) {
                    Object value = d.get(property.projectionKey);
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
            }.switchIfEmpty(Observable.create({ Subscriber s ->
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

    protected Document prepareQuery() {
        def query = createQueryObject(entity)
        populateMongoQuery(new CodecRegistryEmbeddedQueryEncoder(datastoreClient.codecRegistry), query, criteria, entity)
        query
    }

    protected FindObservable executeQuery() {
        Document query = prepareQuery()

        def clazz = entity.javaClass
        def mongoCollection = datastoreClient.getCollection(entity, clazz)

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

