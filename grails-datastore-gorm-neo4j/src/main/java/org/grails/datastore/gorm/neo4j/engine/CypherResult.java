package org.grails.datastore.gorm.neo4j.engine;

import org.neo4j.graphdb.ResourceIterable;

import java.util.List;
import java.util.Map;

/**
 * abstraction to use cypher via embedded and remote the same way
 */
public interface CypherResult extends ResourceIterable<Map<String, Object>>{

    public List<String> getColumns();

}
