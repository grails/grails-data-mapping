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
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.gorm.neo4j.engine.Neo4jEntityPersister
import org.grails.datastore.gorm.neo4j.engine.Neo4jResultList
import org.grails.datastore.gorm.neo4j.extensions.Neo4jExtensions
import org.grails.datastore.mapping.core.AbstractDatastore
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.query.QueryException
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.Result
import org.springframework.transaction.PlatformTransactionManager

/**
 * Adds new static methods specific to Neo4j
 *
 * @author Graeme Rocher
 */
@CompileStatic
class Neo4jGormStaticApi<D> extends GormStaticApi<D> {

    Neo4jGormStaticApi(Class<D> persistentClass, Neo4jDatastore datastore, List<FinderMethod> finders, PlatformTransactionManager transactionManager) {
        super(persistentClass, datastore, finders, transactionManager)
    }

    Result cypherStatic(String queryString, Map params) {
        ((Neo4jDatastore)datastore).graphDatabaseService.execute(queryString, params)
    }

    Result cypherStatic(String queryString, List params) {
        Map paramsMap = new LinkedHashMap()
        int i = 0
        for(p in params) {
            paramsMap.put(String.valueOf(++i), p)
        }
        ((Neo4jDatastore)datastore).graphDatabaseService.execute(queryString, paramsMap)
    }

    Result cypherStatic(String queryString) {
        ((Neo4jDatastore)datastore).graphDatabaseService.execute(queryString)
    }

    @Override
    List<D> findAll(String query, Collection params, Map args) {
        Neo4jSession session = (Neo4jSession)AbstractDatastore.retrieveSession(Neo4jDatastore)

        GraphDatabaseService graphDatabaseService = (GraphDatabaseService)session.nativeInterface
        Result result = Neo4jExtensions.execute(graphDatabaseService, query, (List<Object>)params.toList())
        def persister = session
                .getEntityPersister(persistentEntity)

        if(result.hasNext()) {
            return new Neo4jResultList(0, (Iterator<Object>)result.iterator(), persister)
        }
        else {
            return Collections.emptyList()
        }
    }

    @Override
    List<D> findAll(String query, Map params, Map args) {
        Neo4jSession session = (Neo4jSession)AbstractDatastore.retrieveSession(Neo4jDatastore)

        GraphDatabaseService graphDatabaseService = (GraphDatabaseService)session.nativeInterface
        Result result = graphDatabaseService.execute( query, (Map<String,Object>)params)
        def persister = session
                .getEntityPersister(persistentEntity)

        if(result.hasNext()) {
            return new Neo4jResultList(0, (Iterator<Object>)result.iterator(), persister)
        }
        else {
            return Collections.emptyList()
        }
    }

    @Override
    D find(String query, Collection params, Map args) {
        Neo4jSession session = (Neo4jSession)AbstractDatastore.retrieveSession(Neo4jDatastore)

        GraphDatabaseService graphDatabaseService = (GraphDatabaseService)session.nativeInterface
        Result result = Neo4jExtensions.execute(graphDatabaseService, query, (List<Object>)params.toList())

        def persister = session
                .getEntityPersister(persistentEntity)

        def resultList = new Neo4jResultList(0, 1, (Iterator<Object>)result.iterator(), persister)
        if( !resultList.isEmpty() ) {
            return (D)resultList.get(0)
        }
        return null
    }

    @Override
    D find(String query, Map params, Map args) {
        Neo4jSession session = (Neo4jSession)AbstractDatastore.retrieveSession(Neo4jDatastore)

        GraphDatabaseService graphDatabaseService = (GraphDatabaseService)session.nativeInterface
        Result result = graphDatabaseService.execute( query, (Map<String,Object>)params)
        def persister = session
                .getEntityPersister(persistentEntity)

        def resultList = new Neo4jResultList(0, 1, (Iterator<Object>)result.iterator(), persister)
        if( !resultList.isEmpty() ) {
            return (D)resultList.get(0)
        }
        return null
    }
}
