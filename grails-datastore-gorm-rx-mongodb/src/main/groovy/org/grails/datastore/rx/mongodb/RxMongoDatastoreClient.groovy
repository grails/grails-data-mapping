package org.grails.datastore.rx.mongodb

import com.mongodb.ServerAddress
import com.mongodb.async.client.MongoClientSettings
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.UpdateResult
import com.mongodb.connection.ClusterSettings
import com.mongodb.rx.client.MongoClient
import com.mongodb.rx.client.MongoClients
import com.mongodb.rx.client.Success
import groovy.transform.CompileStatic
import org.bson.Document
import org.bson.codecs.Codec
import org.bson.codecs.configuration.CodecProvider
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.configuration.CodecRegistry
import org.bson.types.ObjectId
import org.grails.datastore.mapping.config.Property
import org.grails.datastore.mapping.core.IdentityGenerationException
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.mongo.MongoConstants
import org.grails.datastore.mapping.mongo.config.MongoCollection
import org.grails.datastore.mapping.mongo.config.MongoMappingContext
import org.grails.datastore.mapping.mongo.engine.codecs.AdditionalCodecs
import org.grails.datastore.mapping.mongo.engine.codecs.PersistentEntityCodec
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.mapping.reflect.EntityReflector
import org.grails.datastore.rx.AbstractRxDatastoreClient
import org.grails.datastore.rx.RxDatastoreClient
import org.grails.datastore.rx.mongodb.query.RxMongoQuery
import org.grails.gorm.rx.api.RxGormEnhancer
import rx.Observable
import rx.functions.Func1

/**
 * Implementatino of the {@link RxDatastoreClient} inteface for MongoDB that uses the MongoDB RX driver
 *
 * @since 6.0
 * @author Graeme Rocher
 */
@CompileStatic
class RxMongoDatastoreClient extends AbstractRxDatastoreClient<MongoClient> implements CodecProvider {

    final MongoClient mongoClient
    final CodecRegistry codecRegistry
    final Map<String, Codec> entityCodecs = [:]
    final Map<String, String> mongoCollections= [:]
    final Map<String, String> mongoDatabases= [:]
    final String defaultDatabase
    final MongoMappingContext mappingContext

    RxMongoDatastoreClient(MongoMappingContext mappingContext,
                           MongoClientSettings clientSettings = MongoClientSettings.builder().build()) {
        super(mappingContext)

        this.mappingContext = mappingContext
        this.defaultDatabase = mappingContext.defaultDatabaseName
        codecRegistry = CodecRegistries.fromRegistries(
                com.mongodb.async.client.MongoClients.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(new AdditionalCodecs(), this)
        )
        initializeMongoDatastoreClient(mappingContext, codecRegistry)

        def clientSettingsBuilder = MongoClientSettings.builder(clientSettings)
                                                        .codecRegistry(codecRegistry)

        if(clientSettings.getClusterSettings() == null) {
            // default to localhost if no cluster settings specified
            def clusterSettings = ClusterSettings.builder().hosts(Arrays.asList(new ServerAddress("localhost")))
            clientSettingsBuilder
                    .clusterSettings(clusterSettings.build())
        }
        mongoClient = MongoClients.create(clientSettingsBuilder.build())
    }

    @Override
    def <T1> Observable<T1> getEntity(PersistentEntity entity, Class<T1> type, Serializable id) {
        com.mongodb.rx.client.MongoCollection<T1> collection = getCollection(entity, type)

        Document idQuery = createIdQuery(id)
        collection
                .find(idQuery)
                .limit(1)
                .first()
    }

    @Override
    def <T1> Observable<T1> updateEntity(PersistentEntity entity, Class<T1> type, Serializable id, T1 instance, Map<String, Object> arguments) {
        def collection = getCollection(entity, type)
        Document idQuery = createIdQuery(id)
        PersistentEntityCodec codec = (PersistentEntityCodec)codecRegistry.get(type)
        def updateDocument = codec.encodeUpdate(instance)
        collection
                .updateOne(idQuery,updateDocument , new UpdateOptions().upsert(false))
                .map({ UpdateResult result ->
            if(result.wasAcknowledged()) {
                return instance
            }
        } as Func1)
    }

