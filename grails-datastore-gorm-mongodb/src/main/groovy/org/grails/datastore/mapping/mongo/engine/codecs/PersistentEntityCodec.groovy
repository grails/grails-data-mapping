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

import groovy.transform.CompileStatic
import org.bson.BsonArray
import org.bson.BsonBinary
import org.bson.BsonDocument
import org.bson.BsonDocumentWriter
import org.bson.BsonReader
import org.bson.BsonType
import org.bson.BsonWriter
import org.bson.Document
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.codecs.configuration.CodecRegistry
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.grails.datastore.mapping.dirty.checking.DirtyCheckingCollection
import org.grails.datastore.mapping.dirty.checking.DirtyCheckingMap
import org.grails.datastore.mapping.dirty.checking.DirtyCheckingSupport
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.engine.internal.MappingUtils
import org.grails.datastore.mapping.engine.types.CustomTypeMarshaller
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.Basic
import org.grails.datastore.mapping.model.types.Custom
import org.grails.datastore.mapping.model.types.Embedded
import org.grails.datastore.mapping.model.types.Simple
import org.grails.datastore.mapping.mongo.MongoDatastore
import org.grails.datastore.mapping.mongo.engine.MongoCodecEntityPersister
import org.springframework.core.convert.ConversionService


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

    static {
        ENCODERS[Simple] = new SimpleEncoder()
        DECODERS[Simple] = new SimpleDecoder()
        ENCODERS[Embedded] = new EmbeddedEncoder()
        DECODERS[Embedded] = new EmbeddedDecoder()
        ENCODERS[Custom] = new CustomTypeEncoder()
        DECODERS[Custom] = new CustomTypeDecoder()
        ENCODERS[Basic] = new BasicCollectionTypeEncoder()
        DECODERS[Basic] = new BasicCollectionTypeDecoder()
    }

    final MongoDatastore datastore
    final MappingContext mappingContext
    final PersistentEntity entity

    PersistentEntityCodec(MongoDatastore datastore, PersistentEntity entity) {
        this.datastore = datastore
        this.mappingContext = datastore.mappingContext
        this.entity = entity
    }

    @Override
    Object decode(BsonReader bsonReader, DecoderContext decoderContext) {
        bsonReader.readStartDocument()
        def instance = entity.javaClass.newInstance()
        EntityAccess access = createEntityAccess(instance)
        BsonType bsonType = bsonReader.readBsonType()
        while(bsonType != BsonType.END_OF_DOCUMENT) {

            def name = bsonReader.readName()
            if(MongoCodecEntityPersister.MONGO_ID_FIELD == name) {
                switch(bsonType) {
                    case BsonType.OBJECT_ID:
                        access.setIdentifier( bsonReader.readObjectId() )
                        break
                    case BsonType.INT64:
                        access.setIdentifier( bsonReader.readInt64() )
                        break
                    case BsonType.INT32:
                        access.setIdentifier( bsonReader.readInt32() )
                        break
                    default:
                        access.setIdentifier( bsonReader.readString())
                }
            }
            else {
                def property = entity.getPropertyByName(name)
                if(property) {
                    def propKind = property.getClass().superclass
                    switch(property.type) {
                        case CharSequence:
                            access.setPropertyNoConversion(name, bsonReader.readString())
                        break
                        default:
                            DECODERS[propKind]?.decode(bsonReader, property, access, decoderContext, datastore)
                    }

                }
                else {
                    bsonReader.skipValue()
                }

            }
            bsonType = bsonReader.readBsonType()
        }
        bsonReader.readEndDocument()
        return instance

    }

    protected EntityAccess createEntityAccess(instance) {
        return datastore.createEntityAccess(entity, instance)
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
    Bson encodeUpdate(Object value, EntityAccess access = createEntityAccess(value), EncoderContext encoderContext = EncoderContext.builder().build()) {
        Document update = new Document()
        if(value instanceof DirtyCheckable) {
            def sets = new BsonDocument()
            def unsets = new Document()
            def writer = new BsonDocumentWriter(sets)
            writer.writeStartDocument()
            DirtyCheckable dirty = (DirtyCheckable)value
            Set<String> processed = []
            for(propertyName in dirty.listDirtyPropertyNames()) {
                def prop = entity.getPropertyByName(propertyName)
                if(prop != null) {

                    processed << propertyName
                    Object v = access.getProperty(prop.name)
                    if (v != null) {
                        def propKind = prop.getClass().superclass
                        ENCODERS[propKind]?.encode(writer, prop, v, encoderContext, datastore)
                    }
                    else {
                        unsets[prop.name] = BLANK_STRING
                    }
                }
            }

            for(association in entity.associations) {
                if(processed.contains( association.name )) continue

                def v = access.getProperty(association.name)
                if( v instanceof DirtyCheckable ) {
                    DirtyCheckable d = (DirtyCheckable)v
                    if(d.hasChanged()) {
                        def propKind = association.getClass().superclass
                        ENCODERS[propKind]?.encode(writer, association, v, encoderContext, datastore)
                    }
                }

                // TODO: handle unprocessed association
            }

            writer.writeEndDocument()

            if(sets) {
                update[MONGO_SET_OPERATOR] = sets
            }
            if(unsets) {
                update[MONGO_UNSET_OPERATOR] = unsets
            }
        }
        else {
            // TODO: Support non-dirty checkable objects?
        }

        return update
    }

    void encode(BsonWriter writer, value, EncoderContext encoderContext, boolean includeIdentifier) {
        writer.writeStartDocument()
        def access = createEntityAccess(value)
        if (includeIdentifier) {

            def id = access.getIdentifier()
            writer.writeName(MongoCodecEntityPersister.MONGO_ID_FIELD)

            if (id instanceof ObjectId) {
                writer.writeObjectId(id)
            } else if (id instanceof Number) {
                writer.writeInt64(((Number) id).toLong())
            } else {
                writer.writeString(id.toString())
            }
        }

        for (PersistentProperty prop in entity.persistentProperties) {
            def propKind = prop.getClass().superclass
            Object v = access.getProperty(prop.name)
            if (v != null) {
                ENCODERS[propKind]?.encode(writer, (PersistentProperty) prop, v, encoderContext, datastore)
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
        void encode(BsonWriter writer, T property, Object value, EncoderContext encoderContext, MongoDatastore datastore)
    }

    /**
     * An interface for encoding PersistentProperty instances
     */
    static interface PropertyDecoder<T extends PersistentProperty> {
        void decode(BsonReader reader, T property, EntityAccess entityAccess, DecoderContext decoderContext, MongoDatastore datastore)
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
            SIMPLE_TYPE_DECODERS[Byte] = convertingIntReader
            SIMPLE_TYPE_DECODERS[Integer] = new TypeDecoder() {
                @Override
                void decode(BsonReader reader, Simple property, EntityAccess entityAccess) {
                    entityAccess.setPropertyNoConversion( property.name, reader.readInt32() )
                }
            }
            SIMPLE_TYPE_DECODERS[Long] = new TypeDecoder() {
                @Override
                void decode(BsonReader reader, Simple property, EntityAccess entityAccess) {
                    entityAccess.setPropertyNoConversion( property.name, reader.readInt64() )
                }
            }
            SIMPLE_TYPE_DECODERS[Double] = new TypeDecoder() {
                @Override
                void decode(BsonReader reader, Simple property, EntityAccess entityAccess) {
                    entityAccess.setPropertyNoConversion( property.name, reader.readDouble() )
                }
            }
            SIMPLE_TYPE_DECODERS[Boolean] = new TypeDecoder() {
                @Override
                void decode(BsonReader reader, Simple property, EntityAccess entityAccess) {
                    entityAccess.setPropertyNoConversion( property.name, reader.readBoolean() )
                }
            }

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


            SIMPLE_TYPE_ENCODERS = new HashMap<Class, TypeEncoder>().withDefault { Class ->
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
            SIMPLE_TYPE_ENCODERS[Integer] = smallNumberEncoder
            SIMPLE_TYPE_ENCODERS[Short] = smallNumberEncoder
            SIMPLE_TYPE_ENCODERS[Double] = new TypeEncoder() {
                @Override
                void encode(BsonWriter writer, Simple property, Object value) {
                    writer.writeDouble( (Double)value )
                }
            }
            SIMPLE_TYPE_ENCODERS[Long] = new TypeEncoder() {
                @Override
                void encode(BsonWriter writer, Simple property, Object value) {
                    writer.writeInt64( (Long)value )
                }
            }
            SIMPLE_TYPE_ENCODERS[Boolean] = new TypeEncoder() {
                @Override
                void encode(BsonWriter writer, Simple property, Object value) {
                    writer.writeBoolean( (Boolean)value )
                }
            }
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
        }

        @Override
        void encode(BsonWriter writer, Simple property, Object value, EncoderContext encoderContext, MongoDatastore datastore) {
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
            def registry = datastore.getCodecRegistry()
            def documentCodec = registry.get(Document)
            def bsonType = reader.currentBsonType
            if(bsonType == BsonType.DOCUMENT) {

                Document doc = documentCodec.decode(reader, decoderContext)

                def value = marshaller.read(property, new Document(
                        MappingUtils.getTargetKey(property),
                        doc
                ))
                if (value != null) {
                    entityAccess.setPropertyNoConversion(property.name, value)
                }
            }
            else if(bsonType == BsonType.ARRAY) {
                def arrayCodec = registry.get(List)
                def array = arrayCodec.decode(reader, decoderContext)
                def value = marshaller.read(property, new Document(
                        MappingUtils.getTargetKey(property),
                        array
                ))
                if (value != null) {
                    entityAccess.setPropertyNoConversion(property.name, value)
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
        void encode(BsonWriter writer, Custom property, Object value, EncoderContext encoderContext, MongoDatastore datastore) {
            def marshaller = property.customTypeMarshaller
            encode(datastore, encoderContext, writer, property, marshaller, value)

        }

        protected static void encode(MongoDatastore datastore, EncoderContext encoderContext, BsonWriter writer, PersistentProperty property, CustomTypeMarshaller marshaller, value) {
            String targetName = MappingUtils.getTargetKey(property)
            def document = new Document()
            marshaller.write(property, value, document)

            Object converted = document.get(targetName)
            if (converted instanceof Document) {
                Codec codec = (Codec) datastore.codecRegistry.get(converted.getClass())
                if (codec) {
                    writer.writeName(targetName)
                    codec.encode(writer, converted, encoderContext)
                }
            }
            else if(converted instanceof List) {
                Codec<List> codec = datastore.codecRegistry.get(List)
                if(codec) {
                    writer.writeName(targetName)
                    codec.encode(writer, converted, encoderContext)
                }
            }
        }
    }

    /**
     * A {@PropertyEncoder} capable of encoding {@Embedded} association types
     */
    static class EmbeddedEncoder implements PropertyEncoder<Embedded> {

        @Override
        void encode(BsonWriter writer, Embedded property, Object value, EncoderContext encoderContext, MongoDatastore datastore) {
            if(value) {
                def associatedEntity = property.associatedEntity
                def registry = datastore.codecRegistry
                writer.writeName property.name
                def access = datastore.createEntityAccess(associatedEntity, value)

                PersistentEntityCodec codec = (PersistentEntityCodec)registry.get(associatedEntity.javaClass)
                codec.encode(writer, value, encoderContext, access.identifier ? true : false)
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
            def registry = datastore.codecRegistry


            PersistentEntityCodec codec = (PersistentEntityCodec)registry.get(associatedEntity.javaClass)

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
        void encode(BsonWriter writer, Basic property, Object value, EncoderContext encoderContext, MongoDatastore datastore) {
            def marshaller = property.customTypeMarshaller
            if(marshaller) {
                CustomTypeEncoder.encode(datastore, encoderContext, writer, property, marshaller, value)
            }
            else {
                writer.writeName( MappingUtils.getTargetKey(property) )
                Codec codec = datastore.codecRegistry.get(property.type)
                codec.encode(writer, value, encoderContext)
            }
        }
    }
}
