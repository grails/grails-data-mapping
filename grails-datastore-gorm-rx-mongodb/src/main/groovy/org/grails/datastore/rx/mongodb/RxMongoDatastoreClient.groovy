package org.grails.datastore.rx.mongodb

import com.mongodb.ServerAddress
import com.mongodb.async.client.MongoClientSettings
import com.mongodb.client.model.UpdateOptions
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
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.mongo.MongoConstants
import org.grails.datastore.mapping.mongo.config.MongoCollection
import org.grails.datastore.mapping.mongo.config.MongoMappingContext
import org.grails.datastore.mapping.mongo.engine.codecs.AdditionalCodecs
import org.grails.datastore.mapping.mongo.engine.codecs.PersistentEntityCodec
import org.grails.datastore.mapping.reflect.EntityReflector
import org.grails.datastore.rx.AbstractRxDatastoreClient
import org.grails.datastore.rx.RxDatastoreClient
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
    final String defaultDatabase
    final MongoMappingContext mappingContext

    RxMongoDatastoreClient(MongoMappingContext mappingContext, MongoClientSettings clientSettings = MongoClientSettings.builder().build()) {
        super(mappingContext)

        this.mappingContext = mappingContext
        this.defaultDatabase = mappingContext.defaultDatabaseName
        codecRegistry = CodecRegistries.fromRegistries(
                com.mongodb.async.client.MongoClients.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(new AdditionalCodecs(), this)
        )
        initializeMongoDatastoreClient(mappingContext, codecRegistry)
        def finalClientSettings = MongoClientSettings.builder(clientSettings)
                                        .clusterSettings(ClusterSettings.builder()
                                            .hosts(Arrays.asList(new ServerAddress("localhost")))
                                        .build())
                                        .codecRegistry(codecRegistry)

                                        .build()
        mongoClient = MongoClients.create(finalClientSettings)
    }

    /**
     * Retrieve and instance of the given type and id
     * @param type The type
     * @param id The id
     * @return An observable
     */
    @Override
    def <T> Observable<T> get(Class type, Serializable id) {

        def entity = mappingContext.getPersistentEntity(type.name)
        if(entity == null) {
            throw new IllegalArgumentException("Type [$type.name] is not a persistent type")
        }
        def collection = mongoClient
                .getDatabase(defaultDatabase)
                .getCollection(getCollectionName(entity))

        Document idQuery = createIdQuery(id)
        collection
            .withDocumentClass(type)
            .withCodecRegistry(codecRegistry)
            .find(idQuery)
            .limit(1)
            .first()
    }

    /**
     * Persist an instance
     * @param instance The instance
     *
     * @return An observable
     */
    def <T> Observable<T> persist(Object instance, Map<String, Object> arguments = Collections.<String,Object>emptyMap()) {
        if(instance == null) throw new IllegalArgumentException("Cannot persist null instance")

        Class<T> type = mappingContext.getProxyHandler().getProxiedClass(instance)

        def entity = mappingContext.getPersistentEntity(type.name)
        if(entity == null) {
            throw new IllegalArgumentException("Type [$type.name] is not a persistent type")
        }

        def collection = mongoClient
                .getDatabase(defaultDatabase)
                .getCollection(getCollectionName(entity))

        def reflector = mappingContext.getEntityReflector(entity)
        def identifier = reflector.getIdentifier(instance)
        if(identifier == null) {
            generateIdentifier(entity, instance, reflector)
            collection
                    .withDocumentClass(type)
                    .withCodecRegistry(codecRegistry)
                    .insertOne((T)instance)
                    .map({ Success success ->
                return instance
            } as Func1)
        }
        else {
            // handle update
            if(instance instanceof DirtyCheckable) {
                if( !((DirtyCheckable)instance).hasChanged() ) {
                    return Observable.just((T)instance)
                }
            }
            Document idQuery = createIdQuery(identifier)
            PersistentEntityCodec codec = (PersistentEntityCodec)codecRegistry.get(type)
            def updateDocument = codec.encodeUpdate(instance)
            collection
                    .withDocumentClass(type)
                    .withCodecRegistry(codecRegistry)
                    .updateOne(idQuery,updateDocument , new UpdateOptions().upsert(false))
                    .map({ UpdateResult result ->
                if(result.wasAcknowledged()) {
                    return instance
                }
            } as Func1)

        }
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

    protected Serializable generateIdentifier(PersistentEntity entity, Object instance, EntityReflector reflector) {

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
            entityCodecs.put(entity.getName(), new PersistentEntityCodec(codecRegistry, entity, false))
            mongoCollections.put(entity.getName(), collectionName)
        }
    }

    public String getCollectionName(PersistentEntity entity) {
        final String collectionName = mongoCollections.get(entity.getName())
        if(collectionName == null) {
            return entity.getDecapitalizedName()
        }
        return collectionName
    }
}
