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
package org.grails.datastore.gorm.neo4j

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.query.Query
import org.neo4j.cypher.javacompat.ExecutionEngine

/**
 * perform criteria queries on a Neo4j backend
 *
 * @author Stefan Armbruster <stefan@armbruster-it.de>
 */
@CompileStatic
@Slf4j
class Neo4jQuery extends Query {

    protected Neo4jQuery(Session session, PersistentEntity entity) {
        super(session, entity)
    }

    @Override
    protected List executeQuery(PersistentEntity entity, Query.Junction criteria) {

        def returnColumns = Neo4jUtils.cypherReturnColumnsForType(entity)
        executionEngine.execute("""MATCH (n:$entity.discriminator)
RETURN $returnColumns
""").collect { Map map ->
            Neo4jUtils.unmarshall(map, entity)
        }
    }

    ExecutionEngine getExecutionEngine() {
        session.nativeInterface as ExecutionEngine
    }
}