    @Override
    def <T1> Observable<T1> saveEntity(PersistentEntity entity, Class<T1> type, T1 instance, Map<String, Object> arguments) {
        getCollection(entity, type)
                .insertOne(instance)
                .map({ Success success ->
            return instance
        } as Func1)
    }

    @Override
    Observable<Boolean> deleteEntity(PersistentEntity entity, Serializable id, Object instance) {
        def collection = getCollection(entity, entity.javaClass)
        Document idQuery = createIdQuery(id)
        collection.deleteOne(idQuery)
                  .map { DeleteResult result ->
            return result.wasAcknowledged()
        }
    }

    public <T1> com.mongodb.rx.client.MongoCollection<T1> getCollection(PersistentEntity entity, Class<T1> type) {
        com.mongodb.rx.client.MongoCollection<T1> collection = mongoClient
                .getDatabase(getDatabaseName(entity))
                .getCollection(getCollectionName(entity))
                .withCodecRegistry(codecRegistry)
                .withDocumentClass(type)
        collection
    }

    public String getCollectionName(PersistentEntity entity) {
        final String collectionName = mongoCollections.get(entity.getName())
        if(collectionName == null) {
            return entity.getDecapitalizedName()
        }
        return collectionName
    }

    public String getDatabaseName(PersistentEntity entity) {
        final String databaseName = mongoDatabases.get(entity.getName())
        if(databaseName == null) {
            return defaultDatabase
        }
        return databaseName
    }

    @Override
    Query createEntityQuery(PersistentEntity entity) {
        return new RxMongoQuery(this, entity)
    }

    @Override
    MongoClient getNativeInterface() {
        return mongoClient
    }

    @Override
    void close() throws IOException {
        mongoClient?.close()
    }

    @Override
    def <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
        return entityCodecs.get(clazz.name)
    }

    @Override
    Serializable generateIdentifier(PersistentEntity entity, Object instance, EntityReflector reflector) {

        if(!isAssignedId(entity)) {

            def identity = entity.identity
            def type = identity.type
            if(ObjectId.isAssignableFrom(type)) {
                def oid = new ObjectId()
                reflector.setProperty(instance, identity.name, oid)
                return oid
            }
            else if(String.isAssignableFrom(type)) {
                def oid = new ObjectId().toString()
                reflector.setProperty(instance, identity.name, oid)
                return oid
            }
            else {
                throw new IdentityGenerationException("Only String and ObjectId types are supported for the id")
            }
        }
        else {
            throw new IdentityGenerationException("Identifier generation strategy is assigned, but no identifier was supplied")
        }
    }

    protected Document createIdQuery(Serializable id) {
        def idQuery = new Document(MongoConstants.MONGO_ID_FIELD, id)
        idQuery
    }


    protected boolean isAssignedId(PersistentEntity persistentEntity) {
        Property mapping = persistentEntity.identity.mapping.mappedForm
        return MongoConstants.ASSIGNED_IDENTIFIER_MAPPING.equals(mapping?.generator)
    }

    protected void initializeMongoDatastoreClient(MongoMappingContext mappingContext, CodecRegistry codecRegistry) {
        for (entity in mappingContext.persistentEntities) {
            RxGormEnhancer.registerEntity(entity, this)
            String collectionName = entity.decapitalizedName
            String databaseName = defaultDatabase

            MongoCollection collectionMapping = (MongoCollection)entity.getMapping().getMappedForm()

            def coll = collectionMapping.collection
            if(coll != null) {
                collectionName = coll
            }
            def db = collectionMapping.database
            if(db != null) {
                databaseName = db
            }

            def entityName = entity.getName()
            entityCodecs.put(entityName, new PersistentEntityCodec(codecRegistry, entity, false))
            mongoCollections.put(entityName, collectionName)
            mongoCollections.put(entityName, databaseName)
        }
    }
}
