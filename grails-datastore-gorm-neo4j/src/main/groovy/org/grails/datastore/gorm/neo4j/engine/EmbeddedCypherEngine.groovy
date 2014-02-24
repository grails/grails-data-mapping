package org.grails.datastore.gorm.neo4j.engine

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.neo4j.cypher.javacompat.ExecutionEngine

/**
 * CypherEngine implementation backed by {@link ExecutionEngine}
 */
@CompileStatic
@Slf4j
class EmbeddedCypherEngine implements CypherEngine {

    ExecutionEngine executionEngine

    @Override
    CypherResult execute(String cypher, Map params=[:]) {
        log.info "running cypher $cypher"
        log.info "   with params $params"
        new EmbeddedCypherResult(params ?
            executionEngine.execute(cypher, params) :
            executionEngine.execute(cypher)
        )
    }

}
