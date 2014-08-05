package org.grails.datastore.gorm.neo4j.engine;

import java.util.List;
import java.util.Map;

/**
 * abstraction to use cypher via embedded and remote the same way
 */
public interface CypherEngine {

    /**
     * only positional parameters are valid in cypher over jdbc
     * The argument list references them by position starting with 1,
     * just like jdbc's preparedstatements do
     * @param cypher cypher string containing positional parameters e.g.<code>MATCH (n:Person {name:{1}}) RETURN n</code>
     * @param params parameters for the above query
     * @return
     */
    public CypherResult execute(String cypher, List params);
    public CypherResult execute(String cypher);

    public void beginTx();
    public void commit();
    public void rollback();
}