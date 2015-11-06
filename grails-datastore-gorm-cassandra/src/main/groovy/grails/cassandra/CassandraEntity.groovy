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

package grails.cassandra

import groovy.transform.CompileStatic
import org.codehaus.groovy.runtime.InvokerHelper
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormEntity
import org.grails.datastore.mapping.cassandra.CassandraDatastore
import org.grails.datastore.mapping.cassandra.CassandraSession
import org.grails.datastore.mapping.cassandra.engine.CassandraEntityPersister
import org.grails.datastore.mapping.cassandra.utils.OptionsUtil
import org.grails.datastore.mapping.core.Session
import org.springframework.data.cassandra.core.CassandraTemplate
/**
 * @author Adds new methods for Cassandra
 */
@CompileStatic
trait CassandraEntity<D> extends GormEntity<D> {

    /**
     * Saves an object with the given parameters
     * @param instance The instance
     * @param params The parameters
     * @return The instance
     */
    @Override
    D save( Map params) {
        withSession ({ Session session ->
            ((CassandraDatastore)session.datastore).setWriteOptions(this, OptionsUtil.convertToWriteOptions(params))
            doSave params, session
        } )
    }

    private D doSave(Map params, Session s, boolean isInsert = false) {
        CassandraSession session = (CassandraSession) s
        boolean hasErrors = false
        boolean validate = params?.containsKey("validate") ? params.validate : true
        MetaMethod validateMethod = respondsTo('validate', InvokerHelper.EMPTY_ARGS).find { MetaMethod mm -> mm.parameterTypes.length == 0 && !mm.vargsMethod}
        if (validateMethod && validate) {
            session.datastore.setSkipValidation(this, false)
            hasErrors = !validateMethod.invoke(this, InvokerHelper.EMPTY_ARGS)
        }
        else {
            session.datastore.setSkipValidation(this, true)
            InvokerHelper.invokeMethod(this, "clearErrors", null)
        }

        if (hasErrors) {
            def instanceApi = GormEnhancer.findInstanceApi(getClass())
            boolean failOnErrorEnabled = params?.containsKey("failOnError") ? params.failOnError : instanceApi.failOnError
            if (failOnErrorEnabled) {
                throw instanceApi.validationException.newInstance("Validation error occurred during call to save()", InvokerHelper.getProperty(this, "errors"))
            }
            return null
        }
        if (params?.update) {
            session.update(this)
        } else if (params?.updateSingleTypes) {
            getCassandraEntityPersister(session).updateSingleTypes(this)
        } else {
            if (isInsert) {
                session.insert(this)
            }
            else {
                markDirty()
                session.persist(this)
            }
        }
        if (params?.flush) {
            session.flush()
        }
        return this
    }

    /**
     * Update an instance without loading into the session
     * @param instance The instance
     * @param params The parameters
     * @return Returns the instance
     */
    D update(Map params = [:]) {
        params.update = true
        save(params)
    }

    /**
     * Update an instance's non collection, non map types only without loading into the session
     * @param instance The instance
     * @param params The parameters
     * @return Returns the instance
     */
    D updateSingleTypes(Map params = [:]) {
        params.updateSingleTypes = true
        save(params)
    }

    /**
     * Add the specified element to the instance's embedded list, set or map and generate an update for the datastore
     * @param instance the instance containing the collection or map
     * @param propertyName the name of the embedded list, set or map
     * @param element the element to add
     * @param params The parameters
     */
    void append(String propertyName, Object element, Map params = [:]) {
        withSession { Session session ->
            getCassandraEntityPersister(session).append(this, propertyName, element, OptionsUtil.convertToWriteOptions(params))
            if (params.flush) {
                session.flush()
            }
        }
    }

    /**
     * Prepend the specified element to the instance's embedded list and generate an update for the datastore
     * @param instance the instance containing the list
     * @param propertyName the name of the embedded list
     * @param element the element to prepend
     * @param params The parameters
     */
    void prepend(String propertyName, Object element, Map params = [:]) {
        withSession { Session session ->
            getCassandraEntityPersister(session).prepend(this, propertyName, element, OptionsUtil.convertToWriteOptions(params))
            if (params.flush) {
                session.flush()
            }
        }
    }

    /**
     * Replace the specified element at the specified index in the instance's embedded list and generate an update for the datastore
     * @param instance the instance containing the list
     * @param propertyName the name of the embedded list
     * @param index the index of the element to replace
     * @param element the element to be stored at the specified index
     * @param params The parameters
     */
    void replaceAt(String propertyName, int index, Object element, Map params = [:]) {
        withSession { Session session ->
            getCassandraEntityPersister(session).replaceAt(this, propertyName, index, element, OptionsUtil.convertToWriteOptions(params))
            if (params.flush) {
                session.flush()
            }
        }
    }

    /**
     * Remove the specified element, or the element at the specified index, from the instance's embedded list, set or map and generate an update for the datastore
     * @param instance the instance containing the collection or map
     * @param propertyName the name of the embedded list, set or map
     * @param item the element or index of the element to remove in the case of a list, the element in the case of a set or map
     * @param isIndex whether the specified item is an element or the index of the element to remove, only true if removing from a list using index, false otherwise
     * @param params The parameters
     */
    void deleteFrom(String propertyName, Object item, boolean isIndex, Map params = [:]) {
        if (isIndex && !(item instanceof Integer || item instanceof int)) {
            throw new IllegalArgumentException("Deleting item by index, item must be an integer")
        }
        withSession { Session session ->
            getCassandraEntityPersister(session).deleteFrom(this, propertyName, item, isIndex, OptionsUtil.convertToWriteOptions(params))
            if (params.flush) {
                session.flush()
            }
        }
    }

