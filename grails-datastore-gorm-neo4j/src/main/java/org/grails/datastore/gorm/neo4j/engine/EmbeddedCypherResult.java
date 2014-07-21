package org.grails.datastore.gorm.neo4j.engine;

import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.ResourceIterator;

import java.util.List;
import java.util.Map;

/**
 * implementation based on a {@link org.neo4j.cypher.javacompat.ExecutionEngine}
 *
 * while iterating over the result a returned node is converted to a map of its properties
 * @deprecated due to usage of {@link org.grails.datastore.gorm.neo4j.engine.JdbcCypherResultD}
 */
@Deprecated
public class EmbeddedCypherResult implements CypherResult {

    //@Delegate
    private ExecutionResult executionResult;

    EmbeddedCypherResult(ExecutionResult executionResult) {
        this.executionResult = executionResult;
    }

    // TODO: check why @Delegate and @CompileStatic show an error if interface method is missing


    @Override
    public List<String> getColumns() {
        return executionResult.columns();
    }

    @Override
    public ResourceIterator<Map<String, Object>> iterator() {
        return new NodeToMapConvertingIterator(executionResult.iterator());
    }
}
