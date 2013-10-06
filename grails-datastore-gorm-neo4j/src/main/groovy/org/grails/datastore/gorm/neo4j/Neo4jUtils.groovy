package org.grails.datastore.gorm.neo4j

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.types.*
import org.neo4j.graphdb.Direction
import org.neo4j.graphdb.DynamicRelationshipType
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.Relationship

/**
 * Collection of static util methods regarding Neo4j
 */
@Slf4j
@CompileStatic
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
        association.name
    }

    /**
     * dump a given node with all properties and relationships
     * @param node
     * @param logger
     */
    static void dumpNode(Node node, logger = null) {
        log.warn "Node $node.id: $node"
        node.propertyKeys.each { String it ->
            log.warn "Node $node.id property $it -> ${node.getProperty(it,null)}"
        }
        node.relationships.each { Relationship it ->
            log.warn "Node $node.id relationship $it.startNode -> $it.endNode : ${it.type.name()}"
        }
    }

    /**
     * check if a given node is valid for a given class
     * @param node
     * @param persistentProperty
     * @return
     */
    /*static boolean doesNodeMatchType(Node node, Class clazz) {
        try {
            def nodeClass = ClassUtils.forName(node.getProperty(Neo4jSession.TYPE_PROPERTY_NAME, null))
            clazz.isAssignableFrom(nodeClass)
        } catch (ClassNotFoundException e) {
            false
        }
    }*/

    static def cypherReturnColumnsForType(PersistentEntity entity) {
        "id(n) as id,${entity.persistentPropertyNames.collect { "n.$it as $it" }.join(",")}"
    }


    static def mapToAllowedNeo4jType(Object value, MappingContext mappingContext) {
        switch (value.class) {
            case String:
            case Long:
            case Float:
            case Integer:
            case Double:
            case Short:
            case Byte:
            case Boolean:
            case byte:
            case short:
            case int:
            case long:
            case float:
            case double:
            case boolean :
            case byte[]:
            case int[]:
            case short[]:
            case long[]:
            case float[]:
            case double[]:
            case boolean[]:
            case String[]:
                //pass
                break

            default:
                log.info "non special type ${value.class}"

                def conversionService = mappingContext.conversionService
                if (conversionService.canConvert(value.class, long)) {
                    value = conversionService.convert(value, long)
                } else if (conversionService.canConvert(value.class, double)) {
                    value = conversionService.convert(value, double)
                } else if (conversionService.canConvert(value.class, String)) {
                    value = conversionService.convert(value, String)
                } else {
                    value = value.toString()
                    //throw new IllegalArgumentException("cannot convert ${value.class} to long or String")
                }
        }
        value
    }


}
