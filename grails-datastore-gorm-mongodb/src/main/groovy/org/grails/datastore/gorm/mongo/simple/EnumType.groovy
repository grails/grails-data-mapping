/* Copyright (C) 2014 SpringSource
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
package org.grails.datastore.gorm.mongo.simple

import org.grails.datastore.mapping.engine.types.AbstractMappingAwareCustomTypeMarshaller
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.Basic
import org.grails.datastore.mapping.mongo.query.MongoQuery
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.mapping.query.Query.Between
import org.grails.datastore.mapping.query.Query.Equals
import org.grails.datastore.mapping.query.Query.In
import org.grails.datastore.mapping.query.Query.NotEquals
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher

import com.mongodb.BasicDBObject
import com.mongodb.DBObject

/**
 * A custom type for persisting Enum which have an id field in domain classes.
 * For example: To save identity instead of string in database for field <b>type</b>.
 * 
 * <pre>
 *      class User {
 *          UserType type
 *          String name
 *      }
 *
 *      enum UserType {
 *          A(1), B(2)
 *          
 *          final int id
 *          UserType(int id) {
 *              this.id = id
 *          }
 *      }
 * </pre>
 * 
 * @author Shashank Agrawal
 * @author Causecode Technologies
 * 
 * @since 3.1.3
 *
 */
class EnumType extends AbstractMappingAwareCustomTypeMarshaller<Object, DBObject, DBObject> {

    /**
     * Get type of collection by looking at <code>hasMany</code> static field in
     * the domain class.
     * @Example: Will return String class for list of String mapped by hasMany.
     */
    private static Class getCollectionType(PersistentProperty property) {
        PersistentEntity owner = property.owner
        ClassPropertyFetcher propertyFetcher = ClassPropertyFetcher.forClass(property.owner.getJavaClass())
        Map hasManyMap = owner.context.getMappingSyntaxStrategy().getAssociationMap(propertyFetcher)

        hasManyMap[property.name]
    }

    private static boolean isEnumTypeCollection(PersistentProperty property) {
        if (!(property instanceof Basic)) {
            return false
        }

        Class collectionType = getCollectionType(property)

        collectionType && (collectionType.isEnum())
    }

    EnumType() {
        super(Enum)
    }

    // Get the value of enum i.e. if has id, returns the id otherwise return name itself.
    private static def enumValue(def value, def enumType) {
        if (value == null) {
            return null
        }

        if (value.toString().isNumber()) {
            return value
        }

        if (value instanceof String && enumType) {
            value = Enum.valueOf(enumType, value)
        }

        if (value instanceof Enum && value.hasProperty("id")) {
            value = value.id
        } else {
            value = value.toString()
        }

        value
    }

    /**
     * For custom user types, GORM return an empty map where our custom value needs
     * to be inserted.
     * For example: If our code is something like this:
     * <pre>
     *      <code>
     *          User.withCriteria {
     *              or {
     *                  eq("name", "admin")
     *                  eq("status", UserStatus.ACTIVE)
     *              }
     *              eq("foo", "bar")
     *          }
     *      </code>
     * </pre>
     *
     * Then the query we receive will be like:
     * <code>
     *      <pre>
     *          [$and: [[$or: [["name": "admin"], [:]]], ["foo": "bar"]]
     *      </pre>
     * </code>
     *
     * Now we have to place value of status to the blank field.
     * This method searches that empty place and put the value to the right place.
     */
    private void putValueToProperPlace(PersistentProperty property, String queryKey, Query.PropertyCriterion criterion, DBObject nativeQuery) {
        if (!nativeQuery || nativeQuery.isEmpty()) {     // If criteria empty, means we got the place to insert.
            BasicDBObject criteriaObject = new BasicDBObject()

            if (criterion instanceof Equals) {
                nativeQuery.put(queryKey, enumValue(criterion.value, property.getType()))
            } else if (criterion instanceof NotEquals) {
                nativeQuery.put(queryKey, [(MongoQuery.MONGO_NE_OPERATOR): enumValue(criterion.value, property.getType())])
            } else if (criterion instanceof Between) {
                criteriaObject.put(MongoQuery.MONGO_GTE_OPERATOR, enumValue(((Between) criterion).getFrom(), property.getType()))
                criteriaObject.put(MongoQuery.MONGO_LTE_OPERATOR, enumValue(((Between) criterion).getTo()), property.getType())

                nativeQuery.put(queryKey, criteriaObject)
            } else if (criterion instanceof In) {
                List criteriaValues = []
                ((In) criterion).getValues().each { crtieriaValue ->
                    criteriaValues << enumValue(crtieriaValue, property.getType())
                }

                criteriaObject.put(MongoQuery.MONGO_IN_OPERATOR, criteriaValues)
                nativeQuery.put(queryKey, criteriaObject)
            }

            return
        }

        // Iterate each field in the query deeply to get the blank field
        nativeQuery.each { key, value ->
            if (value instanceof Collection) {
                value.each { BasicDBObject queryObject ->
                    if (queryObject.isEmpty()) {
                        // Recursive call the same method.
                        putValueToProperPlace(property, queryKey, criterion, queryObject)
                    }
                }
            }
        }
    }

    @Override
    protected void queryInternal(PersistentProperty property, String key, Query.PropertyCriterion criterion, DBObject nativeQuery) {
        putValueToProperPlace(property, key, criterion, nativeQuery)
    }

    @Override
    protected Object readInternal(PersistentProperty property, String key, DBObject nativeSource) {
        final def value = nativeSource.get(key)
        if (value == null) {
            return null
        }

        def finalValue

        Class propertyType = property.getType()
        // If property is a collection of Enum.
        if (isEnumTypeCollection(property)) {
            finalValue = []
            propertyType = getCollectionType(property)

            // Then value will be a list like: ["ACTIVE", "INACTIVE"]
            value.each { persistedValue ->
                // If value is a number, then Enum type has id field.
                if (persistedValue.toString().isNumber()) {
                    finalValue << propertyType.values().find { it.id == persistedValue.toInteger() }
                } else {
                    // Backward support for the enums that does not have id.
                    finalValue << Enum.valueOf(propertyType, persistedValue)
                }
            }

            return finalValue
        } else if (value.toString().isNumber()) {
            // If value is a number, then Enum type has id field.
            return propertyType.values().find { it.id == value.toInteger() }
        }

        // Backward support for the enums that does not have id.
        return Enum.valueOf(propertyType, value)
    }

    @Override
    protected Object writeInternal(PersistentProperty property, String key, Object value, DBObject nativeTarget) {
        if (!value) {
            nativeTarget.put(key, null)
            return null
        }

        // If property is a collection of Enum.
        if (isEnumTypeCollection(property)) {
            List finalValue = []

            Class collectionType = getCollectionType(property)
            value.each {
                finalValue << enumValue(it, collectionType)
            }

            nativeTarget.put(key, finalValue)
            return finalValue
        }

        def finalValue = enumValue(value, property.getType())
        nativeTarget.put(key, finalValue)
        return finalValue
    }
}