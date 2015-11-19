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
package grails.neo4j

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.GormEntity
import org.grails.datastore.gorm.neo4j.Neo4jDatastore
import org.grails.datastore.gorm.neo4j.Neo4jSession
import org.grails.datastore.gorm.neo4j.collection.Neo4jResultList
import org.grails.datastore.gorm.neo4j.engine.Neo4jEntityPersister
import org.grails.datastore.gorm.neo4j.engine.Neo4jQuery
import org.grails.datastore.gorm.neo4j.extensions.Neo4jExtensions
import org.grails.datastore.mapping.core.AbstractDatastore
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Result


/**
 * Extends the default {@org.grails.datastore.gorm.GormEntity} trait, adding new methods specific to Neo4j
 *
 * @author Graeme Rocher
 * @since 5.0
 */
@CompileStatic
trait Neo4jEntity<D> extends GormEntity<D> {
    /**
     * @return Returns all dynamic attributes
     */
    Map<String, Object> dynamicAttributes() {
        def unwrappedInstance = unwrappedInstance(this)
        def session = AbstractDatastore.retrieveSession(Neo4jDatastore)
        return (Map<String,Object>)getOrInitializeUndeclared(session, unwrappedInstance)
    }

    /**
     * Allows accessing to dynamic properties with the dot operator
     *
     * @param instance The instance
     * @param name The property name
     * @return The property value
     */
    def propertyMissing(String name) {
        def unwrappedInstance = unwrappedInstance(this)
        def session = AbstractDatastore.retrieveSession(Neo4jDatastore)
        def undeclared = getOrInitializeUndeclared(session, unwrappedInstance)

        return undeclared.get(name)
    }

    /**
     * dealing with undeclared properties must not happen on proxied instances
     * @param instance
     * @return the unwrapped instance
     */
    private D unwrappedInstance(D instance) {
        def session = AbstractDatastore.retrieveSession(Neo4jDatastore)
        def proxyFactory = session.mappingContext.proxyFactory
        return (D)proxyFactory.unwrap(instance)
    }

    /**
     * Allows setting a dynamic property via the dot operator
     * @param instance The instance
     * @param name The property name
     * @param val The value
     */
    def propertyMissing(String name, val) {
        def unwrappedInstance = unwrappedInstance(this)
        def session = AbstractDatastore.retrieveSession(Neo4jDatastore)
        if (name == Neo4jQuery.UNDECLARED_PROPERTIES) {
            return getOrInitializeUndeclared(session, unwrappedInstance)
        } else {

            Map undeclaredProps = getOrInitializeUndeclared(session, unwrappedInstance)
            undeclaredProps.put(name, val)

            def mappingContext = session.mappingContext
            if (mappingContext.isPersistentEntity(val)) {
                session.persist(val)
            } else if (Neo4jSession.isCollectionWithPersistentEntities(val, mappingContext)) {
                session.persist((Iterable)val)
            }
            if (unwrappedInstance instanceof DirtyCheckable) {
                ((DirtyCheckable)unwrappedInstance).markDirty(name)
            }
        }
    }

    @CompileStatic
    private Map getOrInitializeUndeclared(Session session, D instance) {
        Map undeclaredProps = (Map) session.getAttribute(instance, Neo4jQuery.UNDECLARED_PROPERTIES)
        if (undeclaredProps == null) {
            undeclaredProps = [:]
            session.setAttribute(instance, Neo4jQuery.UNDECLARED_PROPERTIES, undeclaredProps)
        }
        undeclaredProps
    }

    /**
     * Allows subscript access to schemaless attributes.
     *
     * @param instance The instance
     * @param name The name of the field
     */
    void putAt(String name, value) {
        ((GroovyObject)this).setProperty(name, value)
    }

    /**
     * Allows subscript access to schemaless attributes.
     *
     * @param instance The instance
     * @param name The name of the field
     * @return the value
     */
    def getAt(String name) {
        ((GroovyObject)this).getProperty(name)
    }

