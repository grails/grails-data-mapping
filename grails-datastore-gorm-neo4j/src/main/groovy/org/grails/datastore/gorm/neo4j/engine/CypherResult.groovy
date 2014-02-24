package org.grails.datastore.gorm.neo4j.engine

import groovy.transform.CompileStatic
import org.neo4j.graphdb.ResourceIterable

/**
 * abstraction to use cypher via embedded and remote the same way
 */
@CompileStatic
interface CypherResult extends ResourceIterable<Map<String, Object>>{

    List<String> getColumns()

}
