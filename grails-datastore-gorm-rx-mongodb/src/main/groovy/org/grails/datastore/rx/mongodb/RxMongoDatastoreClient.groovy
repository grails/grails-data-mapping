package org.grails.datastore.rx.mongodb

import com.mongodb.ServerAddress
import com.mongodb.async.client.MongoClientSettings
import com.mongodb.bulk.BulkWriteResult
import com.mongodb.client.model.BulkWriteOptions
import com.mongodb.client.model.InsertOneModel
import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.WriteModel
import com.mongodb.client.result.DeleteResult
import com.mongodb.connection.ClusterSettings
import com.mongodb.rx.client.MongoClient
import com.mongodb.rx.client.MongoClients
import groovy.transform.CompileStatic
import org.bson.Document
import org.bson.codecs.Codec
import org.bson.codecs.configuration.CodecProvider
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.configuration.CodecRegistry
import org.bson.types.Binary
import org.bson.types.ObjectId
import org.grails.datastore.gorm.mongo.MongoGormEnhancer
import org.grails.datastore.mapping.config.Property
import org.grails.datastore.mapping.core.IdentityGenerationException
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.types.BasicTypeConverterRegistrar
import org.grails.datastore.mapping.mongo.MongoConstants
import org.grails.datastore.mapping.mongo.config.MongoCollection
import org.grails.datastore.mapping.mongo.config.MongoMappingContext
import org.grails.datastore.mapping.mongo.engine.codecs.AdditionalCodecs
import org.grails.datastore.mapping.mongo.engine.codecs.PersistentEntityCodec
import org.grails.datastore.mapping.mongo.query.MongoQuery
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.mapping.reflect.EntityReflector
import org.grails.datastore.rx.AbstractRxDatastoreClient
import org.grails.datastore.rx.RxDatastoreClient
import org.grails.datastore.rx.batch.BatchOperation
import org.grails.datastore.rx.mongodb.engine.codecs.RxPersistentEntityCodec
import org.grails.datastore.rx.mongodb.query.RxMongoQuery
import org.grails.datastore.rx.query.QueryState
import org.grails.gorm.rx.api.RxGormEnhancer
import org.springframework.core.convert.converter.Converter
import org.springframework.core.convert.converter.ConverterRegistry
import rx.Observable

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

    RxMongoDatastoreClient(MongoMappingContext mappingContext, MongoClient mongoClient) {
        super(mappingContext)
        this.mongoClient = mongoClient
        this.defaultDatabase = mappingContext.defaultDatabaseName
        this.mappingContext = mappingContext
        this.codecRegistry = createCodeRegistry()

        initialize(mappingContext)
    }

    RxMongoDatastoreClient(MongoClient mongoClient, String databaseName, Class...classes) {
        super(new MongoMappingContext(databaseName))
        this.mongoClient = mongoClient
        this.defaultDatabase = mappingContext.defaultDatabaseName
        this.mappingContext = (MongoMappingContext)super.mappingContext
        this.mappingContext.addPersistentEntities(classes)
        this.mappingContext.initialize()
        this.codecRegistry = createCodeRegistry()
        initialize(mappingContext)
    }

    RxMongoDatastoreClient(MongoMappingContext mappingContext,
                           MongoClientSettings clientSettings = MongoClientSettings.builder().build()) {
        super(mappingContext)

        this.mappingContext = mappingContext
        this.defaultDatabase = mappingContext.defaultDatabaseName
        this.codecRegistry = createCodeRegistry()
        def clientSettingsBuilder = MongoClientSettings.builder(clientSettings)
                                                        .codecRegistry(codecRegistry)

        if(clientSettings.getClusterSettings() == null) {
            // default to localhost if no cluster settings specified
            def clusterSettings = ClusterSettings.builder().hosts(Arrays.asList(new ServerAddress("localhost")))
            clientSettingsBuilder
                    .clusterSettings(clusterSettings.build())
        }
        mongoClient = MongoClients.create(clientSettingsBuilder.build())
        initialize(mappingContext)
    }

    protected CodecRegistry createCodeRegistry() {
        CodecRegistries.fromRegistries(
                com.mongodb.async.client.MongoClients.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(new AdditionalCodecs(), this)
        )
    }

    protected void initialize(MongoMappingContext mappingContext) {

        initializeMongoDatastoreClient(mappingContext, codecRegistry)
        initializeConverters(mappingContext);
        MongoGormEnhancer.registerMongoMethodExpressions()
    }


    protected void initializeConverters(MappingContext mappingContext) {
        final ConverterRegistry conversionService = mappingContext.getConverterRegistry();
        BasicTypeConverterRegistrar registrar = new BasicTypeConverterRegistrar();
        registrar.register(conversionService);

        final ConverterRegistry converterRegistry = mappingContext.getConverterRegistry();
        converterRegistry.addConverter(new Converter<String, ObjectId>() {
            public ObjectId convert(String source) {
                return new ObjectId(source);
            }
        });

        converterRegistry.addConverter(new Converter<ObjectId, String>() {
            public String convert(ObjectId source) {
                return source.toString();
            }
        });

        converterRegistry.addConverter(new Converter<byte[], Binary>() {
            public Binary convert(byte[] source) {
                return new Binary(source);
            }
        });

        converterRegistry.addConverter(new Converter<Binary, byte[]>() {
            public byte[] convert(Binary source) {
                return source.getData();
            }
        });

        for (Converter converter : AdditionalCodecs.getBsonConverters()) {
            converterRegistry.addConverter(converter);
        }
    }
    @Override
    boolean isSchemaless() {
        return true
    }

    @Override
    Observable<Number> batchWrite(BatchOperation operation) {
        def inserts = operation.inserts
        Map<PersistentEntity, List<WriteModel>> writeModels = [:].withDefault { [] }

        for(entry in inserts) {

            PersistentEntity entity = entry.key
            List<WriteModel> entityWriteModels = writeModels.get(entity)
            for(op in entry.value) {
                BatchOperation.EntityOperation entityOperation = op.value

                def object = entityOperation.object
                entityWriteModels.add(new InsertOneModel(object))

                activeDirtyChecking(object)
            }
        }

        def updates = operation.updates
        for(entry in updates) {
            PersistentEntity entity = entry.key
            List<WriteModel> entityWriteModels = writeModels.get(entity)
            final PersistentEntityCodec codec = (PersistentEntityCodec)codecRegistry.get(entity.javaClass)
            final updateOptions = new UpdateOptions().upsert(false)

            for(op in entry.value) {
                BatchOperation.EntityOperation entityOperation = op.value
                Document idQuery = createIdQuery(entityOperation.identity)

                def object = entityOperation.object
                Document updateDocument = codec.encodeUpdate(object)

                if(!updateDocument.isEmpty()) {
                    entityWriteModels.add(new UpdateOneModel(idQuery, updateDocument, updateOptions))
                }

                activeDirtyChecking(object)
            }
        }

        List<Observable> observables = []
        for(entry in writeModels) {
            PersistentEntity entity = entry.key
            def mongoCollection = getCollection(entity, entity.javaClass)

            def writeOptions = new BulkWriteOptions()

            observables.add mongoCollection.bulkWrite(entry.value, writeOptions)
        }

        return Observable.concatEager(observables)
                            .reduce(0L, { Long count, BulkWriteResult bwr ->
            if(bwr.wasAcknowledged()) {
                count += bwr.insertedCount
                count += bwr.modifiedCount
                count += bwr.deletedCount
            }
            return count
        })
    }

    @Override
    Observable<Number> batchDelete(BatchOperation operation) {
        Map<PersistentEntity, Map<Serializable, BatchOperation.EntityOperation>> deletes = operation.deletes
        List<Observable> observables = []
        for(entry in deletes) {

            PersistentEntity entity = entry.key
            def mongoCollection = getCollection(entity, entity.javaClass)
            def entityOperations = entry.value.values()

            def inQuery = new Document( MongoConstants.MONGO_ID_FIELD, new Document(MongoQuery.MONGO_IN_OPERATOR, entityOperations.collect() { BatchOperation.EntityOperation eo -> eo.identity }) )
            observables.add mongoCollection.deleteMany(inQuery)
        }

        if(observables.isEmpty()) {
            return Observable.just(0L)
        }
        else {
            return Observable.concatEager(observables)
                             .reduce(0L, { Long count, DeleteResult dr ->
                if(dr.wasAcknowledged()) {
                    count += dr.deletedCount
                }
                return count
            })
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
    Query createEntityQuery(PersistentEntity entity, QueryState queryState) {
        return new RxMongoQuery(this, entity, queryState)
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
            entityCodecs.put(entityName, new RxPersistentEntityCodec(entity, this))
            mongoCollections.put(entityName, collectionName)
            mongoDatabases.put(entityName, databaseName)
        }
    }
}
