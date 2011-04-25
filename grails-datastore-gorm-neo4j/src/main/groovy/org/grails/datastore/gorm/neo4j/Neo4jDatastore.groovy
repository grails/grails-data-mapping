package org.grails.datastore.gorm.neo4j

import org.springframework.datastore.mapping.core.AbstractDatastore
import org.springframework.datastore.mapping.core.Session
import org.springframework.beans.factory.InitializingBean
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.kernel.EmbeddedGraphDatabase
import org.neo4j.graphdb.Transaction

/**
 * Created by IntelliJ IDEA.
 * User: stefan
 * Date: 25.04.11
 * Time: 12:23
 * To change this template use File | Settings | File Templates.
 */
class Neo4jDatastore extends AbstractDatastore implements InitializingBean {

    GraphDatabaseService graphDatabaseService
    String storeDir
    Transaction transaction

    public Neo4jDatastore() {
        this.mappingContext = new Neo4jMappingContext()
        initializeConverters(mappingContext)
    }

    @Override
    protected Session createSession(Map<String, String> connectionDetails) {
        transaction = graphDatabaseService.beginTx()  // TODO: not the right place for transaction handling
        new Neo4jSession(this, mappingContext, applicationEventPublisher)
    }

    void afterPropertiesSet() {
        def directory = storeDir
        if (!directory) {
            directory = File.createTempFile("neo4j",null)
            assert directory.delete()
            assert directory.mkdir()
            // directory.deleteOnExit()
            directory = directory.path
        }
        graphDatabaseService = new EmbeddedGraphDatabase(directory)
        //To change body of implemented methods use File | Settings | File Templates.
    }
}

