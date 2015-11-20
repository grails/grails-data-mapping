/* Copyright (C) 2010 SpringSource
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
package org.grails.datastore.mapping.mongo.config;

import com.mongodb.DBObject;

import groovy.lang.Closure;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.bson.BSONObject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.BSONTimestamp;
import org.bson.types.Code;
import org.bson.types.CodeWScope;
import org.bson.types.Symbol;
import org.grails.datastore.gorm.mongo.geo.*;
import org.grails.datastore.gorm.mongo.simple.EnumType;
import org.grails.datastore.mapping.config.AbstractGormMappingFactory;
import org.grails.datastore.mapping.config.Property;
import org.grails.datastore.mapping.document.config.Collection;
import org.grails.datastore.mapping.document.config.DocumentMappingContext;
import org.grails.datastore.mapping.model.AbstractClassMapping;
import org.grails.datastore.mapping.model.ClassMapping;
import org.grails.datastore.mapping.model.EmbeddedPersistentEntity;
import org.grails.datastore.mapping.model.IdentityMapping;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.MappingFactory;
import org.grails.datastore.mapping.model.PersistentEntity;

import com.mongodb.DBRef;
import org.grails.datastore.mapping.reflect.FieldEntityAccess;

/**
 * Models a {@link org.grails.datastore.mapping.model.MappingContext} for Mongo.
 *
 * @author Graeme Rocher
 */
@SuppressWarnings("rawtypes")
public class MongoMappingContext extends DocumentMappingContext {
    /**
     * Java types supported as mongo property types.
     */
    private static final Set<String> MONGO_NATIVE_TYPES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            Double.class.getName(),
            String.class.getName(),
            Document.class.getName(),
            com.mongodb.DBObject.class.getName(),
            org.bson.types.Binary.class.getName(),
            org.bson.types.ObjectId.class.getName(),
            DBRef.class.getName(),
            Boolean.class.getName(),
            Date.class.getName(),
            Pattern.class.getName(),
            Symbol.class.getName(),
            Integer.class.getName(),
            BSONTimestamp.class.getName(),
            Code.class.getName(),
            CodeWScope.class.getName(),
            Long.class.getName(),
            UUID.class.getName(),
            byte[].class.getName(),
            Byte.class.getName()
    )));

    /**
     * Check whether a type is a native mongo type that can be stored by the mongo driver without conversion.
     * @param clazz The class to check.
     * @return true if no conversion is required and the type can be stored natively.
     */
    public static boolean isMongoNativeType(Class clazz) {
        return MongoMappingContext.MONGO_NATIVE_TYPES.contains(clazz.getName()) ||
                Bson.class.isAssignableFrom(clazz.getClass()) || DBObject.class.isAssignableFrom(clazz.getClass());
    }

    private final class MongoDocumentMappingFactory extends
            AbstractGormMappingFactory<MongoCollection, MongoAttribute> {
        @Override
        protected Class<MongoAttribute> getPropertyMappedFormType() {
            return MongoAttribute.class;
        }

        @Override
        protected Class<MongoCollection> getEntityMappedFormType() {
            return MongoCollection.class;
        }

        @Override
        protected IdentityMapping getIdentityMappedForm(final ClassMapping classMapping, final MongoAttribute property) {
            if (property == null) {
                return super.getIdentityMappedForm(classMapping, property);
            }

            return new IdentityMapping() {
                public String[] getIdentifierName() {
                    if (property.getName() == null) {
                        return new String[] { MappingFactory.IDENTITY_PROPERTY };
                    }
                    return new String[] { property.getName()};
                }

                public ClassMapping getClassMapping() {
                    return classMapping;
                }

                public Property getMappedForm() {
                    return property;
                }
            };
        }

        @Override
        public boolean isSimpleType(Class propType) {
            if (propType == null) return false;
            if (propType.isArray()) {
                return isSimpleType(propType.getComponentType()) || super.isSimpleType(propType);
            }
            return isMongoNativeType(propType) || super.isSimpleType(propType);
        }
    }

    public MongoMappingContext(String defaultDatabaseName) {
        super(defaultDatabaseName);
        registerMongoTypes();
    }

    public MongoMappingContext(String defaultDatabaseName, Closure defaultMapping) {
        super(defaultDatabaseName, defaultMapping);
        registerMongoTypes();
    }

    protected void registerMongoTypes() {
        MappingFactory.registerCustomType(new GeometryCollectionType());
        MappingFactory.registerCustomType(new PointType());
        MappingFactory.registerCustomType(new PolygonType());
        MappingFactory.registerCustomType(new LineStringType());
        MappingFactory.registerCustomType(new MultiLineStringType());
        MappingFactory.registerCustomType(new MultiPointType());
        MappingFactory.registerCustomType(new MultiPolygonType());
        MappingFactory.registerCustomType(new ShapeType());
        MappingFactory.registerCustomType(new BoxType());
        MappingFactory.registerCustomType(new CircleType());
        MappingFactory.registerCustomType(new EnumType());
    }

    @Override
    protected MappingFactory createDocumentMappingFactory(Closure defaultMapping) {
        MongoDocumentMappingFactory mongoDocumentMappingFactory = new MongoDocumentMappingFactory();
        mongoDocumentMappingFactory.setDefaultMapping(defaultMapping);
        return mongoDocumentMappingFactory;
    }

    @Override
    public PersistentEntity createEmbeddedEntity(Class type) {
        return new DocumentEmbeddedPersistentEntity(type, this);
    }

    class DocumentEmbeddedPersistentEntity extends EmbeddedPersistentEntity {

        private DocumentCollectionMapping classMapping ;
        public DocumentEmbeddedPersistentEntity(Class type, MappingContext ctx) {
            super(type, ctx);
            classMapping = new DocumentCollectionMapping(this, ctx);
        }

        @Override
        public ClassMapping getMapping() {
            return classMapping;
        }
        public class DocumentCollectionMapping extends AbstractClassMapping<Collection> {
            private Collection mappedForm;

            public DocumentCollectionMapping(PersistentEntity entity, MappingContext context) {
                super(entity, context);
                this.mappedForm = (Collection) context.getMappingFactory().createMappedForm(DocumentEmbeddedPersistentEntity.this);
            }
            @Override
            public Collection getMappedForm() {
                return mappedForm ;
            }
        }
    }
}
