/*
 * Copyright 2015 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.datastore.mapping.mongo.engine.codecs

import com.mongodb.DBRef
import groovy.transform.CompileStatic
import org.bson.*
import org.bson.codecs.BsonValueCodecProvider
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.codecs.configuration.CodecRegistry
import org.bson.conversions.Bson
import org.bson.types.Binary
import org.bson.types.ObjectId
import org.grails.datastore.mapping.collection.PersistentList
import org.grails.datastore.mapping.collection.PersistentSet
import org.grails.datastore.mapping.collection.PersistentSortedSet
import org.grails.datastore.mapping.config.Property
import org.grails.datastore.mapping.core.DatastoreException
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.grails.datastore.mapping.dirty.checking.DirtyCheckableCollection
import org.grails.datastore.mapping.dirty.checking.DirtyCheckingMap
import org.grails.datastore.mapping.dirty.checking.DirtyCheckingSupport
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.engine.EntityPersister
import org.grails.datastore.mapping.engine.Persister
import org.grails.datastore.mapping.engine.internal.MappingUtils
import org.grails.datastore.mapping.engine.types.CustomTypeMarshaller
import org.grails.datastore.mapping.model.EmbeddedPersistentEntity
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.PropertyMapping
import org.grails.datastore.mapping.model.config.GormProperties
import org.grails.datastore.mapping.model.types.*
import org.grails.datastore.mapping.mongo.AbstractMongoSession
import org.grails.datastore.mapping.mongo.MongoDatastore
import org.grails.datastore.mapping.mongo.config.MongoAttribute
import org.grails.datastore.mapping.mongo.engine.MongoCodecEntityPersister
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.mapping.reflect.FieldEntityAccess
import org.springframework.core.convert.converter.Converter

import javax.persistence.CascadeType
import javax.persistence.FetchType
/**
 * A MongoDB codec for persisting {@link PersistentEntity} instances
 *
 * @author Graeme Rocher
 * @since 4.1
 */
@CompileStatic
class PersistentEntityCodec implements Codec {
    private static final Map<Class, PropertyEncoder> ENCODERS = [:]
    private static final Map<Class, PropertyDecoder> DECODERS = [:]
    private static final String BLANK_STRING = ""
    public static final String MONGO_SET_OPERATOR = '$set'
    public static final String MONGO_UNSET_OPERATOR = '$unset'
    public static final EncoderContext DEFAULT_ENCODER_CONTEXT = EncoderContext.builder().build()
    public static final String DB_REF_ID_FIELD = '$id'
    public static final String SCHEMALESS_ATTRIBUTES = "schemaless.attributes"

    static {
        ENCODERS[Identity] = new IdentityEncoder()
        DECODERS[Identity] = new IdentityDecoder()
        ENCODERS[Simple] = new SimpleEncoder()
        DECODERS[Simple] = new SimpleDecoder()
        ENCODERS[Embedded] = new EmbeddedEncoder()
        DECODERS[Embedded] = new EmbeddedDecoder()
        ENCODERS[EmbeddedCollection] = new EmbeddedCollectionEncoder()
        DECODERS[EmbeddedCollection] = new EmbeddedCollectionDecoder()
        ENCODERS[OneToOne] = new ToOneEncoder()
        DECODERS[OneToOne] = new ToOneDecoder()
        ENCODERS[ManyToOne] = new ToOneEncoder()
        DECODERS[ManyToOne] = new ToOneDecoder()
        ENCODERS[OneToMany] = new OneToManyEncoder()
        DECODERS[OneToMany] = new OneToManyDecoder()
        ENCODERS[ManyToMany] = new OneToManyEncoder()
        DECODERS[ManyToMany] = new OneToManyDecoder()
        ENCODERS[Custom] = new CustomTypeEncoder()
        DECODERS[Custom] = new CustomTypeDecoder()
        ENCODERS[Basic] = new BasicCollectionTypeEncoder()
        DECODERS[Basic] = new BasicCollectionTypeDecoder()
    }

    final MongoDatastore datastore
    final MappingContext mappingContext
    final PersistentEntity entity
    final CodecRegistry codecRegistry

    PersistentEntityCodec(MongoDatastore datastore, PersistentEntity entity) {
        this.datastore = datastore
        this.mappingContext = datastore.mappingContext
        this.codecRegistry = datastore.codecRegistry
        this.entity = entity
    }

