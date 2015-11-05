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
package org.grails.datastore.gorm.neo4j

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.mapping.core.AbstractDatastore
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.neo4j.graphdb.Result


/**
 * Adds instance methods specified to Neo4j
 *
 * @param < D > The domain class type
 */
class Neo4jGormInstanceApi<D> extends GormInstanceApi<D> {

    Neo4jGormInstanceApi(Class<D> persistentClass, Datastore datastore) {
        super(persistentClass, datastore)
    }

    /**
     * @return Returns all dynamic attributes
     */
    Map<String, Object> dynamicAttributes(D instance) {
        def unwrappedInstance = unwrappedInstance(instance)
        def session = datastore.currentSession
        return (Map<String,Object>)getOrInitializeUndeclared(session, unwrappedInstance)
    }

    /**
     * Allows accessing to dynamic properties with the dot operator
     *
     * @param instance The instance
     * @param name The property name
     * @return The property value
     */
    @CompileStatic
    def propertyMissing(D instance, String name) {
        def unwrappedInstance = unwrappedInstance(instance)
        def session = datastore.currentSession
        def undeclared = getOrInitializeUndeclared(session, unwrappedInstance)

        return undeclared.get(name)
    }

    /**
     * dealing with undeclared properties must not happen on proxied instances
     * @param instance
     * @return the unwrapped instance
     */
    @CompileStatic
    private D unwrappedInstance(D instance) {
        def proxyFactory = datastore.mappingContext.proxyFactory
        return (D)proxyFactory.unwrap(instance)
    }

    /**
     * Allows setting a dynamic property via the dot operator
     * @param instance The instance
     * @param name The property name
     * @param val The value
     */
    @CompileStatic
    def propertyMissing(D instance, String name, val) {
        def unwrappedInstance = unwrappedInstance(instance)
        def session = AbstractDatastore.retrieveSession(Neo4jDatastore)
        if (name == Neo4jGormEnhancer.UNDECLARED_PROPERTIES) {
            return getOrInitializeUndeclared(session, unwrappedInstance)
        } else {

            Map undeclaredProps = getOrInitializeUndeclared(session, unwrappedInstance)
            undeclaredProps.put(name, val)
            if (datastore.mappingContext.isPersistentEntity(val)) {
                session.persist(val)
            } else if (Neo4jSession.isCollectionWithPersistentEntities(val, datastore.mappingContext)) {
                session.persist((Iterable)val)
            }
            if (unwrappedInstance instanceof DirtyCheckable) {
                ((DirtyCheckable)unwrappedInstance).markDirty(name)
            }
        }
    }

    @CompileStatic
    protected Map getOrInitializeUndeclared(Session session, D instance) {
        Map undeclaredProps = (Map) session.getAttribute(instance, Neo4jGormEnhancer.UNDECLARED_PROPERTIES)
        if (undeclaredProps == null) {
            undeclaredProps = [:]
            session.setAttribute(instance, Neo4jGormEnhancer.UNDECLARED_PROPERTIES, undeclaredProps)
        }
        undeclaredProps
    }

    /**
     * Allows subscript access to schemaless attributes.
     *
     * @param instance The instance
     * @param name The name of the field
     */
    void putAt(D instance, String name, value) {
        instance."$name" = value

    }

    /**
     * Allows subscript access to schemaless attributes.
     *
     * @param instance The instance
     * @param name The name of the field
     * @return the value
     */
    def getAt(D instance, String name) {
        instance."$name"
    }

    /**
     * perform a cypher query
     * @param queryString
     * @param params
     * @return
     */
    Result cypher(instance, String queryString, Map params ) {
        params['this'] = instance.id
        ((Neo4jDatastore)datastore).graphDatabaseService.execute(queryString, params)
    }

    /**
     * perform a cypher query
     * @param queryString
     * @param params
     * @return
     */
    Result cypher(instance, String queryString, List params ) {
        Map paramsMap = new LinkedHashMap()
        paramsMap.put("this", instance.id)
        int i = 0
        for(p in params) {
            paramsMap.put(String.valueOf(++i), p)
        }
        ((Neo4jDatastore)datastore).graphDatabaseService.execute(queryString, paramsMap)
    }

    /**
     * perform a cypher query
     * @param queryString
     * @return
     */
    Result cypher(instance, String queryString) {
        ((Neo4jDatastore)datastore).graphDatabaseService.execute(queryString, Collections.singletonMap("this", instance.id))
    }

}
