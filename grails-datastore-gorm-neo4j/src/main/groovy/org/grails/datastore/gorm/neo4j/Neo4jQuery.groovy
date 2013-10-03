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
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.OneToOne
import org.grails.datastore.mapping.model.types.Simple
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
        executionEngine.execute("""MATCH (n:$entity.discriminator)
RETURN id(n),${entity.persistentPropertyNames.collect { "n.$it as $it"}.join(",")}
""").collect { Map map ->
            def domainObject = entity.javaClass.newInstance()
            for (PersistentProperty property  in entity.persistentProperties) {

                switch (property) {
                    case Simple:
                        domainObject[property.name] = map[property.name]
                        log.error "simple property $property.name"
                        break

                    case OneToOne:
                        log.error "property $property.name is of type ${property.class.superclass}"
                        break

                    default:
                        throw new IllegalArgumentException("property $property.name is of type ${property.class.superclass}")

                }
            }

            domainObject
        }

    }

    ExecutionEngine getExecutionEngine() {
        session.nativeInterface as ExecutionEngine
    }
}