    @Override
    Object decode(BsonReader bsonReader, DecoderContext decoderContext) {
        bsonReader.readStartDocument()
        def persistentEntity = entity
        def instance = persistentEntity.javaClass.newInstance()
        AbstractMongoSession mongoSession = (AbstractMongoSession)datastore.currentSession
        EntityAccess access = createEntityAccess(mongoSession, persistentEntity, instance)
        Document schemalessAttributes = null
        BsonType bsonType = bsonReader.readBsonType()
        boolean abortReading = false
        while(bsonType != BsonType.END_OF_DOCUMENT) {

            def name = bsonReader.readName()
            if(!abortReading) {

                if(MongoCodecEntityPersister.MONGO_CLASS_FIELD == name) {
                    def childEntity = mappingContext
                            .getChildEntityByDiscriminator(persistentEntity.rootEntity, bsonReader.readString())
                    if(childEntity != null) {
                        persistentEntity = childEntity
                        instance = childEntity
                                .newInstance()
                        def newAccess = createEntityAccess(childEntity, instance)
                        newAccess.setIdentifierNoConversion( access.identifier )
                        access = newAccess
                    }
                    bsonType = bsonReader.readBsonType()
                    continue
                }

                if(MongoCodecEntityPersister.MONGO_ID_FIELD == name) {
                    DECODERS[Identity].decode( bsonReader, persistentEntity.identity, access, decoderContext, datastore)
                    if(mongoSession.contains(instance)) {
                        instance = mongoSession.retrieve( persistentEntity.javaClass, (Serializable)access.identifier )
                        abortReading = true
                    }
                }
                else {
                    def property = persistentEntity.getPropertyByName(name)
                    if(property && bsonType != BsonType.NULL) {
                        def propKind = property.getClass().superclass
                        switch(property.type) {
                            case CharSequence:
                                access.setPropertyNoConversion(property.name, bsonReader.readString())
                                break
                            default:
                                DECODERS[propKind]?.decode(bsonReader, property, access, decoderContext, datastore)
                        }

                    }
                    else if(!abortReading) {
                        if(schemalessAttributes == null) {
                            schemalessAttributes = new Document()
                        }
                        readSchemaless(bsonReader, schemalessAttributes, name, decoderContext)
                    }
                    else {
                        bsonReader.skipValue()
                    }

                }
            }
            else if(!abortReading){
                if(schemalessAttributes == null) {
                    schemalessAttributes = new Document()
                }
                readSchemaless(bsonReader, schemalessAttributes, name, decoderContext)
            }
            else {
                bsonReader.skipValue()
            }
            bsonType = bsonReader.readBsonType()
        }
        bsonReader.readEndDocument()

        def session = mongoSession
        for( association in entity.associations ) {
            if(association instanceof OneToMany) {
                if(association.isBidirectional()) {
                    OneToManyDecoder.initializePersistentCollection(session, access, association)
                }
            }
            else if(association instanceof OneToOne) {
                if(((ToOne)association).isForeignKeyInChild()) {
                    def associatedClass = association.associatedEntity.javaClass
                    Query query = session.createQuery(associatedClass)
                    query.eq(association.inverseSide.name, access.identifier)
                            .projections().id()

                    def id = query.singleResult()
                    boolean lazy = association.mapping.mappedForm.fetchStrategy == FetchType.LAZY
                    access.setPropertyNoConversion(
                            association.name,
                            lazy ? session.proxy( associatedClass, (Serializable) id) : session.retrieve( associatedClass, (Serializable) id)
                    )

                }
            }
        }
        if(schemalessAttributes != null) {
            mongoSession.setAttribute(instance, SCHEMALESS_ATTRIBUTES, schemalessAttributes)
        }
        return instance

    }

    protected void readSchemaless(BsonReader bsonReader, Document schemalessAttributes, String name, DecoderContext decoderContext) {
        def currentBsonType = bsonReader.getCurrentBsonType()
        def targetClass = BsonValueCodecProvider.getClassForBsonType(currentBsonType)

        def codec = codecRegistry.get(targetClass)

        BsonValue bsonValue = (BsonValue)codec.decode(bsonReader, decoderContext)
        if(bsonValue != null) {

            def converter = AdditionalCodecs.getBsonConverter(bsonValue.getClass())
            schemalessAttributes.put(
                    name,
                    converter != null ? converter.convert( bsonValue ) : bsonValue
            )
        }
    }

    protected EntityAccess createEntityAccess(Object instance) {
        def entity = mappingContext.getPersistentEntity(instance.getClass().name)
        return createEntityAccess(entity, instance)
    }

    protected EntityAccess createEntityAccess(PersistentEntity entity, instance) {
        AbstractMongoSession session = (AbstractMongoSession) getDatastore().currentSession
        return createEntityAccess(session, entity, instance)
    }

    protected EntityAccess createEntityAccess(AbstractMongoSession session, PersistentEntity entity, instance) {
        return session.createEntityAccess(entity, instance)
    }

    @Override
    void encode(BsonWriter writer, Object value, EncoderContext encoderContext) {
        boolean includeIdentifier = true

        encode(writer, value, encoderContext, includeIdentifier)
    }

    /**
     * This method will encode an update for the given object based
     * @param value A {@link Bson} that is the update object
     * @return A Bson
     */
    Document encodeUpdate(Object value, EntityAccess access = createEntityAccess(value), EncoderContext encoderContext = DEFAULT_ENCODER_CONTEXT) {
        Document update = new Document()
        def entity = access.persistentEntity

        def proxyFactory = mappingContext.proxyFactory
        if( proxyFactory.isProxy(value) ) {
            value = proxyFactory.unwrap(value)
        }
        if(value instanceof DirtyCheckable) {
            def sets = new BsonDocument()
            def unsets = new Document()
            def writer = new BsonDocumentWriter(sets)
            writer.writeStartDocument()
            DirtyCheckable dirty = (DirtyCheckable)value
            Set<String> processed = []

            def dirtyProperties = dirty.listDirtyPropertyNames()
            boolean isNew = dirtyProperties.isEmpty() && dirty.hasChanged()
            def isVersioned = entity.isVersioned()
            if(isNew) {
                // if it is new it can only be an embedded entity that has now been updated
                // so we get all properties
                dirtyProperties = entity.persistentPropertyNames
                if(!entity.isRoot()) {
                    sets.put(MongoCodecEntityPersister.MONGO_CLASS_FIELD, new BsonString(entity.discriminator))
                }

                if(isVersioned) {
                    EntityPersister.incrementEntityVersion(access)
                }

            }


            def mongoDatastore = datastore
            for(propertyName in dirtyProperties) {
                def prop = entity.getPropertyByName(propertyName)
                if(prop != null) {

                    processed << propertyName
                    Object v = access.getProperty(prop.name)
                    if (v != null) {
                        if(prop instanceof Embedded) {
                            encodeEmbeddedUpdate(sets, (Association)prop, v)
                        }
                        else if(prop instanceof EmbeddedCollection) {
                            encodeEmbeddedCollectionUpdate(access, sets, (Association)prop, v)
                        }
                        else {
                            def propKind = prop.getClass().superclass
                            ENCODERS.get(propKind)?.encode(writer, prop, v, access, encoderContext, mongoDatastore)
                        }

                    }
                    else if(!isNew) {
                        unsets[prop.name] = BLANK_STRING
                    }
                }
            }

            Document schemaless = (Document)datastore.currentSession.getAttribute(value, SCHEMALESS_ATTRIBUTES)
            if(schemaless != null) {
                for(name in schemaless.keySet()) {
                    def v = schemaless.get(name)
                    if(v == null) {
                        unsets.put(name,BLANK_STRING)
                    }
                    else {
                        writer.writeName(name)
                        def codec = codecRegistry.get(v.getClass())
                        codec.encode(writer, v, encoderContext)
                    }
                }
            }

            for(association in entity.associations) {
                if(processed.contains( association.name )) continue
                if(association instanceof OneToMany) {
                    def v = access.getProperty(association.name)
                    if (v != null) {
                        // TODO: handle unprocessed association
                    }
                }
                else if(association instanceof ToOne) {
                    def v = access.getProperty(association.name)
                    if( v instanceof DirtyCheckable ) {
                        if(((DirtyCheckable)v).hasChanged()) {
                            if(association instanceof Embedded) {
                                encodeEmbeddedUpdate(sets, association, v)
                            }
                        }
                    }
                }
                else if(association instanceof EmbeddedCollection) {
                    def v = access.getProperty(association.name)
                    if( v instanceof DirtyCheckableCollection ) {
                        if(((DirtyCheckableCollection)v).hasChanged()) {
                            encodeEmbeddedCollectionUpdate(access, sets, association, v)
                        }
                    }
                }
            }


            boolean hasSets = !sets.isEmpty()
            boolean hasUnsets = !unsets.isEmpty()

            if(hasSets && isVersioned) {
                def version = entity.version
                def propKind = version.getClass().superclass
                MongoCodecEntityPersister.incrementEntityVersion(access)
                def v = access.getProperty(version.name)
                ENCODERS.get(propKind)?.encode(writer, version, v, access, encoderContext, mongoDatastore)
            }

            writer.writeEndDocument()

            if(hasSets) {
                update.put(MONGO_SET_OPERATOR, sets)
            }
            if(hasUnsets) {
                update.put(MONGO_UNSET_OPERATOR,unsets)
            }
        }
        else {
            // TODO: Support non-dirty checkable objects?
        }

        return update
    }

