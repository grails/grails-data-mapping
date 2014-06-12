package org.grails.datastore.gorm.neo4j.engine;

import java.util.Map;

/**
 * for now just a placeholder for using http-builder
 */
public class RemoteCypherEngine implements CypherEngine {

    // TODO: implement me
    @Override
    public CypherResult execute(String cypher, Map params) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CypherResult execute(String cypher) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void beginTx() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void commit() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void rollback() {
        throw new UnsupportedOperationException();
    }

}
