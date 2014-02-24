package org.grails.datastore.gorm.neo4j.engine

/**
 * abstraction to use cypher via embedded and remote the same way
 */
public interface CypherEngine {

    CypherResult execute(String cypher, Map params)
    CypherResult execute(String cypher)

}