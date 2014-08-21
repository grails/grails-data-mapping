package org.grails.datastore.gorm.neo4j

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.datastore.mapping.model.MappingContext
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.Relationship
import org.neo4j.visualization.graphviz.GraphvizWriter
import org.neo4j.walk.Walker

/**
 * Collection of static util methods regarding Neo4j
 */
@Slf4j
@CompileStatic
abstract class Neo4jUtils {

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
            case boolean:
//            case byte[]:
            case int[]:
            case short[]:
            case long[]:
            case float[]:
            case double[]:
            case boolean[]:
            case String[]:
                //pass
                break
            case Collection:
                break
            case BigDecimal:
                value = ((BigDecimal)value).doubleValue()
                break

            case byte[]:
                value = mappingContext.conversionService.convert(value, String)
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

    static def dumpGraphToSvg(GraphDatabaseService graphDatabaseService) {
        File dotFile = File.createTempFile("temp", ".dot")
        File svgFile = File.createTempFile("temp", ".svg")
        def dotName = "/usr/bin/dot"
        def dot = new File(dotName)
        if (dot.exists() && dot.canExecute()) {
            // TODO: sort properties when emitting.
            new GraphvizWriter().emit(dotFile, Walker.fullGraph(graphDatabaseService))
            def proc = "${dotName} -Tsvg ${dotFile.absolutePath}".execute()
            svgFile.withWriter { Writer it -> it << proc.in.text }
            dotFile.delete()
            svgFile.toURI().toURL()
        } else {
            "cannot find or execute $dotName, consider to install graphviz binaries (apt-get install graphviz)"
        }
    }
}