    protected void encodeEmbeddedCollectionUpdate(EntityAccess parentAccess, BsonDocument sets, Association association, v) {
        if(v instanceof Collection) {
            if((v instanceof DirtyCheckableCollection) && !((DirtyCheckableCollection)v).hasChangedSize()) {
                int i = 0
                for(o in v) {
                    def embeddedUpdate = encodeUpdate(o)
                    def embeddedSets = embeddedUpdate.get(MONGO_SET_OPERATOR)
                    if(embeddedSets) {

                        def map = (Map) embeddedSets
                        for (key in map.keySet()) {
                            sets.put("${association.name}.${i}.$key", (BsonValue) map.get(key))
                        }
                    }
                    i++
                }
            }
            else {
                // if this is not a dirty checkable collection or the collection has changed size then a whole new collection has been
                // set so we overwrite existing
                def associatedEntity = association.associatedEntity
                def rootClass = associatedEntity.javaClass
                def mongoDatastore = this.datastore
                def entityCodec = mongoDatastore.getPersistentEntityCodec(rootClass)
                def inverseProperty = association.inverseSide
                List<BsonValue> documents =[]
                for(o in v) {
                    if(o == null) {
                        documents << null
                        continue
                    }
                    PersistentEntity entity = associatedEntity
                    PersistentEntityCodec codec = entityCodec

                    def cls = o.getClass()
                    if(rootClass != cls) {
                        // a subclass, so lookup correct codec
                        entity = mongoDatastore.mappingContext.getPersistentEntity(cls.name)
                        if(entity == null) {
                            throw new DatastoreException("Value [$o] is not a valid type for association [$association]" )
                        }
                        codec = mongoDatastore.getPersistentEntityCodec(cls)
                    }
                    def ea = createEntityAccess(entity, o)
                    if(inverseProperty != null) {
                        if(inverseProperty instanceof ToOne) {
                            ea.setPropertyNoConversion( inverseProperty.name, parentAccess.entity)
                        }

                    }
                    def doc = new BsonDocument()
                    def id = ea.identifier
                    codec.encode( new BsonDocumentWriter(doc), o, DEFAULT_ENCODER_CONTEXT, id != null )
                    documents.add( doc )
                }
                def bsonArray = new BsonArray(documents)
                sets.put( association.name, bsonArray)
            }
        }
        else {
            // TODO: Map handling
        }

    }
    protected void encodeEmbeddedUpdate(BsonDocument sets, Association association, v) {
        def embeddedUpdate = encodeUpdate(v)
        def embeddedSets = embeddedUpdate.get(MONGO_SET_OPERATOR)
        if(embeddedSets) {

            def map = (Map) embeddedSets
            for (key in map.keySet()) {
                sets.put("${association.name}.$key", (BsonValue) map.get(key))
            }
        }
    }

    void encode(BsonWriter writer, value, EncoderContext encoderContext, boolean includeIdentifier) {
        writer.writeStartDocument()
        def access = createEntityAccess(value)
        def entity = access.persistentEntity

        if(!entity.isRoot()) {
            def discriminator = entity.discriminator
            writer.writeName(MongoCodecEntityPersister.MONGO_CLASS_FIELD)
            writer.writeString(discriminator)
        }

        def mongoDatastore = datastore
        if (includeIdentifier) {

            def id = access.getIdentifier()
            ENCODERS.get(Identity).encode writer, entity.identity, id, access, encoderContext, mongoDatastore
        }



        for (PersistentProperty prop in entity.persistentProperties) {
            def propKind = prop.getClass().superclass
            Object v = access.getProperty(prop.name)
            if (v != null) {
                ENCODERS.get(propKind)?.encode(writer, (PersistentProperty) prop, v, access, encoderContext, mongoDatastore)
            }
        }

        AbstractMongoSession mongoSession = (AbstractMongoSession)datastore.currentSession

        Document schemaless = (Document)mongoSession.getAttribute(access.entity, SCHEMALESS_ATTRIBUTES)
        if(schemaless != null) {
            for(name in schemaless.keySet()) {
                writer.writeName name
                Object v = schemaless.get(name)
                def codec = codecRegistry.get(v.getClass())
                codec.encode(writer, v, encoderContext)
            }
        }

        writer.writeEndDocument()
        writer.flush()
    }

    @Override
    Class getEncoderClass() {
        entity.javaClass
    }