    private CassandraEntityPersister getCassandraEntityPersister(Session session) {
        return (CassandraEntityPersister) session.getPersister(this)
    }

    private static CassandraEntityPersister getStaticCassandraEntityPersister(Session session) {
        return (CassandraEntityPersister) session.getPersister(this)
    }

    static CassandraTemplate getCassandraTemplate() {
        withSession { CassandraSession session ->
            return session.cassandraTemplate
        }
    }

    /**
     * Update a property on an instance with the specified item
     * @param id the id of the instance to update
     * @param propertyName the name of the property to update
     * @param item the new value of the property
     * @param params The parameters
     */
    static void updateProperty(Serializable id, String propertyName, Object item, Map params = [:]) {
        withSession { CassandraSession session ->
            getStaticCassandraEntityPersister(session).updateProperty(id, propertyName, item, OptionsUtil.convertToWriteOptions(params))
            if (params.flush) {
                session.flush()
            }
        }
    }

    /**
     * Update multiple properties on an instance with the specified properties
     * @param id the id of the instance to update
     * @param properties a map of property name/value pairs to update
     * @param params The parameters
     */
    static void updateProperties(Serializable id, Map properties, Map params = [:]) {
        withSession { CassandraSession session ->
            getStaticCassandraEntityPersister(session).updateProperties(id, properties, OptionsUtil.convertToWriteOptions(params))
            if (params.flush) {
                session.flush()
            }
        }
    }

    /**
     * Add the specified element to the instance's embedded list, set or map in the datastore
     * @param id the id of the instance to update
     * @param propertyName the name of the embedded list, set or map
     * @param element the element to add
     * @param params The parameters
     */
    static void append(Serializable id, String propertyName, Object element, Map params = [:]) {
        withSession { CassandraSession session ->
            getStaticCassandraEntityPersister(session).append(id, propertyName, element, OptionsUtil.convertToWriteOptions(params))
            if (params.flush) {
                session.flush()
            }
        }
    }

    /**
     * Prepend the specified element to the instance's embedded list in the datastore
     * @param id the id of the instance to update
     * @param propertyName the name of the embedded list
     * @param element the element to prepend
     * @param params The parameters
     */
    static void prepend(Serializable id, String propertyName, Object element, Map params = [:]) {
        withSession { CassandraSession session ->
            getStaticCassandraEntityPersister(session).prepend(id, propertyName, element, OptionsUtil.convertToWriteOptions(params))
            if (params.flush) {
                session.flush()
            }
        }
    }

    /**
     * Replace the specified element at the specified index in the instance's embedded list in the datastore
     * @param id the id of the instance to update
     * @param propertyName the name of the embedded list
     * @param index the index of the element to replace
     * @param element the element to be stored at the specified index
     * @param params The parameters
     */
    static void replaceAt(Serializable id, String propertyName, int index, Object element, Map params = [:]) {
        withSession { CassandraSession session ->
            getStaticCassandraEntityPersister(session).replaceAt(id, propertyName, index, element, OptionsUtil.convertToWriteOptions(params))
            if (params.flush) {
                session.flush()
            }
        }
    }

    /**
     * Remove the specified element, or the element at the specified index, from the instance's embedded list, set or map in the datastore
     * @param id the id of the instance to update
     * @param propertyName the name of the embedded list, set or map
     * @param item the element or index of the element to remove in the case of a list, the element in the case of a set or map
     * @param isIndex whether the specified item is an element or the index of the element to remove, only true if removing from a list using index, false otherwise
     * @param params The parameters
     */
    static void deleteFrom(Serializable id, String propertyName, Object element, boolean index, Map params = [:]) {
        withSession { CassandraSession session ->
            getStaticCassandraEntityPersister(session).deleteFrom(id, propertyName, element, index, OptionsUtil.convertToWriteOptions(params))
            if (params.flush) {
                session.flush()
            }
        }
    }

    /**
     * Finds a single result matching all of the given conditions. Eg. Book.findWhere(author:"Stephen King", title:"The Stand").  If
     * a matching persistent entity is not found a new entity is created and returned.
     *
     * @param queryMap The map of conditions
     * @param args The Query arguments
     * @return A single result
     */
    static D findOrCreateWhere(Map queryMap, Map args) {
        internalFindOrCreate(queryMap, args, false)
    }

    /**
     * Finds a single result matching all of the given conditions. Eg. Book.findWhere(author:"Stephen King", title:"The Stand").  If
     * a matching persistent entity is not found a new entity is created, saved and returned.
     *
     * @param queryMap The map of conditions
     * @param args The Query arguments
     * @return A single result
     */
    static D findOrSaveWhere(Map queryMap, Map args) {
        internalFindOrCreate(queryMap, args, true)
    }

    private static  D internalFindOrCreate(Map queryMap, Map args, boolean shouldSave) {
        D result = findWhere(queryMap, args)
        if (!result) {
            def persistentMetaClass = GroovySystem.metaClassRegistry.getMetaClass(this)
            result = (D)persistentMetaClass.invokeConstructor(queryMap)
            if (shouldSave) {
                InvokerHelper.invokeMethod(result, "save", args)
            }
        }
        result
    }

    /**
     * Finds a single result matching all of the given conditions. Eg. Book.findWhere(author:"Stephen King", title:"The Stand")
     *
     * @param queryMap The map of conditions
     * @param args The Query arguments
     *
     * @return A single result
     */
    static D findWhere(Map queryMap, Map args) {
        GormEnhancer.findStaticApi(this).findWhere queryMap, args
    }

    /**
     * Execute a closure whose first argument is a reference to the current session.
     *
     * @param callable the closure
     * @return The result of the closure
     */
    static <T> T withSession(Closure<T> callable) {
        GormEnhancer.findStaticApi(this).withSession callable
    }
}