    /**
     * perform a cypher query
     * @param queryString
     * @param params
     * @return
     */
    Result cypher(String queryString, Map params ) {
        def session = AbstractDatastore.retrieveSession(Neo4jDatastore)
        params['this'] = session.getObjectIdentifier(this)
        def graphDatabaseService = (GraphDatabaseService)session.nativeInterface
        graphDatabaseService.execute(queryString, (Map<String,Object>)params)
    }

    /**
     * perform a cypher query
     * @param queryString
     * @param params
     * @return
     */
    Result cypher(String queryString, List params ) {
        def session = AbstractDatastore.retrieveSession(Neo4jDatastore)
        Map<String,Object> paramsMap = new LinkedHashMap()
        paramsMap.put("this", session.getObjectIdentifier(this))
        int i = 0
        for(p in params) {
            paramsMap.put(String.valueOf(++i), p)
        }
        def graphDatabaseService = (GraphDatabaseService)session.nativeInterface
        graphDatabaseService.execute(queryString, paramsMap)
    }

    /**
     * perform a cypher query
     * @param queryString
     * @return
     */
    Result cypher(String queryString) {
        def session = AbstractDatastore.retrieveSession(Neo4jDatastore)
        def graphDatabaseService = (GraphDatabaseService)session.nativeInterface
        graphDatabaseService.execute(queryString, (Map<String,Object>)Collections.singletonMap("this", session.getObjectIdentifier(this)))
    }

    /**
     * perform a cypher query
     *
     * @param queryString
     * @return
     */
    static Result cypherStatic(String queryString, Map params ) {
        def session = AbstractDatastore.retrieveSession(Neo4jDatastore)
        def graphDatabaseService = (GraphDatabaseService)session.nativeInterface
        graphDatabaseService.execute(queryString, params)
    }

    /**
     * perform a cypher query
     *
     * @param queryString
     * @return
     */
    static Result cypherStatic(String queryString, List params) {
        def session = AbstractDatastore.retrieveSession(Neo4jDatastore)
        Map paramsMap = new LinkedHashMap()
        int i = 0
        for(p in params) {
            paramsMap.put(String.valueOf(++i), p)
        }
        def graphDatabaseService = (GraphDatabaseService)session.nativeInterface
        graphDatabaseService.execute(queryString, paramsMap)
    }

    /**
     * perform a cypher query
     *
     * @param queryString
     * @return
     */
    static Result cypherStatic(String queryString) {
        def session = AbstractDatastore.retrieveSession(Neo4jDatastore)
        def graphDatabaseService = (GraphDatabaseService)session.nativeInterface
        graphDatabaseService.execute(queryString)
    }

    /**
     * @see {@link #cypherStatic(java.lang.String, java.util.Map)}
     */
    static List<D> executeQuery(String query, Map params = Collections.emptyMap(), Map args = Collections.emptyMap()) {
        def session = AbstractDatastore.retrieveSession(Neo4jDatastore)
        def result = cypherStatic(query, params)
        return new Neo4jResultList(0, (Iterator<Object>)result.iterator(),(Neo4jEntityPersister) session.getPersister(this))
    }

    /**
     * @see {@link #cypherStatic(java.lang.String, java.util.Map)}
     */
    static List<D> executeQuery(String query, Collection params, Map args) {
        def result = cypherStatic(query, params.toList())
        def session = AbstractDatastore.retrieveSession(Neo4jDatastore)
        return new Neo4jResultList(0, (Iterator<Object>)result.iterator(), (Neo4jEntityPersister)session.getPersister(this))
    }

    /**
     * Finds all results using the given cypher query, converting each result to a domain instance
     *
     * @param query The cypher query
     * @param params The positional parameters
     * @param args The arguments to the query
     * @return The results
     */
    static List<D> findAll(String query, Collection params, Map args) {
        Neo4jSession session = (Neo4jSession)AbstractDatastore.retrieveSession(Neo4jDatastore)

        GraphDatabaseService graphDatabaseService = (GraphDatabaseService)session.nativeInterface
        Result result = Neo4jExtensions.execute(graphDatabaseService, query, (List<Object>)params.toList())
        def persister = session
                .getEntityPersister(this)

        if(result.hasNext()) {
            return new Neo4jResultList(0, (Iterator<Object>)result.iterator(), persister)
        }
        else {
            return Collections.emptyList()
        }
    }