    /**
     * An interface for encoding PersistentProperty instances
     */
    static interface PropertyEncoder<T extends PersistentProperty> {
        void encode(BsonWriter writer, T property, Object value, EntityAccess parentAccess, EncoderContext encoderContext, MongoDatastore datastore)
    }

    /**
     * An interface for encoding PersistentProperty instances
     */
    static interface PropertyDecoder<T extends PersistentProperty> {
        void decode(BsonReader reader, T property, EntityAccess entityAccess, DecoderContext decoderContext, MongoDatastore datastore)
    }

    /**
     * A {@PropertyDecoder} capable of decoding the {@link Identity}
     */
    static class IdentityDecoder implements PropertyDecoder<Identity> {

        @Override
        void decode(BsonReader bsonReader, Identity property, EntityAccess access, DecoderContext decoderContext, MongoDatastore datastore) {
            switch(property.type) {
                case ObjectId:
                    access.setIdentifierNoConversion( bsonReader.readObjectId() )
                    break
                case Long:
                    access.setIdentifierNoConversion( bsonReader.readInt64() )
                    break
                case Integer:
                    access.setIdentifierNoConversion( bsonReader.readInt32() )
                    break
                default:
                    access.setIdentifierNoConversion( bsonReader.readString())
            }

        }
    }
    /**
     * A {@PropertyEncoder} capable of encoding the {@link Identity}
     */
    static class IdentityEncoder implements PropertyEncoder<Identity> {

        @Override
        void encode(BsonWriter writer, Identity property, Object id, EntityAccess parentAccess, EncoderContext encoderContext, MongoDatastore datastore) {
            writer.writeName(MongoCodecEntityPersister.MONGO_ID_FIELD)

            if (id instanceof ObjectId) {
                writer.writeObjectId(id)
            } else if (id instanceof Number) {
                writer.writeInt64(((Number) id).toLong())
            } else {
                writer.writeString(id.toString())
            }

        }
    }
    /**
     * A {@PropertyDecoder} capable of decoding {@link Simple} properties
     */
    static class SimpleDecoder implements PropertyDecoder<Simple> {
        public static final Map<Class, TypeDecoder> SIMPLE_TYPE_DECODERS
        public static final TypeDecoder DEFAULT_DECODER = new TypeDecoder() {
            @Override
            void decode(BsonReader reader, Simple property, EntityAccess entityAccess) {
                entityAccess.setProperty( property.name, reader.readString())
            }
        }
        static interface TypeDecoder {
            void decode(BsonReader reader, Simple property, EntityAccess entityAccess)
        }

        static {
            SIMPLE_TYPE_DECODERS = new HashMap<Class, TypeDecoder>().withDefault { Class ->
                DEFAULT_DECODER
            }

             def convertingIntReader = new TypeDecoder() {
                @Override
                void decode(BsonReader reader, Simple property, EntityAccess entityAccess) {
                    entityAccess.setProperty( property.name, reader.readInt32() )
                }
            }
            SIMPLE_TYPE_DECODERS[Short] = convertingIntReader
            SIMPLE_TYPE_DECODERS[short.class] = convertingIntReader
            SIMPLE_TYPE_DECODERS[Byte] = convertingIntReader
            SIMPLE_TYPE_DECODERS[byte.class] = convertingIntReader
            def intDecoder = new TypeDecoder() {
                @Override
                void decode(BsonReader reader, Simple property, EntityAccess entityAccess) {
                    entityAccess.setPropertyNoConversion( property.name, reader.readInt32() )
                }
            }
            SIMPLE_TYPE_DECODERS[Integer] = intDecoder
            SIMPLE_TYPE_DECODERS[int.class] = intDecoder
            def longDecoder = new TypeDecoder() {
                @Override
                void decode(BsonReader reader, Simple property, EntityAccess entityAccess) {
                    entityAccess.setPropertyNoConversion( property.name, reader.readInt64() )
                }
            }
            SIMPLE_TYPE_DECODERS[Long] = longDecoder
            SIMPLE_TYPE_DECODERS[long.class] = longDecoder
            def doubleDecoder = new TypeDecoder() {
                @Override
                void decode(BsonReader reader, Simple property, EntityAccess entityAccess) {
                    entityAccess.setPropertyNoConversion( property.name, reader.readDouble() )
                }
            }
            SIMPLE_TYPE_DECODERS[Double] = doubleDecoder
            SIMPLE_TYPE_DECODERS[double.class] = doubleDecoder
            def booleanDecoder = new TypeDecoder() {
                @Override
                void decode(BsonReader reader, Simple property, EntityAccess entityAccess) {
                    entityAccess.setPropertyNoConversion( property.name, reader.readBoolean() )
                }
            }
            SIMPLE_TYPE_DECODERS[Boolean] = booleanDecoder
            SIMPLE_TYPE_DECODERS[boolean.class] = booleanDecoder

            SIMPLE_TYPE_DECODERS[([] as byte[]).getClass()] = new TypeDecoder() {
                @Override
                void decode(BsonReader reader, Simple property, EntityAccess entityAccess) {
                    def binary = reader.readBinaryData()
                    entityAccess.setPropertyNoConversion( property.name, binary.data )
                }
            }
            SIMPLE_TYPE_DECODERS[Date] = new TypeDecoder() {
                @Override
                void decode(BsonReader reader, Simple property, EntityAccess entityAccess) {
                    def time = reader.readDateTime()
                    entityAccess.setPropertyNoConversion( property.name, new Date(time))
                }
            }
            SIMPLE_TYPE_DECODERS[Calendar] = new TypeDecoder() {
                @Override
                void decode(BsonReader reader, Simple property, EntityAccess entityAccess) {
                    def time = reader.readDateTime()
                    def calendar = new GregorianCalendar()
                    calendar.setTimeInMillis(time)
                    entityAccess.setPropertyNoConversion( property.name, calendar)
                }
            }

            SIMPLE_TYPE_DECODERS[Binary] = new TypeDecoder() {
                @Override
                void decode(BsonReader reader, Simple property, EntityAccess entityAccess) {

                    entityAccess.setPropertyNoConversion(
                            property.name,
                            new Binary(reader.readBinaryData().data)
                    )
                }
            }
            SIMPLE_TYPE_DECODERS[ObjectId] = new TypeDecoder() {
                @Override
                void decode(BsonReader reader, Simple property, EntityAccess entityAccess) {

                    entityAccess.setPropertyNoConversion(
                            property.name,
                            reader.readObjectId()
                    )
                }
            }
        }

