package org.grails.datastore.rx.mongodb.api

import com.mongodb.ReadPreference
import com.mongodb.rx.client.AggregateObservable
import com.mongodb.rx.client.MongoClient
import com.mongodb.rx.client.MongoCollection
import com.mongodb.rx.client.MongoDatabase
import grails.gorm.rx.CriteriaBuilder
import grails.gorm.rx.mongodb.MongoCriteriaBuilder
import grails.gorm.rx.mongodb.api.RxMongoStaticOperations
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.bson.Document
import org.bson.conversions.Bson
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.rx.mongodb.RxMongoDatastoreClient
import org.grails.datastore.rx.mongodb.client.DelegatingRxMongoDatastoreClient
import org.grails.gorm.rx.api.RxGormStaticApi
import rx.Observable

/**
 * Subclasses {@link RxMongoStaticApi} and provides additional functionality specific to MongoDB
 *
 * @param <D> The type of the domain class
 * @author Graeme Rocher
 */
@CompileStatic
class RxMongoStaticApi<D> extends RxGormStaticApi<D> implements RxMongoStaticOperations<D> {
    final RxMongoDatastoreClient mongoDatastoreClient

    RxMongoStaticApi(PersistentEntity entity, RxMongoDatastoreClient datastoreClient) {
        super(entity, datastoreClient)
        this.mongoDatastoreClient = datastoreClient
    }

    @Override
    MongoDatabase getDB() {
        return mongoDatastoreClient.nativeInterface.getDatabase( mongoDatastoreClient.getDatabaseName(entity) )
    }

    @Override
    MongoCollection<D> getCollection() {
        return mongoDatastoreClient.getCollection(entity, entity.javaClass)
    }

    @Override
    RxMongoStaticOperations<D> withCollection(String name) {
        def delegatingClient = new DelegatingRxMongoDatastoreClient(mongoDatastoreClient)
        delegatingClient.targetCollectionName = name
        def delegateApi = new RxMongoStaticApi<D>(entity, delegatingClient)
        return delegateApi
    }

    @Override
    def <T> T withClient(MongoClient client, @DelegatesTo(RxMongoStaticOperations) Closure<T> callable) {
        def delegatingClient = new DelegatingRxMongoDatastoreClient(mongoDatastoreClient)
        delegatingClient.targetMongoClient = client
        def delegateApi = new RxMongoStaticApi<D>(entity, delegatingClient)
        callable.setResolveStrategy(Closure.DELEGATE_FIRST)
        callable.setDelegate(delegateApi)
        return callable.call()
    }

    @Override
    RxMongoStaticOperations<D> withClient(MongoClient client) {
        def delegatingClient = new DelegatingRxMongoDatastoreClient(mongoDatastoreClient)
        delegatingClient.targetMongoClient = client
        def delegateApi = new RxMongoStaticApi<D>(entity, delegatingClient)
        return delegateApi
    }

    @Override
    def <T> T withCollection(String name, @DelegatesTo(RxMongoStaticOperations) Closure<T> callable) {
        def delegatingClient = new DelegatingRxMongoDatastoreClient(mongoDatastoreClient)
        delegatingClient.targetCollectionName = name
        def delegateApi = new RxMongoStaticApi<D>(entity, delegatingClient)
        callable.setResolveStrategy(Closure.DELEGATE_FIRST)
        callable.setDelegate(delegateApi)
        return callable.call()
    }

    @Override
    def <T> T withDatabase(String name, @DelegatesTo(RxMongoStaticOperations) Closure<T> callable) {
        def delegatingClient = new DelegatingRxMongoDatastoreClient(mongoDatastoreClient)
        delegatingClient.targetDatabaseName = name
        def delegateApi = new RxMongoStaticApi<D>(entity, delegatingClient)
        callable.setResolveStrategy(Closure.DELEGATE_FIRST)
        callable.setDelegate(delegateApi)
        return callable.call()
    }

    @Override
    RxMongoStaticOperations<D> withDatabase(String name) {
        def delegatingClient = new DelegatingRxMongoDatastoreClient(mongoDatastoreClient)
        delegatingClient.targetDatabaseName = name
        def delegateApi = new RxMongoStaticApi<D>(entity, delegatingClient)
        return delegateApi
    }

    @Override
    MongoCriteriaBuilder<D> createCriteria() {
        return new MongoCriteriaBuilder<D>(entity.javaClass, mongoDatastoreClient, mongoDatastoreClient.mappingContext)
    }

    @Override
    Observable<Integer> countHits(String query, Map options = Collections.emptyMap()) {
        search(query, options).reduce(0, { Integer i, D d ->
            return ++i
        })
    }

    @Override
    Observable<D> search(String query, Map options = Collections.emptyMap()) {
        def coll = mongoDatastoreClient.getCollection(entity, entity.javaClass)
        def searchArgs = ['$search': query]
        if(options.language) {
            searchArgs['$language'] = options.language.toString()
        }
        def findObservable = coll.find((Bson)new Document('$text',searchArgs))
        int offset = options.offset instanceof Number ? ((Number)options.offset).intValue() : 0
        int max = options.max instanceof Number ? ((Number)options.max).intValue() : -1
        if(offset > 0) findObservable.skip(offset)
        if(max > -1) findObservable.limit(max)
        findObservable.toObservable()
    }


    @Override
    Observable<D> searchTop(String query, int limit = 5, Map options = Collections.emptyMap()) {
        def coll = mongoDatastoreClient.getCollection(entity, entity.javaClass)

        def searchArgs = ['$search': query]
        if(options.language) {
            searchArgs['$language'] = options.language.toString()
        }

        def score = new Document('score', ['$meta': 'textScore'])
        def findObservable = coll.find((Bson)new Document('$text', searchArgs))
                .projection((Bson)score)
                .sort((Bson)score)
                .limit(limit)

        findObservable.toObservable()
    }

    @Override
    Observable<D> aggregate(List pipeline, Map<String, Object> options = Collections.emptyMap()) {
        def mongoCollection = mongoDatastoreClient.getCollection(entity, entity.javaClass)

        if(options.readPreference != null) {
            mongoCollection = mongoCollection.withReadPreference(ReadPreference.valueOf(options.readPreference.toString()))
        }
        List<Document> newPipeline = cleanPipeline(pipeline)
        AggregateObservable aggregateObservable = mongoCollection.aggregate(newPipeline)
        for(opt in options.keySet()) {
            if(aggregateObservable.respondsTo(opt)) {
                setOption((Object)aggregateObservable, opt, options)
            }
        }

        return (Observable<D>)aggregateObservable.toObservable()
    }

    @CompileDynamic
    private static void setOption(Object target, String opt, Map options) {
        target."$opt"(options.get(opt))
    }

    private static List<Document> cleanPipeline(List pipeline) {
        List<Document> newPipeline = new ArrayList<Document>()
        for (o in pipeline) {
            if (o instanceof Document) {
                newPipeline << (Document)o
            } else if (o instanceof Map) {
                newPipeline << new Document((Map) o)
            }
        }
        newPipeline
    }
}
