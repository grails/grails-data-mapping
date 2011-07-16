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

import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.index.Index
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.datastore.mapping.engine.PropertyValueIndexer
import org.springframework.datastore.mapping.model.PersistentProperty

class Neo4jPropertyValueIndexer implements PropertyValueIndexer<Long> {

    protected final Logger log = LoggerFactory.getLogger(getClass())

    PersistentProperty persistentProperty
    GraphDatabaseService graphDatabaseService

    void index(value, Long primaryKey) {
        if (!value) {
            log.info "skipping indexing property $persistentProperty.name for node $primaryKey since value = $value"
            return
        }

        log.info "index property $persistentProperty.name for node $primaryKey value $value"
        Index<Node> index = graphDatabaseService.index().forNodes(persistentProperty.owner.name)
        Node node = graphDatabaseService.getNodeById(primaryKey)
        index.remove(node, persistentProperty.name)
        index.add(node, persistentProperty.name, value)
    }

    List<Long> query(value) {
        throw new UnsupportedOperationException() // TODO: implement me
    }

    List<Long> query(value, int offset, int max) {
        throw new UnsupportedOperationException() // TODO: implement me
    }

    String getIndexName(value) {
        throw new UnsupportedOperationException() // TODO: implement me
    }

    void deindex(value, Long primaryKey) {
        Index<Node> index = graphDatabaseService.index().forNodes(persistentProperty.owner.name)
        Node node = graphDatabaseService.getNodeById(primaryKey)
        index.remove(node, persistentProperty.name, value)
    }
}
