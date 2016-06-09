package grails.gorm.rx.mongodb

import com.mongodb.rx.client.MongoClient
import com.mongodb.rx.client.MongoCollection
import com.mongodb.rx.client.MongoDatabase
import grails.gorm.rx.RxEntity
import grails.gorm.rx.api.RxGormStaticOperations
import grails.gorm.rx.mongodb.api.RxMongoStaticOperations
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.bson.BsonDocument
import org.bson.BsonDocumentReader
import org.bson.BsonDocumentWriter
import org.bson.Document
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.codecs.configuration.CodecRegistry
import org.bson.conversions.Bson
import org.grails.datastore.gorm.schemaless.DynamicAttributes
import org.grails.datastore.rx.mongodb.RxMongoDatastoreClient
import org.grails.datastore.rx.mongodb.api.RxMongoStaticApi
import org.grails.gorm.rx.api.RxGormEnhancer
import rx.Observable
/**
 * Represents a reactive MongoDB document
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
trait RxMongoEntity<D> implements RxEntity<D>, DynamicAttributes {


    /**
     * Converts this entity into a {@link BsonDocument}
     *
     * @return The {@link BsonDocument} instance
     */
    BsonDocument toBsonDocument() {
        def staticApi = RxGormEnhancer.findStaticApi(getClass())
        RxMongoDatastoreClient client = (RxMongoDatastoreClient)staticApi.datastoreClient
        def doc = new BsonDocument()

        Codec<D> codec = client.codecRegistry.get((Class<D>)getClass())
        codec.encode(new BsonDocumentWriter(doc), (D)this, EncoderContext.builder().build())
        return doc
    }

    /**
     * @return The {@MongoDatabase} in use
     */
    static MongoDatabase getDB() {
        RxMongoStaticApi<D> staticApi = (RxMongoStaticApi<D>)RxGormEnhancer.findStaticApi(this)
        return staticApi.getDB()
    }

    /**
     * Convert the given {@link Document} to an instance of this entity
     * @param bson The document
     * @return An instance of this entity
     */
    static D fromBson(Bson bson) {
        def staticApi = RxGormEnhancer.findStaticApi(getClass())
        RxMongoDatastoreClient client = (RxMongoDatastoreClient)staticApi.datastoreClient

        def codecRegistry = client.codecRegistry
        def bsonDocument = bson.toBsonDocument(Document, codecRegistry)
        return (D) codecRegistry.get(this).decode(new BsonDocumentReader(bsonDocument), DecoderContext.builder().build())
    }

    /**
     * @return The {@MongoCollection} in use
     */
    static MongoCollection<D> getCollection() {
        RxMongoStaticApi<D> staticApi = (RxMongoStaticApi<D>)RxGormEnhancer.findStaticApi(this)
        return staticApi.getCollection()
    }

    /**
     * Creates a criteria builder instance
     */
    static MongoCriteriaBuilder<D> createCriteria() {
        RxMongoStaticApi<D> staticApi = (RxMongoStaticApi<D>)RxGormEnhancer.findStaticApi(this)
        return staticApi.createCriteria()
    }

    /**
     * Creates a criteria builder instance
     */
    @CompileDynamic
    static Observable withCriteria(@DelegatesTo(MongoCriteriaBuilder) Closure callable) {
        createCriteria().call(callable)
    }

    /**
     * Creates a criteria builder instance
     */
    @CompileDynamic
    static Observable withCriteria(Map builderArgs, @DelegatesTo(MongoCriteriaBuilder) Closure callable) {
        def criteriaBuilder = createCriteria()
        GroovyObject builderBean = (GroovyObject)criteriaBuilder
        for (entry in builderArgs.entrySet()) {
            String propertyName = entry.key.toString()
            if (builderBean.hasProperty(propertyName)) {
                builderBean.setProperty(propertyName, entry.value)
            }
        }

        if(builderArgs?.uniqueResult) {
            return criteriaBuilder.get(callable)

        }
        else {
            return criteriaBuilder.findAll(callable)
        }
    }

    /**
     * Switches to given client within the context of the closure
     * @param name The client
     * @param callable The closure
     * @return
     */
    static <T> T withClient(MongoClient client, @DelegatesTo(RxMongoStaticOperations) Closure<T> callable ) {
        RxMongoStaticApi<D> staticApi = (RxMongoStaticApi<D>)RxGormEnhancer.findStaticApi(this)
        staticApi.withClient(client, callable)
    }

    /**
     * Switches to the given client for the returned API
     *
     * @param name The client
     * @return The {@link RxMongoStaticOperations} instance
     */
    static RxMongoStaticOperations<D> withClient(MongoClient client) {
        RxMongoStaticApi<D> staticApi = (RxMongoStaticApi<D>)RxGormEnhancer.findStaticApi(this)
        staticApi.withClient(client)
    }

    /**
     * Switches to given database within the context of the closure
     * @param name The name of the database
     * @param callable The closure
     * @return
     */
    static <T> T withDatabase(String name, @DelegatesTo(RxGormStaticOperations) Closure<T> callable ) {
        RxMongoStaticApi<D> staticApi = (RxMongoStaticApi<D>)RxGormEnhancer.findStaticApi(this)
        staticApi.withDatabase(name, callable)
    }

    /**
     * Switches to the given database for the returned API
     *
     * @param name The name of the database
     * @return The {@link RxMongoStaticOperations} instance
     */
    static RxMongoStaticOperations<D> withDatabase(String name) {
        RxMongoStaticApi<D> staticApi = (RxMongoStaticApi<D>)RxGormEnhancer.findStaticApi(this)
        staticApi.withDatabase(name)
    }

    /**
     * Switches to the given collection for the returned API
     *
     * @param name The name of the collection
     * @return The {@link RxMongoStaticOperations} instance
     */
    static RxMongoStaticOperations<D> withCollection(String name) {
        RxMongoStaticApi<D> staticApi = (RxMongoStaticApi<D>)RxGormEnhancer.findStaticApi(this)
        staticApi.withCollection(name)
    }

    /**
     * Switches to given collection within the context of the closure
     * @param name The name of the collection
     * @param callable The closure
     * @return
     */
    static <T> T withCollection(String name, @DelegatesTo(RxMongoStaticOperations) Closure<T> callable ) {
        RxMongoStaticApi<D> staticApi = (RxMongoStaticApi<D>)RxGormEnhancer.findStaticApi(this)
        staticApi.withCollection(name, callable)
    }

    /**
     * Execute a MongoDB aggregation pipeline. Note that the pipeline should return documents that represent this domain class as each return document will be converted to a domain instance in the result set
     *
     * @param pipeline The pipeline
     * @param options The options (optional)
     * @return A mongodb result list
     */
    static Observable<D> aggregate(List pipeline, Map<String,Object> options = Collections.emptyMap()) {
        RxMongoStaticApi<D> staticApi = (RxMongoStaticApi<D>)RxGormEnhancer.findStaticApi(this)
        staticApi.aggregate(pipeline, options)
    }


    /**
     * Counts the number of hits
     * @param query The query
     * @return The hit count
     */
    static Observable<Integer> countHits(String query, Map options = Collections.emptyMap()) {
        RxMongoStaticApi<D> staticApi = (RxMongoStaticApi<D>)RxGormEnhancer.findStaticApi(this)
        staticApi.countHits(query, options)
    }
    /**
     * Search for entities using the given query
     *
     * @param query The query
     * @return The results
     */
    static Observable<D> search(String query, Map options = Collections.emptyMap()) {
        RxMongoStaticApi<D> staticApi = (RxMongoStaticApi<D>)RxGormEnhancer.findStaticApi(this)
        staticApi.search(query, options)
    }

    /**
     * Searches for the top results ordered by the MongoDB score
     *
     * @param query The query
     * @param limit The maximum number of results. Defaults to 5.
     * @return The results
     */
    static Observable<D> searchTop(String query, int limit = 5, Map options = Collections.emptyMap()) {
        RxMongoStaticApi<D> staticApi = (RxMongoStaticApi<D>)RxGormEnhancer.findStaticApi(this)
        staticApi.searchTop(query,limit, options)

    }

}