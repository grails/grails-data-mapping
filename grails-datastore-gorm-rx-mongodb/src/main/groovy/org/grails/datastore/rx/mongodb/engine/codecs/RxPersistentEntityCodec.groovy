package org.grails.datastore.rx.mongodb.engine.codecs

import com.mongodb.DBRef
import groovy.transform.CompileStatic
import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.codecs.configuration.CodecRegistry
import org.bson.types.ObjectId
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.grails.datastore.mapping.dirty.checking.DirtyCheckableCollection
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.engine.internal.MappingUtils
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.Embedded
import org.grails.datastore.mapping.model.types.EmbeddedCollection
import org.grails.datastore.mapping.model.types.Identity
import org.grails.datastore.mapping.model.types.ManyToMany
import org.grails.datastore.mapping.model.types.ManyToOne
import org.grails.datastore.mapping.model.types.OneToMany
import org.grails.datastore.mapping.model.types.OneToOne
import org.grails.datastore.mapping.model.types.ToMany
import org.grails.datastore.mapping.model.types.ToOne
import org.grails.datastore.mapping.mongo.AbstractMongoSession
import org.grails.datastore.mapping.mongo.config.MongoAttribute
import org.grails.datastore.mapping.mongo.engine.codecs.PersistentEntityCodec
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.mapping.reflect.FieldEntityAccess
import org.grails.datastore.rx.RxDatastoreClient
import org.grails.datastore.rx.collection.RxPersistentList
import org.grails.datastore.rx.collection.RxPersistentSet
import org.grails.datastore.rx.collection.RxPersistentSortedSet
import org.grails.datastore.rx.internal.RxDatastoreClientImplementor
import org.grails.datastore.rx.mongodb.RxMongoDatastoreClient
import org.grails.datastore.rx.query.QueryState
import org.grails.gorm.rx.api.RxGormEnhancer
import org.grails.gorm.rx.api.RxGormStaticApi

import javax.persistence.FetchType