    /**
     * Varargs version of {@link #findAll(java.lang.String, java.util.Collection, java.util.Map)}
     */
    static List<D> findAll(String query, Object[] params) {
        Neo4jSession session = (Neo4jSession)AbstractDatastore.retrieveSession(Neo4jDatastore)

        GraphDatabaseService graphDatabaseService = (GraphDatabaseService)session.nativeInterface
        Result result = Neo4jExtensions.execute(graphDatabaseService, query, Arrays.asList(params))
        def persister = session
                .getEntityPersister(this)

        if(result.hasNext()) {
            return new Neo4jResultList(0, (Iterator<Object>)result.iterator(), persister)
        }
        else {
            return Collections.emptyList()
        }
    }

    /**
     * Finds all results using the given cypher query, converting each result to a domain instance
     *
     * @param query The cypher query
     * @param params The parameters
     * @param args The arguments to the query
     * @return The results
     */
    static List<D> findAll(String query, Map params = Collections.emptyMap(), Map args = Collections.emptyMap()) {
        Neo4jSession session = (Neo4jSession)AbstractDatastore.retrieveSession(Neo4jDatastore)

        GraphDatabaseService graphDatabaseService = (GraphDatabaseService)session.nativeInterface
        Result result = graphDatabaseService.execute( query, (Map<String,Object>)params)
        def persister = session
                .getEntityPersister(this)

        if(result.hasNext()) {
            return new Neo4jResultList(0, (Iterator<Object>)result.iterator(), persister)
        }
        else {
            return Collections.emptyList()
        }
    }

    /**
     * Finds a single result using the given cypher query, converting the result to a domain instance
     *
     * @param query The cypher query
     * @param params The positional parameters
     * @param args The arguments to the query
     * @return The results
     */
    static D find(String query, Collection params, Map args = Collections.emptyMap()) {
        Neo4jSession session = (Neo4jSession)AbstractDatastore.retrieveSession(Neo4jDatastore)

        GraphDatabaseService graphDatabaseService = (GraphDatabaseService)session.nativeInterface
        Result result = Neo4jExtensions.execute(graphDatabaseService, query, (List<Object>)params.toList())

        def persister = session
                .getEntityPersister(this)

        def resultList = new Neo4jResultList(0, 1, (Iterator<Object>)result.iterator(), persister)
        if( !resultList.isEmpty() ) {
            return (D)resultList.get(0)
        }
        return null
    }

    /**
     * Finds a single result using the given cypher query, converting the result to a domain instance
     *
     * @param query The cypher query
     * @param params The parameters
     * @param args The arguments to the query
     * @return The results
     */
    static D find(String query, Map params = Collections.emptyMap(), Map args = Collections.emptyMap()) {
        Neo4jSession session = (Neo4jSession)AbstractDatastore.retrieveSession(Neo4jDatastore)

        GraphDatabaseService graphDatabaseService = (GraphDatabaseService)session.nativeInterface
        Result result = graphDatabaseService.execute( query, (Map<String,Object>)params)
        def persister = session
                .getEntityPersister(this)

        def resultList = new Neo4jResultList(0, 1, (Iterator<Object>)result.iterator(), persister)
        if( !resultList.isEmpty() ) {
            return (D)resultList.get(0)
        }
        return null
    }

    /**
     * Varargs version of {@link #find(java.lang.String, java.util.Collection, java.util.Map)}
     */
    static D find(String query, Object[] params) {
        Neo4jSession session = (Neo4jSession)AbstractDatastore.retrieveSession(Neo4jDatastore)

        GraphDatabaseService graphDatabaseService = (GraphDatabaseService)session.nativeInterface
        Result result = Neo4jExtensions.execute(graphDatabaseService, query, Arrays.asList(params))

        def persister = session
                .getEntityPersister(this)

        def resultList = new Neo4jResultList(0, 1, (Iterator<Object>)result.iterator(), persister)
        if( !resultList.isEmpty() ) {
            return (D)resultList.get(0)
        }
        return null
    }
}