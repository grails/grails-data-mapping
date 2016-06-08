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

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormEntity
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.neo4j.Neo4jDatastore
import org.grails.datastore.gorm.neo4j.Neo4jSession
import org.grails.datastore.gorm.neo4j.collection.Neo4jResultList
import org.grails.datastore.gorm.neo4j.engine.Neo4jEntityPersister
import org.grails.datastore.gorm.neo4j.extensions.Neo4jExtensions
import org.grails.datastore.gorm.schemaless.DynamicAttributeHelper
import org.grails.datastore.gorm.schemaless.DynamicAttributes
import org.grails.datastore.mapping.core.AbstractDatastore
import org.neo4j.driver.v1.StatementResult
import org.neo4j.driver.v1.StatementRunner

/**
 * Extends the default {@org.grails.datastore.gorm.GormEntity} trait, adding new methods specific to Neo4j
 *
 * @author Graeme Rocher
 * @since 5.0
 */
@CompileStatic
trait Neo4jEntity<D> implements GormEntity<D>, DynamicAttributes {
    @Override
    void putAt(String name, Object val) {
        def old = DynamicAttributes.super.getAt(name)
        DynamicAttributes.super.putAt(name, val)
        if(old != val) {
            GormStaticApi staticApi = GormEnhancer.findStaticApi(getClass())
            if(val instanceof Neo4jEntity) {
                ((Neo4jEntity)val).save()
            } else if (Neo4jSession.isCollectionWithPersistentEntities(val, staticApi.getGormPersistentEntity().getMappingContext())) {
                staticApi.saveAll((Iterable)val)
            }
        }
    }


    /**
     * Allows accessing to dynamic properties with the dot operator
     *
     * @param instance The instance
     * @param name The property name
     * @return The property value
     */
    def propertyMissing(String name) {
        DynamicAttributes.super.getAt(name)
    }


    /**
     * Allows setting a dynamic property via the dot operator
     * @param instance The instance
     * @param name The property name
     * @param val The value
     */
    def propertyMissing(String name, val) {
        putAt(name, val)
    }

    /**
     * perform a cypher query
     * @param queryString
     * @param params
     * @return
     */
    StatementResult cypher(String queryString, Map params ) {
        def session = (Neo4jSession)AbstractDatastore.retrieveSession(Neo4jDatastore)
        StatementRunner boltSession = getStatementRunner(session)

        params['this'] = session.getObjectIdentifier(this)
        boltSession.run(queryString, (Map<String,Object>)params)
    }

    /**
     * perform a cypher query
     * @param queryString
     * @param params
     * @return
     */
    StatementResult cypher(String queryString, List params ) {
        def session = (Neo4jSession)AbstractDatastore.retrieveSession(Neo4jDatastore)
        StatementRunner boltSession = getStatementRunner(session)

        Map<String,Object> paramsMap = new LinkedHashMap()
        paramsMap.put("this", session.getObjectIdentifier(this))
        int i = 0
        for(p in params) {
            paramsMap.put(String.valueOf(++i), p)
        }
        boltSession.run(queryString, paramsMap)
    }

    /**
     * perform a cypher query
     * @param queryString
     * @return
     */
    StatementResult cypher(String queryString) {
        def session = (Neo4jSession)AbstractDatastore.retrieveSession(Neo4jDatastore)
        StatementRunner boltSession = getStatementRunner(session)
        boltSession.run(queryString, (Map<String,Object>)Collections.singletonMap("this", session.getObjectIdentifier(this)))
    }

    /**
     * perform a cypher query
     *
     * @param queryString
     * @return
     */
    static StatementResult cypherStatic(String queryString, Map params ) {
        def session = (Neo4jSession)AbstractDatastore.retrieveSession(Neo4jDatastore)
        StatementRunner boltSession = getStatementRunner(session)
        boltSession.run(queryString, params)
    }

    /**
     * perform a cypher query
     *
     * @param queryString
     * @return
     */
    static StatementResult cypherStatic(String queryString, List params) {
        def session = (Neo4jSession)AbstractDatastore.retrieveSession(Neo4jDatastore)
        Map paramsMap = new LinkedHashMap()
        int i = 0
        for(p in params) {
            paramsMap.put(String.valueOf(++i), p)
        }
        StatementRunner boltSession = getStatementRunner(session)
        boltSession.run(queryString, paramsMap)
    }

    /**
     * perform a cypher query
     *
     * @param queryString
     * @return
     */
    static StatementResult cypherStatic(String queryString) {
        def session = (Neo4jSession)AbstractDatastore.retrieveSession(Neo4jDatastore)

        StatementRunner boltSession = getStatementRunner(session)
        boltSession.run(queryString)
    }

    /**
     * @see {@link #cypherStatic(java.lang.String, java.util.Map)}
     */
    static List<D> executeQuery(String query, Map params = Collections.emptyMap(), Map args = Collections.emptyMap()) {
        def session = AbstractDatastore.retrieveSession(Neo4jDatastore)
        def result = cypherStatic(query, params)
        return new Neo4jResultList(0, result,(Neo4jEntityPersister) session.getPersister(this))
    }

    /**
     * @see {@link #cypherStatic(java.lang.String, java.util.Map)}
     */
    static List<D> executeQuery(String query, Collection params, Map args) {
        def result = cypherStatic(query, params.toList())
        def session = AbstractDatastore.retrieveSession(Neo4jDatastore)
        return new Neo4jResultList(0, result, (Neo4jEntityPersister)session.getPersister(this))
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

        StatementRunner boltSession = getStatementRunner(session)
        StatementResult result = Neo4jExtensions.execute(boltSession, query, (List<Object>)params.toList())
        def persister = session
                .getEntityPersister(this)

        if(result.hasNext()) {
            return new Neo4jResultList(0, result, persister)
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

        StatementRunner boltSession = getStatementRunner(session)
        StatementResult result = Neo4jExtensions.execute(boltSession, query, Arrays.asList(params))
        def persister = session
                .getEntityPersister(this)

        if(result.hasNext()) {
            return new Neo4jResultList(0, result, persister)
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

        StatementRunner boltSession = getStatementRunner(session)
        StatementResult result = boltSession.run( query, (Map<String,Object>)params)
        def persister = session
                .getEntityPersister(this)

        if(result.hasNext()) {
            return new Neo4jResultList(0, result, persister)
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

        StatementRunner boltSession = getStatementRunner(session)
        StatementResult result = Neo4jExtensions.execute(boltSession, query, (List<Object>)params.toList())

        def persister = session
                .getEntityPersister(this)

        def resultList = new Neo4jResultList(0, 1, (Iterator<Object>)result, persister)
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

        StatementRunner boltSession = getStatementRunner(session)
        StatementResult result = boltSession.run( query, (Map<String,Object>)params)
        def persister = session
                .getEntityPersister(this)

        def resultList = new Neo4jResultList(0, 1, (Iterator<Object>)result, persister)
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

        StatementRunner boltSession = getStatementRunner(session)
        StatementResult result = Neo4jExtensions.execute(boltSession, query, Arrays.asList(params))

        def persister = session
                .getEntityPersister(this)

        def resultList = new Neo4jResultList(0, 1, (Iterator<Object>)result, persister)
        if( !resultList.isEmpty() ) {
            return (D)resultList.get(0)
        }
        return null
    }

    private static StatementRunner getStatementRunner(Neo4jSession session) {
        return session.hasTransaction() ? session.getTransaction().getNativeTransaction() : session.getNativeInterface()
    }
}