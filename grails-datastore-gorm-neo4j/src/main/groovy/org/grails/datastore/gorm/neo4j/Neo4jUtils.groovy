package org.grails.datastore.gorm.neo4j

import org.neo4j.graphdb.Direction
import org.neo4j.graphdb.Node
import org.grails.datastore.mapping.model.types.ManyToMany
import org.neo4j.graphdb.DynamicRelationshipType
import org.grails.datastore.mapping.model.types.Association
import org.slf4j.LoggerFactory
import org.grails.datastore.mapping.model.types.ManyToOne

/**
 * Collection of static util methods regarding Neo4j
 */
abstract class Neo4jUtils {

    /**
     *
     * @return {@link org.neo4j.graphdb.RelationshipType}, {@link org.neo4j.graphdb.Direction}
     */
    static List relationTypeAndDirection(Association association) {
        Direction direction = Direction.OUTGOING
        String relTypeName = relationshipTypeName(association)

        // switch direction and name if we have a bidi and ( (many2many with not owning side) or (onetomany))
        if (association.bidirectional &&
            ((association instanceof ManyToMany && (!association.owningSide)) ||
            association instanceof ManyToOne)) {
                direction = Direction.INCOMING
                relTypeName = relationshipTypeName(association.inverseSide)
        }
        [DynamicRelationshipType.withName(relTypeName), direction ]
    }


    static String relationshipTypeName(Association association) {
        "${association.owner.decapitalizedName}_${association.name}"
    }

    /**
     * dump a given node with all properties and relationships
     * @param node
     * @param logger
     */
    static void dumpNode(Node node, logger = null) {
        logger = logger ?: LoggerFactory.getLogger(Neo4jDatastore.class)
        logger.warn "Node $node.id: $node"
        node.propertyKeys.each {
            logger.warn "Node $node.id property $it -> ${node.getProperty(it,null)}"
        }
        node.relationships.each {
            logger.warn "Node $node.id relationship $it.startNode -> $it.endNode : ${it.type.name()}"
        }
    }

}
