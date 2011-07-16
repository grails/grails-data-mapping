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

import org.neo4j.graphdb.Direction
import org.neo4j.graphdb.DynamicRelationshipType
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.Relationship
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.datastore.mapping.engine.AssociationIndexer
import org.springframework.datastore.mapping.model.PersistentEntity
import org.springframework.datastore.mapping.model.types.Association
import org.springframework.datastore.mapping.model.types.ManyToMany

/**
 * @author Stefan Armbruster <stefan@armbruster-it.de>
 */
class Neo4jAssociationIndexer implements AssociationIndexer {

    protected final Logger log = LoggerFactory.getLogger(getClass())

    Node nativeEntry
    Association association
    GraphDatabaseService graphDatabaseService

    void index(primaryKey, List foreignKeys) {
        assert nativeEntry.id == primaryKey
        log.info "indexing for $primaryKey : $foreignKeys, $association"
        /*for (Relationship rel in nativeEntry.getRelationships(relationshipType, Direction.OUTGOING)) {
            Long otherId = rel.endNode.id
            if (otherId in foreignKeys) {
                foreignKeys.remove(otherId) // TODO: check if modifying foreignKeys causes side effects
            } else {
                log.info "deleting relationship $rel.startNode -> $rel.endNode : ${rel.type.name()}"
                rel.delete()
            }
        } */
        for (fk in foreignKeys) {
            index(primaryKey, fk)
        }
    }

    List query(primaryKey) {

        Direction direction = Direction.OUTGOING
        DynamicRelationshipType relType = relationshipType
        if ((association instanceof ManyToMany) && (!association.owningSide)) {
            direction = Direction.INCOMING
            relType = inverseRelationshipType
        }

        List<Long> ids = nativeEntry.getRelationships(relType, direction).collect {
            log.debug "relation: $it.startNode -> $it.endNode $it.type"
            it.getOtherNode(nativeEntry).id
        }
        log.info("query $primaryKey: $ids")
        //dumpNode(nativeEntry)
        ids
    }

    PersistentEntity getIndexedEntity() {
        association.associatedEntity
    }

    void index(primaryKey, foreignKey) {
        if (primaryKey == foreignKey) {
            log.warn "self-referencing is not yet supported"
            return
        }

        log.info "index $primaryKey, $foreignKey, bidi: $association.bidirectional, own: $association.owningSide"

        Node startNode = graphDatabaseService.getNodeById(primaryKey)
        Node endNode = graphDatabaseService.getNodeById(foreignKey)
        DynamicRelationshipType relType = relationshipType

        if (association instanceof ManyToMany && !association.owningSide) {
            (startNode, endNode) = [endNode, startNode]
            relType = inverseRelationshipType
        }

        boolean hasRelationship = startNode.getRelationships(relType, Direction.OUTGOING).any { it.endNode == endNode }
        if (!hasRelationship) {
            Relationship rel = startNode.createRelationshipTo(endNode, relType)
            log.info("createRelationship $rel.startNode.id -> $rel.endNode.id ($rel.type)")
            //dumpNode(startNode)
        }
    }

    DynamicRelationshipType getRelationshipType() {
        DynamicRelationshipType.withName(association.name)
    }

    DynamicRelationshipType getInverseRelationshipType() {
        DynamicRelationshipType.withName(association.inversePropertyName)
    }

    protected void dumpNode(Node node) {
        log.debug ("Node $node.id: $node")
        node.propertyKeys.each {
            log.debug "Node $node.id property $it -> ${node.getProperty(it,null)}"
        }
        node.relationships.each {
            log.debug "Node $node.id relationship $it.startNode -> $it.endNode : ${it.type.name()}"
        }
    }
}