        @Override
        void decode(BsonReader reader, Simple property, EntityAccess entityAccess, DecoderContext decoderContext, MongoDatastore datastore) {
            def type = property.type
            def decoder = SIMPLE_TYPE_DECODERS[type]
            if(type.isArray()) {
                if(!decoder.is(DEFAULT_DECODER)) {
                    decoder.decode reader, property, entityAccess
                }
                else {
                    def arrayDecoder = datastore.codecRegistry.get(List)
                    def bsonArray = arrayDecoder.decode(reader, decoderContext)
                    entityAccess.setProperty(property.name, bsonArray)
                }
            }
            else {
                decoder.decode reader, property, entityAccess
            }
        }
    }
    /**
     * An encoder for simple types persistable by MongoDB
     *
     * @author Graeme Rocher
     * @since 4.1
     */
    static class SimpleEncoder implements PropertyEncoder<Simple> {

        static interface TypeEncoder {
            void encode(BsonWriter writer, Simple property, Object value)
        }

        public static final Map<Class, TypeEncoder> SIMPLE_TYPE_ENCODERS
        public static final TypeEncoder DEFAULT_ENCODER = new TypeEncoder() {
            @Override
            void encode(BsonWriter writer, Simple property, Object value) {
                writer.writeString( value.toString() )
            }
        }

        static {


            SIMPLE_TYPE_ENCODERS = new HashMap<Class, TypeEncoder>().withDefault { Class c ->
                DEFAULT_ENCODER
            }

            TypeEncoder smallNumberEncoder = new TypeEncoder() {
                @Override
                void encode(BsonWriter writer, Simple property, Object value) {
                    writer.writeInt32( ((Number)value).intValue() )
                }
            }
            SIMPLE_TYPE_ENCODERS[CharSequence] = DEFAULT_ENCODER
            SIMPLE_TYPE_ENCODERS[String] = DEFAULT_ENCODER
            SIMPLE_TYPE_ENCODERS[StringBuffer] = DEFAULT_ENCODER
            SIMPLE_TYPE_ENCODERS[StringBuilder] = DEFAULT_ENCODER
            SIMPLE_TYPE_ENCODERS[BigInteger] = DEFAULT_ENCODER
            SIMPLE_TYPE_ENCODERS[BigDecimal] = DEFAULT_ENCODER
            SIMPLE_TYPE_ENCODERS[Byte] = smallNumberEncoder
            SIMPLE_TYPE_ENCODERS[byte.class] = smallNumberEncoder
            SIMPLE_TYPE_ENCODERS[Integer] = smallNumberEncoder
            SIMPLE_TYPE_ENCODERS[int.class] = smallNumberEncoder
            SIMPLE_TYPE_ENCODERS[Short] = smallNumberEncoder
            SIMPLE_TYPE_ENCODERS[short.class] = smallNumberEncoder
            TypeEncoder doubleEncoder = new TypeEncoder() {
                @Override
                void encode(BsonWriter writer, Simple property, Object value) {
                    writer.writeDouble( (Double)value )
                }
            }
            SIMPLE_TYPE_ENCODERS[Double] = doubleEncoder
            SIMPLE_TYPE_ENCODERS[double.class] = doubleEncoder
            TypeEncoder longEncoder = new TypeEncoder() {
                @Override
                void encode(BsonWriter writer, Simple property, Object value) {
                    writer.writeInt64( (Long)value )
                }
            }
            SIMPLE_TYPE_ENCODERS[Long] = longEncoder
            SIMPLE_TYPE_ENCODERS[long.class] = longEncoder
            TypeEncoder booleanEncoder = new TypeEncoder() {
                @Override
                void encode(BsonWriter writer, Simple property, Object value) {
                    writer.writeBoolean( (Boolean)value )
                }
            }
            SIMPLE_TYPE_ENCODERS[Boolean] = booleanEncoder
            SIMPLE_TYPE_ENCODERS[boolean.class] = booleanEncoder
            SIMPLE_TYPE_ENCODERS[Calendar] = new TypeEncoder() {
                @Override
                void encode(BsonWriter writer, Simple property, Object value) {
                    writer.writeDateTime( ((Calendar)value).timeInMillis )
                }
            }
            SIMPLE_TYPE_ENCODERS[Date] = new TypeEncoder() {
                @Override
                void encode(BsonWriter writer, Simple property, Object value) {
                    writer.writeDateTime( ((Date)value).time )
                }
            }
            SIMPLE_TYPE_ENCODERS[TimeZone] = new TypeEncoder() {
                @Override
                void encode(BsonWriter writer, Simple property, Object value) {
                    writer.writeString( ((TimeZone)value).ID )
                }
            }
            SIMPLE_TYPE_ENCODERS[([] as byte[]).getClass()] = new TypeEncoder() {
                @Override
                void encode(BsonWriter writer, Simple property, Object value) {
                    writer.writeBinaryData( new BsonBinary((byte[])value))
                }
            }
            SIMPLE_TYPE_ENCODERS[Binary] = new TypeEncoder() {
                @Override
                void encode(BsonWriter writer, Simple property, Object value) {
                    writer.writeBinaryData( new BsonBinary(((Binary)value).data))
                }
            }
            SIMPLE_TYPE_ENCODERS[ObjectId] = new TypeEncoder() {
                @Override
                void encode(BsonWriter writer, Simple property, Object value) {
                    writer.writeObjectId((ObjectId)value)
                }
            }
        }

