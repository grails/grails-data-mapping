package org.grails.datastore.gorm.neo4j.engine

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.neo4j.cypher.javacompat.ExecutionResult
import org.neo4j.graphdb.ResourceIterator

/**
 * implementation based on a {@link org.neo4j.cypher.javacompat.ExecutionEngine}
 *
 * while iterating over the result a returned node is converted to a map of its properties
 */
@Slf4j
@CompileStatic
class EmbeddedCypherResult implements CypherResult {

    //@Delegate
    ExecutionResult executionResult

    EmbeddedCypherResult(ExecutionResult executionResult) {
        this.executionResult = executionResult
    }

    // TODO: check why @Delegate and @CompileStatic show an error if interface method is missing


    @Override
    List<String> getColumns() {
        executionResult.columns()
    }

    @Override
    ResourceIterator<Map<String, Object>> iterator() {
        new NodeToMapConvertingIterator(executionResult.iterator())
    }
}