/**
 * Overrides the default PersistentEntity codecs for associations with reactive implementation for RxGORM
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class RxPersistentEntityCodec extends PersistentEntityCodec {

    private static final Map<Class, PersistentEntityCodec.PropertyEncoder> RX_ENCODERS = [:]
    private static final Map<Class, PersistentEntityCodec.PropertyDecoder> RX_DECODERS = [:]

    static {
        RX_ENCODERS[OneToMany] = new OneToManyEncoder()
        RX_ENCODERS[ManyToMany] = new OneToManyEncoder()
        RX_ENCODERS[Embedded] = new EmbeddedEncoder()
        RX_DECODERS[Embedded] = new EmbeddedDecoder()
        RX_ENCODERS[EmbeddedCollection] = new EmbeddedCollectionEncoder()
        RX_DECODERS[EmbeddedCollection] = new EmbeddedCollectionDecoder()
        RX_DECODERS[OneToOne] = new ToOneDecoder()
        RX_DECODERS[ManyToOne] = new ToOneDecoder()
    }

    final RxDatastoreClient datastoreClient


    private final Map<Class, PersistentEntityCodec.PropertyDecoder> localDecoders = [:]
    private final QueryState queryState


    RxPersistentEntityCodec(PersistentEntity entity, RxMongoDatastoreClient datastoreClient, QueryState queryState = null) {
        super(datastoreClient.codecRegistry, entity, false)
        this.datastoreClient = datastoreClient
        this.queryState = queryState
        if(queryState != null) {
            localDecoders.put(OneToOne, new ToOneDecoder(queryState))
            localDecoders.put(ManyToOne, new ToOneDecoder(queryState))
            localDecoders.put(Identity, new IdentityDecoder(queryState))
        }
    }

    @Override
    Object decode(BsonReader bsonReader, DecoderContext decoderContext) {
        def decoded = super.decode(bsonReader, decoderContext)
        if(decoded instanceof DirtyCheckable) {
            ((DirtyCheckable)decoded).trackChanges()
        }
        return decoded
    }

    @Override
    protected void decodeAssociations(Session mongoSession, EntityAccess access) {
        // session argument will be null, so we read from client

        PersistentEntity persistentEntity = access.persistentEntity
        for(association in persistentEntity.associations) {
            if(association.isBidirectional()) {
                // create inverse lookup collection
                if(association instanceof ToMany) {
                    def foreignKey = (Serializable) access.getIdentifier()

                    access.setPropertyNoConversion(association.name, createConcreteCollection(association, foreignKey))
                }
                else if (association instanceof OneToOne) {
                    if (((ToOne) association).isForeignKeyInChild()) {
                        def associatedClass = association.associatedEntity.javaClass
                        boolean lazy = association.mapping.mappedForm.fetchStrategy == FetchType.LAZY
                        if(lazy) {

                            def proxy = datastoreClient.proxy(
                                    datastoreClient.createQuery(associatedClass)
                                            .eq(association.inverseSide.name, access.identifier)
                            )
                            access.setPropertyNoConversion(association.name, proxy)
                        }
                    }
                }
            }
        }
    }

    protected Collection createConcreteCollection(Association association, Serializable foreignKey) {
        switch(association.type) {
            case SortedSet:
                return new RxPersistentSortedSet(datastoreClient, association, foreignKey, queryState)
            case List:
                return new RxPersistentList(datastoreClient, association, foreignKey, queryState)
            default:
                return new RxPersistentSet(datastoreClient, association, foreignKey, queryState)
        }
    }

    @Override
    protected <T extends PersistentProperty> PersistentEntityCodec.PropertyEncoder<T> getPropertyEncoder(Class<T> type) {
        return RX_ENCODERS.get(type) ?: super.getPropertyEncoder(type)
    }

    @Override
    protected <T extends PersistentProperty> PersistentEntityCodec.PropertyDecoder<T> getPropertyDecoder(Class<T> type) {
        return localDecoders.get(type) ?: RX_DECODERS.get(type) ?: super.getPropertyDecoder(type)
    }

    static class EmbeddedEncoder extends PersistentEntityCodec.EmbeddedEncoder {
        @Override
        protected PersistentEntityCodec createEmbeddedEntityCodec(CodecRegistry codecRegistry, PersistentEntity associatedEntity) {
            return new RxPersistentEntityCodec(associatedEntity, (RxMongoDatastoreClient)RxGormEnhancer.findStaticApi(associatedEntity.javaClass).getDatastoreClient())
        }
    }

    static class EmbeddedDecoder extends PersistentEntityCodec.EmbeddedDecoder {
        @Override
        protected PersistentEntityCodec createEmbeddedEntityCodec(CodecRegistry codecRegistry, PersistentEntity associatedEntity) {
            return new RxPersistentEntityCodec(associatedEntity, (RxMongoDatastoreClient)RxGormEnhancer.findStaticApi(associatedEntity.javaClass).getDatastoreClient())
        }
    }

    static class EmbeddedCollectionEncoder extends PersistentEntityCodec.EmbeddedCollectionEncoder {
        @Override
        protected PersistentEntityCodec createEmbeddedEntityCodec(CodecRegistry codecRegistry, PersistentEntity associatedEntity) {
            return new RxPersistentEntityCodec(associatedEntity, (RxMongoDatastoreClient)RxGormEnhancer.findStaticApi(associatedEntity.javaClass).getDatastoreClient())
        }
    }

    static class EmbeddedCollectionDecoder extends PersistentEntityCodec.EmbeddedCollectionDecoder {
        @Override
        protected PersistentEntityCodec createEmbeddedEntityCodec(CodecRegistry codecRegistry, PersistentEntity associatedEntity) {
            return new RxPersistentEntityCodec(associatedEntity, (RxMongoDatastoreClient)RxGormEnhancer.findStaticApi(associatedEntity.javaClass).getDatastoreClient())
        }
    }

    static class IdentityDecoder implements PersistentEntityCodec.PropertyDecoder<Identity> {
        final QueryState queryState

        IdentityDecoder(QueryState queryState) {
            this.queryState = queryState
        }

        @Override
        void decode(BsonReader bsonReader, Identity property, EntityAccess access, DecoderContext decoderContext, CodecRegistry codecRegistry) {
            def objectId
            switch(property.type) {
                case ObjectId:
                    objectId = bsonReader.readObjectId()
                    break
                case Long:
                    objectId = bsonReader.readInt64()
                    break
                case Integer:
                    objectId = bsonReader.readInt32()
                    break
                default:
                    objectId =  bsonReader.readString()
            }
            access.setIdentifierNoConversion(objectId)
            queryState.addLoadedEntity(access.persistentEntity.javaClass,(Serializable) objectId, access.entity)
        }
    }
    static class ToOneDecoder implements PersistentEntityCodec.PropertyDecoder<ToOne> {

        QueryState queryState

        ToOneDecoder(QueryState queryState) {
            this.queryState = queryState
        }

        ToOneDecoder() {
        }

        @Override
        void decode(BsonReader bsonReader, ToOne property, EntityAccess entityAccess, DecoderContext decoderContext, CodecRegistry codecRegistry) {
            def associatedEntity = property.associatedEntity
            Serializable associationId
            switch(associatedEntity.identity.type) {
                case ObjectId:
                    associationId = bsonReader.readObjectId()
                    break
                case Long:
                    associationId = (Long)bsonReader.readInt64()
                    break
                case Integer:
                    associationId =  (Integer)bsonReader.readInt32()
                    break
                default:
                    associationId = bsonReader.readString()
            }


            def associationType = associatedEntity.javaClass
            def loadedEntity = queryState?.getLoadedEntity(associationType, associationId)
            if(loadedEntity != null) {
                entityAccess.setPropertyNoConversion(property.name, loadedEntity)
            }
            else {
                RxDatastoreClientImplementor datastoreClient = (RxDatastoreClientImplementor)RxGormEnhancer.findStaticApi(associationType).datastoreClient
                entityAccess.setPropertyNoConversion(property.name, datastoreClient.proxy(associationType, associationId, queryState))
            }
        }
    }
    @CompileStatic
    static class OneToManyEncoder implements PersistentEntityCodec.PropertyEncoder<OneToMany> {

        @Override
        void encode(BsonWriter writer, OneToMany property, Object value, EntityAccess parentAccess, EncoderContext encoderContext, CodecRegistry codecRegistry) {
            boolean shouldEncodeIds = !property.isBidirectional() || (property instanceof ManyToMany)
            if(shouldEncodeIds) {
                if(value instanceof Collection) {
                    boolean updateCollection = false
                    if((value instanceof DirtyCheckableCollection)) {
                        def persistentCollection = (DirtyCheckableCollection) value
                        updateCollection = persistentCollection.hasChanged()
                    }
                    else {
                        // write new collection
                        updateCollection = true
                    }

                    if(updateCollection) {
                        // update existing collection
                        Collection identifiers
                        def entityReflector = FieldEntityAccess.getOrIntializeReflector(property.associatedEntity)
                        identifiers = ((Collection)value).collect() {
                            entityReflector.getIdentifier(it)
                        }

                        writer.writeName MappingUtils.getTargetKey((PersistentProperty)property)
                        def listCodec = codecRegistry.get(List)

                        def identifierList = identifiers.toList()
                        listCodec.encode writer, identifierList, encoderContext
                    }
                }
            }
        }
    }
}
