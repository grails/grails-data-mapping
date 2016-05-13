package grails.gorm.rx.mongodb.api

import com.mongodb.rx.client.MongoClient
import com.mongodb.rx.client.MongoCollection
import com.mongodb.rx.client.MongoDatabase
import grails.gorm.rx.api.RxGormStaticOperations
import grails.gorm.rx.mongodb.MongoCriteriaBuilder
import rx.Observable

/**
 *
 * Static methods for interfacing with MongoDB
 *
 * @author Graeme Rocher
 * @since 6.0
 */
interface RxMongoStaticOperations<D> extends RxGormStaticOperations<D> {

    /**
     * @return The {@MongoDatabase} in use
     */
    MongoDatabase getDB()

    /**
     * @return The {@MongoCollection} in use
     */
    MongoCollection<D> getCollection()
    /**
     * Creates a criteria builder instance
     */
    MongoCriteriaBuilder<D> createCriteria()

    /**
     * Creates a criteria builder instance and executes the given closure against the instance
     */
    Observable withCriteria(@DelegatesTo(MongoCriteriaBuilder) Closure callable)

    /**
     * Creates a criteria builder instance and executes the given closure against the instance
     */
    Observable withCriteria(Map builderArgs, @DelegatesTo(MongoCriteriaBuilder) Closure callable)

    /**
     * Switches to given client within the context of the closure
     * @param name The client
     * @param callable The closure
     * @return
     */
    public <T> T withClient(MongoClient client, @DelegatesTo(RxMongoStaticOperations) Closure<T> callable )

    /**
     * Switches to the given client for the returned API
     *
     * @param name The client
     * @return The {@link RxMongoStaticOperations} instance
     */
    RxMongoStaticOperations<D> withClient(MongoClient client)

    /**
     * Switches to given database within the context of the closure
     * @param name The name of the database
     * @param callable The closure
     * @return
     */
    public <T> T withDatabase(String name, @DelegatesTo(RxMongoStaticOperations) Closure<T> callable )

    /**
     * Switches to the given database for the returned API
     *
     * @param name The name of the database
     * @return The {@link RxMongoStaticOperations} instance
     */
    RxMongoStaticOperations<D> withDatabase(String name)

    /**
     * Switches to the given collection for the returned API
     *
     * @param name The name of the collection
     * @return The {@link RxMongoStaticOperations} instance
     */
    RxMongoStaticOperations<D> withCollection(String name)

    /**
     * Switches to given collection within the context of the closure
     * @param name The name of the collection
     * @param callable The closure
     * @return
     */
    public <T> T withCollection(String name, @DelegatesTo(RxMongoStaticOperations) Closure<T> callable )

    /**
     * Counts the number of hits
     * @param query The query
     * @return The hit count
     */
    Observable<Integer> countHits(String query, Map options)

    /**
     * Counts the number of hits
     * @param query The query
     * @return The hit count
     */
    Observable<Integer> countHits(String query)

    /**
     * Search for entities using the given query
     *
     * @param query The query
     * @return The results
     */
    Observable<D> search(String query, Map options)
    /**
     * Search for entities using the given query
     *
     * @param query The query
     * @return The results
     */
    Observable<D> search(String query)

    /**
     * Searches for the top results ordered by the MongoDB score
     *
     * @param query The query
     * @return The results
     */
    Observable<D> searchTop(String query)

    /**
     * Searches for the top results ordered by the MongoDB score
     *
     * @param query The query
     * @param limit The maximum number of results. Defaults to 5.
     * @return The results
     */
    Observable<D> searchTop(String query, int limit)

    /**
     * Searches for the top results ordered by the MongoDB score
     *
     * @param query The query
     * @param limit The maximum number of results. Defaults to 5.
     * @return The results
     */
    Observable<D> searchTop(String query, int limit, Map options)


    /**
     * Execute a MongoDB aggregation pipeline. Note that the pipeline should return documents that represent this domain class as each return document will be converted to a domain instance in the result set
     *
     * @param pipeline The pipeline
     * @return A mongodb result list
     */
    Observable<D> aggregate(List pipeline)

    /**
     * Execute a MongoDB aggregation pipeline. Note that the pipeline should return documents that represent this domain class as each return document will be converted to a domain instance in the result set
     *
     * @param pipeline The pipeline
     * @param options The options (optional)
     * @return A mongodb result list
     */
    Observable<D> aggregate(List pipeline, Map<String,Object> options )
}