        @Override
        void encode(BsonWriter writer, Simple property, Object value, EntityAccess parentAccess, EncoderContext encoderContext, MongoDatastore datastore) {
            def type = property.type
            def encoder = SIMPLE_TYPE_ENCODERS[type]
            writer.writeName( MappingUtils.getTargetKey(property) )
            if(type.isArray()) {
                if(!encoder.is(DEFAULT_ENCODER)) {
                    encoder.encode(writer, property, value)
                }
                else {
                    writer.writeStartArray()
                    for( o in value ) {
                        encoder = SIMPLE_TYPE_ENCODERS[type.componentType]
                        encoder.encode(writer, property, o)
                    }
                    writer.writeEndArray()
                }
            }
            else {
                encoder.encode(writer, property, value)
            }
        }
    }

    /**
     * A {@PropertyDecoder} capable of decoding {@Custom} types
     */
    static class CustomTypeDecoder implements PropertyDecoder<Custom> {

        @Override
        void decode(BsonReader reader, Custom property, EntityAccess entityAccess, DecoderContext decoderContext, MongoDatastore datastore) {
            CustomTypeMarshaller marshaller = property.customTypeMarshaller

            decode(datastore, reader, decoderContext, marshaller, property, entityAccess)
        }

        protected static void decode(MongoDatastore datastore, BsonReader reader, DecoderContext decoderContext, CustomTypeMarshaller marshaller, PersistentProperty property, EntityAccess entityAccess) {
            def bsonType = reader.currentBsonType
            def codec = AdditionalCodecs.getCodecForBsonType(bsonType, datastore.codecRegistry)
            if(codec != null) {
                def decoded = codec.decode(reader, decoderContext)
                def value = marshaller.read(property, new Document(
                        MappingUtils.getTargetKey(property),
                        decoded
                ))
                if (value != null) {
                    entityAccess.setProperty(property.name, value)
                }
            }
            else {
                reader.skipValue()
            }
        }
    }

    /**
     * A {@PropertyEncoder} capable of encoding {@Custom} types
     */
    static class CustomTypeEncoder implements PropertyEncoder<Custom> {

        @Override
        void encode(BsonWriter writer, Custom property, Object value, EntityAccess parentAccess, EncoderContext encoderContext, MongoDatastore datastore) {
            def marshaller = property.customTypeMarshaller
            encode(datastore, encoderContext, writer, property, marshaller, value)

        }

        protected static void encode(MongoDatastore datastore, EncoderContext encoderContext, BsonWriter writer, PersistentProperty property, CustomTypeMarshaller marshaller, value) {
            String targetName = MappingUtils.getTargetKey(property)
            def document = new Document()
            marshaller.write(property, value, document)

            Object converted = document.get(targetName)
            if(converted != null) {
                Codec codec = (Codec) datastore.codecRegistry.get(converted.getClass())
                if (codec) {
                    writer.writeName(targetName)
                    codec.encode(writer, converted, encoderContext)
                }

            }
        }
    }

    static class OneToManyDecoder implements PropertyDecoder<Association> {
        @Override
        void decode(BsonReader reader, Association property, EntityAccess entityAccess, DecoderContext decoderContext, MongoDatastore datastore) {
            AbstractMongoSession session = (AbstractMongoSession)datastore.currentSession
            if(property.isBidirectional() && !(property instanceof ManyToMany)) {

                initializePersistentCollection(session, entityAccess, property)
            }
            else {
                def type = property.type
                def propertyName = property.name

                def codecRegistry = datastore.codecRegistry

                def listCodec = codecRegistry.get(List)
                def identifiers = listCodec.decode(reader, decoderContext)
                MongoAttribute attr = (MongoAttribute)property.mapping.mappedForm
                if(attr?.isReference()) {
                    identifiers = identifiers.collect {
                        if(it instanceof DBRef) {
                            return ((DBRef)it).id
                        }
                        else if(it instanceof Map) {
                            return ((Map)it).get(DB_REF_ID_FIELD)
                        }
                        return it
                    }
                }
                def associatedType = property.associatedEntity.javaClass
                if(SortedSet.isAssignableFrom(type)) {
                    entityAccess.setPropertyNoConversion(
                            propertyName,
                            new PersistentSortedSet( identifiers, associatedType, session)
                    )
                }
                else if(Set.isAssignableFrom(type)) {
                    entityAccess.setPropertyNoConversion(
                            propertyName,
                            new PersistentSet( identifiers, associatedType, session)
                    )
                }
                else {
                    entityAccess.setPropertyNoConversion(
                            propertyName,
                            new PersistentList( identifiers, associatedType, session)
                    )
                }
            }
        }

        static initializePersistentCollection(Session session, EntityAccess entityAccess, Association property) {
            def type = property.type
            def propertyName = property.name
            def identifier = (Serializable) entityAccess.identifier

            if(SortedSet.isAssignableFrom(type)) {
                entityAccess.setPropertyNoConversion(
                        propertyName,
                        new PersistentSortedSet( property, identifier, session)
                )
            }
            else if(Set.isAssignableFrom(type)) {
                entityAccess.setPropertyNoConversion(
                        propertyName,
                        new PersistentSet( property, identifier, session)
                )
            }
            else {
                entityAccess.setPropertyNoConversion(
                        propertyName,
                        new PersistentList( property, identifier, session)
                )
            }
        }
    }
    static class OneToManyEncoder implements PropertyEncoder<Association> {

