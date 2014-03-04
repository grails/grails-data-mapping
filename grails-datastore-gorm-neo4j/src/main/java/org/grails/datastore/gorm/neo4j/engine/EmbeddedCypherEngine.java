package org.grails.datastore.gorm.neo4j.engine;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * CypherEngine implementation backed by {@link ExecutionEngine}
 */
public class EmbeddedCypherEngine implements CypherEngine {

    private static Logger log = LoggerFactory.getLogger(EmbeddedCypherEngine.class);
    private ExecutionEngine executionEngine;

    @Override
    public CypherResult execute(String cypher, Map params) {
        log.info("running cypher {}", cypher);
        log.info("   with params {}", params);
        return new EmbeddedCypherResult(params != null ?
            executionEngine.execute(cypher, params) :
            executionEngine.execute(cypher)
        );
    }

    @Override
    public CypherResult execute(String cypher) {
        return execute(cypher, null);
    }

}
