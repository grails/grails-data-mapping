package org.grails.datastore.gorm.neo4j.engine

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.neo4j.graphdb.ResourceIterable

/**
 * for now just a placeholder for using http-builder
 */
@CompileStatic
@Slf4j
class RemoteCypherEngine implements CypherEngine {

    // TODO: implement me
    @Override
    CypherResult execute(String cypher, Map params=[:]) {
        throw new UnsupportedOperationException()
    }

}
