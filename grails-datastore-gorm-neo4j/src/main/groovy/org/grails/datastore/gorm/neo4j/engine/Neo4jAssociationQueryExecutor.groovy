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
package org.grails.datastore.gorm.neo4j.engine

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.neo4j.CypherBuilder
import org.grails.datastore.gorm.neo4j.GraphPersistentEntity
import org.grails.datastore.gorm.neo4j.Neo4jSession
import org.grails.datastore.gorm.neo4j.collection.Neo4jResultList
import org.grails.datastore.mapping.engine.AssociationQueryExecutor
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.config.GormProperties
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.ToOne
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Result


/**
 * Responsible for lazy loading associations
 *
 * @author Graeme Rocher
 * @since 5.0
 */
@CompileStatic
@Slf4j
class Neo4jAssociationQueryExecutor implements AssociationQueryExecutor<Serializable, Object> {

    final Neo4jSession session
    final PersistentEntity indexedEntity
    final Association association
    final boolean lazy
    final boolean singleResult

    Neo4jAssociationQueryExecutor(Neo4jSession session, Association association, boolean lazy = false, boolean singleResult = false) {
        this.session = session
        this.indexedEntity = association.associatedEntity
        this.association = association
        this.lazy = lazy
        this.singleResult = singleResult
    }

    @Override
    boolean doesReturnKeys() {
        return false
    }

    @Override
    List<Object> query(Serializable primaryKey) {

        GraphDatabaseService graphDatabaseService = (GraphDatabaseService)session.nativeInterface
        def relType = Neo4jQuery.matchForAssociation(association)
        GraphPersistentEntity parent = (GraphPersistentEntity)association.owner
        GraphPersistentEntity related = (GraphPersistentEntity)indexedEntity
        String relationship = "(from${parent.labelsAsString})${relType}(to${related.labelsAsString})"

        StringBuilder cypher = new StringBuilder("MATCH $relationship WHERE ")

        if(parent.idGenerator == null) {
            cypher.append("ID(from) = {id} RETURN ")
        }
        else {
            cypher.append("from.${CypherBuilder.IDENTIFIER} = {id}  RETURN ")
        }

        if(lazy) {
            if(related.idGenerator == null) {
                cypher.append("ID(to) as id")
            }
            else {
                cypher.append("to.${CypherBuilder.IDENTIFIER} as id")
            }
        }
        else {
            cypher.append('to as data')
        }
        cypher.append(singleResult ? 'LIMIT 1' : '')


        Map<String,Object> params = (Map<String,Object>)Collections.singletonMap(GormProperties.IDENTITY, primaryKey)


        log.debug("Lazy loading association [${association}] using relationship $relationship")
        log.debug("QUERY Cypher [$cypher] for params [$params]")

        Result result = graphDatabaseService.execute(cypher.toString(), params)
        if(isLazy()) {
            List<Object> results = []
            while(result.hasNext()) {
                def id = result.next().get(GormProperties.IDENTITY)
                if(id instanceof Long) {
                    results.add( session.proxy(related.javaClass, id) )
                }
            }
            return results
        }
        else {
            def resultList = new Neo4jResultList(0, result, session.getEntityPersister(related))
            if(association.isBidirectional()) {
                def inverseSide = association.inverseSide
                if(inverseSide instanceof ToOne) {
                    def parentObject = session.getCachedInstance(association.getOwner().getJavaClass(), primaryKey)
                    if(parentObject != null) {
                        resultList.setInitializedAssociations(Collections.<Association,Object>singletonMap(inverseSide, parentObject))
                    }
                }
            }
            return resultList
        }
    }

}
