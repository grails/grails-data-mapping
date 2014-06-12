package org.grails.datastore.gorm.neo4j.engine;

import java.util.Map;

/**
 * abstraction to use cypher via embedded and remote the same way
 */
public interface CypherEngine {

    public CypherResult execute(String cypher, Map params);
    public CypherResult execute(String cypher);

    public void beginTx();
    public void commit();
    public void rollback();
}