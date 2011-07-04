package org.grails.datastore.gorm.neo4j

import org.neo4j.graphdb.Direction
import org.neo4j.graphdb.DynamicRelationshipType
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Relationship
import org.neo4j.graphdb.Node
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.datastore.mapping.engine.AssociationIndexer
import org.springframework.datastore.mapping.model.PersistentEntity
import org.springframework.datastore.mapping.model.types.Association
import org.springframework.datastore.mapping.model.types.ManyToMany

/**
 * Created by IntelliJ IDEA.
 * User: stefan
 * Date: 26.04.11
 * Time: 14:45
 * To change this template use File | Settings | File Templates.
 */
class Neo4jAssociationIndexer implements AssociationIndexer {

    private static final Logger log = LoggerFactory.getLogger(Neo4jAssociationIndexer.class);

    Node nativeEntry
    Association association
	GraphDatabaseService graphDatabaseService

    void index(Object primaryKey, List foreignKeys) {
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
        for (def fk in foreignKeys) {
            index(primaryKey, fk)
        }
    }

    List query(Object primaryKey) {

        def direction = Direction.OUTGOING
        def relType = relationshipType
        if ((association instanceof ManyToMany) && (!association.owningSide)) {
            direction = Direction.INCOMING
            relType = inverseRelationshipType
        }

        def ids = nativeEntry.getRelationships(relType, direction).collect {
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

    void index(Object primaryKey, Object foreignKey) {
        log.info "index $primaryKey, $foreignKey, bidi: $association.bidirectional, own: $association.owningSide"
        if (primaryKey!=foreignKey) {

            def startNode = graphDatabaseService.getNodeById(primaryKey)
            def endNode = graphDatabaseService.getNodeById(foreignKey)
            def relType = relationshipType

            if ((association instanceof ManyToMany) && (!association.owningSide)) {
                (startNode, endNode) = [endNode, startNode]
                relType = inverseRelationshipType
            }

            def hasRelationship = startNode.getRelationships(relType, Direction.OUTGOING).any { it.endNode == endNode }
            if (!hasRelationship) {
                def rel = startNode.createRelationshipTo(endNode, relType)
                log.info("createRelationship $rel.startNode.id -> $rel.endNode.id ($rel.type)")
                //dumpNode(startNode)
            }

        } else {
            log.warn "selfreferecing is not yet supported"
        }
    }

    def getRelationshipType() {
        DynamicRelationshipType.withName(association.name)
    }

    def getInverseRelationshipType() {
        DynamicRelationshipType.withName(association.inversePropertyName)
    }

    private dumpNode(Node node) {
        log.debug ("Node $node.id: $node")
        node.propertyKeys.each {
            log.debug "Node $node.id property $it -> ${node.getProperty(it,null)}"
        }
        node.relationships.each {
            log.debug "Node $node.id relationship $it.startNode -> $it.endNode : ${it.type.name()}"
        }
    }
}
