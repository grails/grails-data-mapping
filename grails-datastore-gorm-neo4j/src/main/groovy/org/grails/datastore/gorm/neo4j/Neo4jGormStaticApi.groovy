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

    def cypherStatic(String queryString, Map params) {
        ((Neo4jDatastore)datastore).graphDatabaseService.execute(queryString, params)
    }

    def cypherStatic(String queryString, List params) {
        Map paramsMap = new LinkedHashMap()
        int i = 0
        for(p in params) {
            paramsMap.put(String.valueOf(++i), p)
        }
        ((Neo4jDatastore)datastore).graphDatabaseService.execute(queryString, paramsMap)
    }

    def cypherStatic(String queryString) {
        ((Neo4jDatastore)datastore).graphDatabaseService.execute(queryString)
    }

}
