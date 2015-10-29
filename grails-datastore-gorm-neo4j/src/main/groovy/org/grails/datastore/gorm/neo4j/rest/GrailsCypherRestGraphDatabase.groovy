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
package org.grails.datastore.gorm.neo4j.rest

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.springframework.data.neo4j.conversion.Result
import org.springframework.data.neo4j.rest.SpringCypherRestGraphDatabase
import org.springframework.data.neo4j.support.query.CypherQueryEngine


/**
 * @author Graeme Rocher
 * @since 5.0
 */
@InheritConstructors
@CompileStatic
class GrailsCypherRestGraphDatabase extends SpringCypherRestGraphDatabase {

    CypherQueryEngine queryEngine


    @Override
    org.neo4j.graphdb.Result execute(String s) {

        Result<Map<String,Object>> result = queryEngine().query(s, Collections.<String, Object> emptyMap())
        return createQueryResult(result)
    }

    @Override
    org.neo4j.graphdb.Result execute(String s, Map<String, Object> map) {
        Result<Map<String,Object>> result = queryEngine().query(s, map)
        return createQueryResult(result)
    }

    protected org.neo4j.graphdb.Result createQueryResult(Result<Map<String, Object>> result) {
        def iterator = result.iterator()
        List<String,Object> columns = null
        return [
                columns: { ->
                    if(columns == null) {
                        throw new IllegalStateException("Only call columns after starting iteration!")
                    }
                    return columns
                },
                hasNext: { -> iterator.hasNext() },
                next: { ->
                    def obj = iterator.next()
                    if(columns == null) {
                        columns = obj.keySet().toList()
                    }
                    return obj
                },
                close  : { -> result.finish() }
        ] as org.neo4j.graphdb.Result
    }

    @Override
    CypherQueryEngine queryEngine() {
        if(queryEngine == null) {
            queryEngine = super.queryEngine()
        }
        return queryEngine
    }
}