        @Override
        void encode(BsonWriter writer, Association property, Object value, EntityAccess parentAccess, EncoderContext encoderContext, MongoDatastore datastore) {
            boolean shouldEncodeIds = !property.isBidirectional() || (property instanceof ManyToMany)
            if(shouldEncodeIds) {
                // if it is unidirectional we encode the values inside the current
                // document, otherwise nothing to do, encoding foreign key stored in inverse side

                def associatedEntity = property.associatedEntity
                def mongoSession = (AbstractMongoSession)datastore.currentSession
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
                        Collection identifiers = (Collection)mongoSession.getAttribute(parentAccess.entity, "${property}.ids")
                        if(identifiers == null) {
                            def fastClassData = FieldEntityAccess.getOrIntializeReflector(associatedEntity)
                            identifiers = ((Collection)value).collect() {
                                fastClassData.getIdentifier(it)
                            }
                        }
                        writer.writeName MappingUtils.getTargetKey((PersistentProperty)property)
                        def listCodec = datastore.codecRegistry.get(List)

                        def identifierList = identifiers.toList()
                        MongoAttribute attr = (MongoAttribute)property.mapping.mappedForm
                        if(attr?.isReference()) {
                            def collectionName = mongoSession.getCollectionName(property.associatedEntity)
                            identifierList = identifierList.findAll(){ it != null }.collect {
                                new DBRef(collectionName, it)
                            }
                        }
                        listCodec.encode writer, identifierList, encoderContext
                    }
                }
            }
        }
    }

    /**
     * A {@PropertyEncoder} capable of encoding {@ToOne} association types
     */
    static class ToOneEncoder implements PropertyEncoder<ToOne> {

        @Override
        void encode(BsonWriter writer, ToOne property, Object value, EntityAccess parentAccess, EncoderContext encoderContext, MongoDatastore datastore) {
            if(value) {
                def associatedEntity = property.associatedEntity

                Object associationId
                if(property.doesCascade(CascadeType.PERSIST) && associatedEntity != null) {
                    if(!property.isForeignKeyInChild()) {
                        def proxyFactory = datastore.mappingContext.proxyFactory
                        def codecRegistry = datastore.codecRegistry
                        if(proxyFactory.isProxy(value)) {
                            associationId = proxyFactory.getIdentifier(value)
                        }
                        else {
                            def associationAccess = datastore.mappingContext.getEntityReflector(associatedEntity)
                            associationId = associationAccess.getIdentifier(value)
                        }
                        if(associationId != null) {
                            writer.writeName MappingUtils.getTargetKey(property)
                            MongoAttribute attr = (MongoAttribute)property.mapping.mappedForm
                            if(attr?.isReference()) {
                                AbstractMongoSession mongoSession = (AbstractMongoSession)datastore.currentSession
                                def identityEncoder = codecRegistry.get(DBRef)

                                def ref = new DBRef(mongoSession.getCollectionName( associatedEntity),associationId)
                                identityEncoder.encode writer, ref, encoderContext
                            }
                            else {
                                def identityEncoder = codecRegistry.get(associationId.getClass())
                                identityEncoder.encode writer, associationId, encoderContext
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * A {@PropertyEncoder} capable of encoding {@ToOne} association types
     */
    static class ToOneDecoder implements PropertyDecoder<ToOne> {

        @Override
        void decode(BsonReader bsonReader, ToOne property, EntityAccess entityAccess, DecoderContext decoderContext, MongoDatastore datastore) {
            def mongoSession = datastore.currentSession
            MongoAttribute attr = (MongoAttribute)property.mapping.mappedForm
            boolean isLazy = isLazyAssociation(attr)
            def associatedEntity = property.associatedEntity
            if(associatedEntity == null) {
                bsonReader.skipValue()
                return
            }

            Serializable associationId

            if(attr.reference && bsonReader.currentBsonType == BsonType.DOCUMENT) {
                def dbRefCodec = datastore.codecRegistry.get(Document)
                def dBRef = dbRefCodec.decode(bsonReader, decoderContext)
                associationId = (Serializable)dBRef.get(DB_REF_ID_FIELD)
            }
            else {
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
            }


            if(isLazy) {
                entityAccess.setPropertyNoConversion(
                        property.name,
                        mongoSession.proxy(associatedEntity.javaClass, associationId )
                )
            }
            else {
                entityAccess.setPropertyNoConversion(
                        property.name,
                        mongoSession.retrieve(associatedEntity.javaClass, associationId )
                )
            }

        }

        private boolean isLazyAssociation(MongoAttribute attribute) {
            if (attribute == null) {
                return true
            }

            return attribute.getFetchStrategy() == FetchType.LAZY
        }

    }

    /**
     * A {@PropertyEncoder} capable of encoding {@Embedded} association types
     */
    static class EmbeddedEncoder implements PropertyEncoder<Embedded> {

        @Override
        void encode(BsonWriter writer, Embedded property, Object value, EntityAccess parentAccess, EncoderContext encoderContext, MongoDatastore datastore) {
            if(value != null) {
                PersistentEntity associatedEntity = datastore.mappingContext.getPersistentEntity(value.getClass().name)
                if(associatedEntity == null) {
                    associatedEntity = property.associatedEntity
                }

                writer.writeName MappingUtils.getTargetKey(property)

                def reflector = datastore.mappingContext.getEntityReflector(associatedEntity)
                PersistentEntityCodec codec = datastore.getPersistentEntityCodec(associatedEntity)


                def identifier = reflector.getIdentifier(value)

                def hasIdentifier = identifier != null
                codec.encode(writer, value, encoderContext, hasIdentifier)
            }
        }
    }

    /**
     * A {@PropertyDecoder} capable of decoding {@Embedded} association types
     */
    static class EmbeddedDecoder implements PropertyDecoder<Embedded> {

        @Override
        void decode(BsonReader reader, Embedded property, EntityAccess entityAccess, DecoderContext decoderContext, MongoDatastore datastore) {
            def associatedEntity = property.associatedEntity
            PersistentEntityCodec codec = datastore.getPersistentEntityCodec(associatedEntity)

            def decoded = codec.decode(reader, decoderContext)
            if(decoded instanceof DirtyCheckable) {
                decoded.trackChanges()
            }
            entityAccess.setPropertyNoConversion(
                    property.name,
                    decoded
            )

        }
    }

    /**
     * A {@PropertyEncoder} capable of encoding {@EmbeddedCollection} collection types
     */
    static class EmbeddedCollectionEncoder implements PropertyEncoder<EmbeddedCollection> {

        @Override
        void encode(BsonWriter writer, EmbeddedCollection property, Object value, EntityAccess parentAccess, EncoderContext encoderContext, MongoDatastore datastore) {

            writer.writeName MappingUtils.getTargetKey(property)

            def associatedEntity = property.associatedEntity
            PersistentEntityCodec associatedCodec = datastore.getPersistentEntityCodec( associatedEntity )
            def isBidirectional = property.isBidirectional()
            Association inverseSide = isBidirectional ? property.inverseSide : null
            String inverseProperty = isBidirectional ? inverseSide.name : null
            def isToOne = inverseSide instanceof ToOne
            def mappingContext = datastore.mappingContext

            if(Collection.isInstance(value)) {
                writer.writeStartArray()

                for(v in value) {
                    if(v != null) {
                        PersistentEntityCodec codec = associatedCodec
                        PersistentEntity entity = associatedEntity

                        def cls = v.getClass()
                        if(cls != associatedEntity.javaClass) {
                            // try subclass

                            def childEntity = mappingContext.getPersistentEntity(cls.name)
                            if(childEntity != null) {
                                entity = childEntity
                                codec = datastore.getPersistentEntityCodec(cls)
                            }
                            else {
                                continue
                            }
                        }

                        def ea = datastore.mappingContext.getEntityReflector(entity)
                        def id = ea.getIdentifier(v)
                        if(isBidirectional) {
                            if(isToOne) {
                                ea.setProperty(v, inverseProperty, parentAccess.entity)
                            }
                        }

                        codec.encode(writer, v, encoderContext, id != null)
                    }
                }
                writer.writeEndArray()
            }
            else if(Map.isInstance(value)) {
                writer.writeStartDocument()

                for(e in value) {
                    Map.Entry<String, Object> entry = (Map.Entry<String, Object>)e

                    writer.writeName entry.key


                    def v = entry.value
                    def ea = datastore.mappingContext.getEntityReflector(associatedEntity)
                    def id = ea.getIdentifier(v)

                    associatedCodec.encode(writer, v, encoderContext, id != null)

                }
                writer.writeEndDocument()
            }


        }
    }
    /**
     * A {@PropertyDecoder} capable of decoding {@EmbeddedCollection} collection types
     */
    static class EmbeddedCollectionDecoder implements PropertyDecoder<EmbeddedCollection> {

        @Override
        void decode(BsonReader reader, EmbeddedCollection property, EntityAccess entityAccess, DecoderContext decoderContext, MongoDatastore datastore) {
            def associatedEntity = property.associatedEntity
            def associationCodec = datastore.getPersistentEntityCodec(associatedEntity)
            if(Collection.isAssignableFrom(property.type)) {
                reader.readStartArray()
                def bsonType = reader.readBsonType()
                def collection = MappingUtils.createConcreteCollection(property.type)
                while(bsonType != BsonType.END_OF_DOCUMENT) {
                    collection << associationCodec.decode(reader, decoderContext)
                    bsonType = reader.readBsonType()
                }
                reader.readEndArray()
                entityAccess.setPropertyNoConversion(
                        property.name,
                        DirtyCheckingSupport.wrap(collection, (DirtyCheckable)entityAccess.entity, property.name)
                )
            }
            else if(Map.isAssignableFrom(property.type)) {
                reader.readStartDocument()
                def bsonType = reader.readBsonType()
                def map = [:]
                while(bsonType != BsonType.END_OF_DOCUMENT) {
                    def key = reader.readName()
                    map[key] = associationCodec.decode(reader, decoderContext)
                    bsonType = reader.readBsonType()
                }
                reader.readEndDocument()
                entityAccess.setPropertyNoConversion(
                        property.name,
                        new DirtyCheckingMap(map, (DirtyCheckable)entityAccess.entity, property.name)
                )
            }
            else {
                reader.skipValue()
            }
        }
    }
    /**
     * A {@PropertyDecoder} capable of decoding {@Basic} collection types
     */
    static class BasicCollectionTypeDecoder implements PropertyDecoder<Basic> {

        @Override
        void decode(BsonReader reader, Basic property, EntityAccess entityAccess, DecoderContext decoderContext, MongoDatastore datastore) {
            CustomTypeMarshaller marshaller = property.customTypeMarshaller

            if(marshaller) {
                CustomTypeDecoder.decode(datastore, reader, decoderContext, marshaller, property, entityAccess)
            }
            else {
                def conversionService = datastore.mappingContext.conversionService
                def componentType = property.componentType
                Codec codec = datastore.codecRegistry.get(property.type)
                def value = codec.decode(reader, decoderContext)
                def entity = entityAccess.entity
                if(value instanceof Collection) {
                    def converted = value.collect() { conversionService.convert(it, componentType) }


                    if(entity instanceof DirtyCheckable) {
                        converted = DirtyCheckingSupport.wrap(converted, (DirtyCheckable) entity, property.name)
                    }
                    entityAccess.setProperty( property.name, converted )
                }
                else if(value instanceof Map) {
                    def converted = value.collectEntries() { Map.Entry entry ->
                        def v = entry.value
                        entry.value = conversionService.convert(v, componentType)
                        return entry
                    }
                    if(entity instanceof DirtyCheckable) {
                        converted = new DirtyCheckingMap(converted, (DirtyCheckable) entity, property.name)
                    }
                    entityAccess.setProperty( property.name, converted)
                }
            }
        }
    }

    /**
     * A {@PropertyEncoder} capable of encoding {@Basic}  collection types
     */
    static class BasicCollectionTypeEncoder implements PropertyEncoder<Basic> {

        @Override
        void encode(BsonWriter writer, Basic property, Object value, EntityAccess parentAccess, EncoderContext encoderContext, MongoDatastore datastore) {
            def marshaller = property.customTypeMarshaller
            if(marshaller) {
                CustomTypeEncoder.encode(datastore, encoderContext, writer, property, marshaller, value)
            }
            else {
                writer.writeName( MappingUtils.getTargetKey(property) )
                Codec codec = datastore.codecRegistry.get(property.type)
                codec.encode(writer, value, encoderContext)
                def parent = parentAccess.entity
                if(parent instanceof DirtyCheckable) {
                    if(value instanceof Collection) {
                        def propertyName = property.name
                        parentAccess.setPropertyNoConversion(
                                propertyName,
                                DirtyCheckingSupport.wrap(value, parent, propertyName)
                        )
                    }
                    else if(value instanceof Map &&  !(value instanceof Bson)) {
                        def propertyName = property.name
                        parentAccess.setPropertyNoConversion(
                                propertyName,
                                new DirtyCheckingMap(value, parent, propertyName)
                        )
                    }
                }
            }
        }
    }
}
