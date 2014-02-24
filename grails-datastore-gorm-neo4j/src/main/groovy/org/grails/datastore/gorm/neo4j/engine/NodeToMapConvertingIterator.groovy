package org.grails.datastore.gorm.neo4j.engine

import groovy.transform.CompileStatic
import org.neo4j.graphdb.ResourceIterator

/**
 * wrap an standard iterator from a ExecutionResult to return maps instead of Node
 */
@CompileStatic
class NodeToMapConvertingIterator implements ResourceIterator<Map<String, Object>> {

//    @Delegate
    ResourceIterator<Map<String, Object>> delegatingIterator

    NodeToMapConvertingIterator(ResourceIterator<Map<String, Object>> delegatingIterator) {
        this.delegatingIterator = delegatingIterator
    }

    @Override
    void close() {
        delegatingIterator.close()
    }

    @Override
    boolean hasNext() {
        delegatingIterator.hasNext()
    }

    @Override
    Map<String, Object> next() {
        Map<String, Object> row = (Map<String, Object>)delegatingIterator.next()
        def result = [:]
        for (Map.Entry<String, Object> entry in row.entrySet()) {
            def value
            if (entry.value instanceof org.neo4j.graphdb.Node) {
                value = [:]
                def node = entry.value as org.neo4j.graphdb.Node
                for (String key in node.getPropertyKeys()) {
                    value[key] = node.getProperty(key)
                }
            } else {
                value = entry.value
            }
            result[entry.key] = value
        }
        return result
    }

    @Override
    void remove() {
        delegatingIterator.remove()
    }

}
