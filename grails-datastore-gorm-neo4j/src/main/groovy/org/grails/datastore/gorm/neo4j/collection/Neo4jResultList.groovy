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
package org.grails.datastore.gorm.neo4j.collection

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.neo4j.CypherBuilder
import org.grails.datastore.gorm.neo4j.engine.Neo4jEntityPersister
import org.grails.datastore.gorm.query.AbstractResultList
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.query.QueryException
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.Result

import javax.persistence.LockModeType


/**
 * A Neo4j result list for decoding objects from the {@link Result} interface
 *
 * @author Graeme Rocher
 * @since 5.0
 */
@CompileStatic
class Neo4jResultList extends AbstractResultList {

    private static final Map<Association, Object> EMPTY_ASSOCIATIONS = Collections.<Association, Object> emptyMap()
    private static final Map<String, Object> EMPTY_RESULT_DATA = Collections.<String, Object> emptyMap()

    final protected transient  Neo4jEntityPersister entityPersister;

    protected transient Map<Association, Object> initializedAssociations = EMPTY_ASSOCIATIONS

    protected final LockModeType lockMode

    Neo4jResultList(int offset, Result cursor, Neo4jEntityPersister entityPersister, LockModeType lockMode = LockModeType.NONE) {
        super(offset, (Iterator<Object>)cursor)
        this.entityPersister = entityPersister
        this.lockMode = lockMode;
    }

    Neo4jResultList(int offset, Iterator<Object> cursor, Neo4jEntityPersister entityPersister) {
        super(offset, cursor)
        this.entityPersister = entityPersister
        this.lockMode = LockModeType.NONE;
    }

    Neo4jResultList(int offset, Integer size, Iterator<Object> cursor, Neo4jEntityPersister entityPersister) {
        super(offset, size, cursor)
        this.entityPersister = entityPersister
        this.lockMode = LockModeType.NONE;
    }

    /**
     * Set any already initialized associations to avoid extra proxy queries
     *
     * @param initializedAssociations
     */
    void setInitializedAssociations(Map<Association, Object> initializedAssociations) {
        this.initializedAssociations = initializedAssociations
    }

    @Override
    protected Object nextDecoded() {
        return nextDecodedInternal()
    }

    private Object nextDecodedInternal() {
        def next = cursor.next()
        if (next instanceof Node) {
            Node node = (Node) next
            return entityPersister.unmarshallOrFromCache(entityPersister.getPersistentEntity(), node, EMPTY_RESULT_DATA, initializedAssociations, lockMode)
        } else {
            Map<String, Object> map = (Map<String, Object>) next
            if (map.containsKey(CypherBuilder.NODE_DATA)) {
                Node data = (Node) map.get(CypherBuilder.NODE_DATA);
                return entityPersister.unmarshallOrFromCache(entityPersister.getPersistentEntity(), data, map, initializedAssociations, lockMode)
            } else {
                Node node = (Node) map.values().find() { it instanceof Node }
                if (node != null) {
                    return entityPersister.unmarshallOrFromCache(entityPersister.getPersistentEntity(), node, map, initializedAssociations, lockMode)
                } else {
                    throw new QueryException("Query must return a node as the first column of the RETURN statement")
                }
            }
        }
    }


    @Override
    void close() throws IOException {
        if(cursor instanceof Result) {
            ((Result)cursor).close()
        }
    }
}
