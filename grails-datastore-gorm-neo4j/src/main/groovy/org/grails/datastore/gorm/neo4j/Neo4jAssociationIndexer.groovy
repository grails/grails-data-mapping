package org.grails.datastore.gorm.neo4j

import org.springframework.datastore.mapping.engine.AssociationIndexer
import org.springframework.datastore.mapping.model.PersistentEntity
import org.springframework.datastore.mapping.model.types.Association
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.apache.commons.lang.NotImplementedException
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.DynamicRelationshipType
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Direction
import org.neo4j.graphdb.Relationship

/**
 * Created by IntelliJ IDEA.
 * User: stefan
 * Date: 26.04.11
 * Time: 14:45
 * To change this template use File | Settings | File Templates.
 */
class Neo4jAssociationIndexer implements AssociationIndexer {

    private static final Logger LOG = LoggerFactory.getLogger(Neo4jAssociationIndexer.class);

    Node nativeEntry
    Association association
	GraphDatabaseService graphDatabaseService

    void index(Object primaryKey, List foreignKeys) {
        assert nativeEntry.id == primaryKey

        foreignKeys.each {
            index(primaryKey, it)
        }
    }

    List query(Object primaryKey) {
        def ids = nativeEntry.getRelationships(relationshipType)
            .collect { it.getOtherNode(nativeEntry).id}
	    LOG.info("query $primaryKey: $ids")
	    ids
    }

    PersistentEntity getIndexedEntity() {
        association.associatedEntity
    }

    void index(Object primaryKey, Object foreignKey) {
        if (primaryKey!=foreignKey) {
            def keyOfOtherNode = (primaryKey == nativeEntry.id) ? foreignKey : primaryKey
            def target = graphDatabaseService.getNodeById(keyOfOtherNode)

            def hasRelationship = nativeEntry.getRelationships(relationshipType, Direction.OUTGOING).any { it.endNode == target}
            if (!hasRelationship) {
                def rel = nativeEntry.createRelationshipTo(target, relationshipType)
                LOG.warn("createRelationship $rel.startNode.id -> $rel.endNode.id ($rel.type)")

            }
        }
    }

    def getRelationshipType() {
        return DynamicRelationshipType.withName(association.name)
    }
}
