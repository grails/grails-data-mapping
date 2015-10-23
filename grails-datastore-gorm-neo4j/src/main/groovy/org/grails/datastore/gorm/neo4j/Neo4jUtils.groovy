package org.grails.datastore.gorm.neo4j

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.datastore.mapping.model.MappingContext
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Label
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.Relationship
import org.neo4j.visualization.graphviz.GraphvizWriter
import org.neo4j.walk.Walker
import org.slf4j.Logger

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
    static void logNode(Node node, Logger logger = log) {
        logger.warn "Node $node.id: $node"
        node.propertyKeys.each { String it ->
            logger.warn "Node $node.id property $it -> ${node.getProperty(it,null)}"
        }
        for(Label label in node.labels) {
            logger.warn "Node $node.id label [${label.name()}]"
        }
        node.relationships.each { Relationship it ->
            logger.warn "Node $node.id relationship $it.startNode -> $it.endNode : ${it.type.name()}"
        }
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
