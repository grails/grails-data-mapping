package grails.gorm.rx.mongodb

import grails.gorm.rx.RxEntity
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.bson.Document
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.grails.datastore.gorm.schemaless.DynamicAttributes
import org.grails.datastore.rx.mongodb.RxMongoDatastoreClient
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
     * The id of the document
     */
    ObjectId id

    /**
     * Creates a criteria builder instance
     */
    static MongoCriteriaBuilder<D> createCriteria() {
        def staticApi = RxGormEnhancer.findStaticApi(this)
        def client = staticApi.datastoreClient
        return new MongoCriteriaBuilder<D>(this, client, client.mappingContext)
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
     * Counts the number of hits
     * @param query The query
     * @return The hit count
     */
    static Observable<Integer> countHits(String query, Map options = Collections.emptyMap()) {
        search(query, options).reduce(0, { Integer i, D d ->
                return ++i
        })
    }
    /**
     * Search for entities using the given query
     *
     * @param query The query
     * @return The results
     */
    static Observable<D> search(String query, Map options = Collections.emptyMap()) {
        def staticApi = RxGormEnhancer.findStaticApi(this)
        RxMongoDatastoreClient client = (RxMongoDatastoreClient)staticApi.datastoreClient
        def persistentEntity = staticApi.entity
        def coll = client.getCollection(persistentEntity, persistentEntity.javaClass)
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

    /**
     * Searches for the top results ordered by the MongoDB score
     *
     * @param query The query
     * @param limit The maximum number of results. Defaults to 5.
     * @return The results
     */
    static Observable<D> searchTop(String query, int limit = 5, Map options = Collections.emptyMap()) {
        def staticApi = RxGormEnhancer.findStaticApi(this)
        RxMongoDatastoreClient client = (RxMongoDatastoreClient)staticApi.datastoreClient
        def persistentEntity = staticApi.entity
        def coll = client.getCollection(persistentEntity, persistentEntity.javaClass)

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
}