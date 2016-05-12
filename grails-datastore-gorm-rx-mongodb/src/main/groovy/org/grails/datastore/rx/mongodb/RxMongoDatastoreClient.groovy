package org.grails.datastore.rx.mongodb

import com.mongodb.ServerAddress
import com.mongodb.async.client.MongoClientSettings
import com.mongodb.bulk.BulkWriteResult
import com.mongodb.client.model.BulkWriteOptions
import com.mongodb.client.model.IndexOptions
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
import org.grails.datastore.mapping.model.ClassMapping
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.PropertyMapping
import org.grails.datastore.mapping.model.types.BasicTypeConverterRegistrar
import org.grails.datastore.mapping.mongo.MongoConstants
import org.grails.datastore.mapping.mongo.config.MongoAttribute
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
import org.grails.datastore.rx.mongodb.extensions.MongoExtensions
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
    private static final String INDEX_ATTRIBUTES = "indexAttributes"

    final MongoClient mongoClient
    final CodecRegistry codecRegistry
    final Map<String, Codec> entityCodecs = [:]
    final Map<String, String> mongoCollections= [:]
    final Map<String, String> mongoDatabases= [:]
    final String defaultDatabase
    final MongoMappingContext mappingContext

    /**
     * Creates a new RxMongoDatastoreClient for the given mapping context and {@link MongoClient}
     *
     * @param mongoClient The mongo client
     * @param mappingContext The mapping context
     */
    RxMongoDatastoreClient(MongoClient mongoClient, MongoMappingContext mappingContext) {
        super(mappingContext)
        this.mongoClient = mongoClient
        this.defaultDatabase = mappingContext.defaultDatabaseName
        this.mappingContext = mappingContext
        this.codecRegistry = createCodeRegistry()

        initialize(mappingContext)
    }

    /**
     * Creates a new RxMongoDatastoreClient for the given database name, classes and {@link MongoClient}
     *
     * @param mongoClient The mongo client
     * @param databaseName The default database name
     * @param classes The classes which must implement {@link grails.gorm.rx.mongodb.RxMongoEntity}
     */
    RxMongoDatastoreClient(MongoClient mongoClient, String databaseName, Class...classes) {
        super(new MongoMappingContext(databaseName))
        this.mongoClient = mongoClient
        this.defaultDatabase = mappingContext.defaultDatabaseName
        this.mappingContext = initializeMappingContext(classes)
        this.codecRegistry = createCodeRegistry()
        initialize(mappingContext)
    }

    /**
     * Creates a new RxMongoDatastoreClient for the given database name and classes using the default Mongo configuration
     *
     * @param mongoClient The mongo client
     * @param databaseName The default database name
     * @param classes The classes which must implement {@link grails.gorm.rx.mongodb.RxMongoEntity}
     */
    RxMongoDatastoreClient(String databaseName, Class...classes) {
        super(new MongoMappingContext(databaseName))
        this.defaultDatabase = mappingContext.defaultDatabaseName
        this.codecRegistry = createCodeRegistry()
        this.mongoClient = initializeMongoClient(MongoClientSettings.builder().build())
        this.mappingContext = initializeMappingContext(classes)
        initialize(mappingContext)
    }

    RxMongoDatastoreClient(MongoMappingContext mappingContext,
                           MongoClientSettings clientSettings = MongoClientSettings.builder().build()) {
        super(mappingContext)
        this.mappingContext = mappingContext
        this.defaultDatabase = mappingContext.defaultDatabaseName
        this.codecRegistry = createCodeRegistry()
        this.mongoClient = initializeMongoClient(clientSettings)
        initialize(mappingContext)
    }

    protected MongoMappingContext initializeMappingContext(Class... classes) {
        MongoMappingContext mongoMappingContext = (MongoMappingContext) super.mappingContext
        mongoMappingContext.addPersistentEntities(classes)
        mongoMappingContext.initialize()
        return mongoMappingContext;
    }

    protected MongoClient initializeMongoClient(MongoClientSettings clientSettings) {
        def clientSettingsBuilder = MongoClientSettings.builder(clientSettings)
                .codecRegistry(codecRegistry)

        if (clientSettings.getClusterSettings() == null) {
            // default to localhost if no cluster settings specified
            def clusterSettings = ClusterSettings.builder().hosts(Arrays.asList(new ServerAddress("localhost")))
            clientSettingsBuilder
                    .clusterSettings(clusterSettings.build())
        }
        return MongoClients.create(clientSettingsBuilder.build())
    }

    /**
     * Rebuilds the MongoDB index, useful if the database is dropped
     */
    void rebuildIndex() {
        def entities = mappingContext.getPersistentEntities()
        for(entity in entities) {
            initializeMongoIndex(entity)
        }
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
            List<WriteModel> entityWriteModels = entry.value

            if(!entityWriteModels.isEmpty()) {

                PersistentEntity entity = entry.key
                def mongoCollection = getCollection(entity, entity.javaClass)

                def writeOptions = new BulkWriteOptions()

                observables.add mongoCollection.bulkWrite(entityWriteModels, writeOptions)
            }
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
            initializeMongoIndex(entity)
        }
    }

    protected void initializeMongoIndex(PersistentEntity entity) {

        def collection = getCollection(entity, entity.getJavaClass())
        final ClassMapping<MongoCollection> classMapping = entity.getMapping()
        if (classMapping != null) {
            final MongoCollection mappedForm = classMapping.mappedForm
            if (mappedForm != null) {
                List<MongoCollection.Index> indices = mappedForm.indices
                for (MongoCollection.Index index in indices) {
                    final Map<String, Object> options = index.options
                    final IndexOptions indexOptions = MongoExtensions.mapToObject(IndexOptions, options)
                    collection.createIndex(new Document(index.getDefinition()), indexOptions).toBlocking().first()
                }

                for (Map compoundIndex in mappedForm.getCompoundIndices()) {

                    Map indexAttributes = null;
                    if (compoundIndex.containsKey(INDEX_ATTRIBUTES)) {
                        Object o = compoundIndex.remove(INDEX_ATTRIBUTES)
                        if (o instanceof Map) {
                            indexAttributes = (Map) o
                        }
                    }
                    Document indexDef = new Document(compoundIndex)
                    if (indexAttributes != null) {
                        final IndexOptions indexOptions = MongoExtensions.mapToObject(IndexOptions, indexAttributes)
                        collection.createIndex(indexDef, indexOptions).toBlocking().first()
                    } else {
                        collection.createIndex(indexDef).toBlocking().first()
                    }
                }
            }
        }

        for (PersistentProperty<MongoAttribute> property in entity.getPersistentProperties()) {
            final boolean indexed = isIndexed(property)

            if (indexed) {
                final MongoAttribute mongoAttributeMapping = property.mapping.mappedForm
                def dbObject = new Document()
                final String fieldName = getMongoFieldNameForProperty(property)
                dbObject.put(fieldName, 1)
                def options = new Document()
                if (mongoAttributeMapping != null) {
                    Map attributes = mongoAttributeMapping.indexAttributes
                    if (attributes != null) {
                        attributes = new HashMap(attributes)
                        if (attributes.containsKey(MongoAttribute.INDEX_TYPE)) {
                            dbObject.put(fieldName, attributes.remove(MongoAttribute.INDEX_TYPE))
                        }
                        options.putAll(attributes)
                    }
                }
                // continue using deprecated method to support older versions of MongoDB
                if (options.isEmpty()) {
                    collection.createIndex(dbObject).toBlocking().first()
                } else {
                    final IndexOptions indexOptions = MongoExtensions.mapToObject(IndexOptions, options)
                    collection.createIndex(dbObject, indexOptions).toBlocking().first()
                }
            }
        }
    }

    String getMongoFieldNameForProperty(PersistentProperty<MongoAttribute> property) {
        PropertyMapping<MongoAttribute> pm = property.getMapping();
        String propKey = null;
        if (pm.getMappedForm() != null) {
            propKey = pm.getMappedForm().getField();
        }
        if (propKey == null) {
            propKey = property.getName();
        }
        return propKey;
    }
}
