package org.grails.datastore.gorm.neo4j

import org.springframework.datastore.mapping.engine.PropertyValueIndexer
import org.springframework.datastore.mapping.model.PersistentProperty
import org.slf4j.LoggerFactory
import org.slf4j.Logger
import org.neo4j.graphdb.GraphDatabaseService
import org.apache.commons.lang.NotImplementedException

class Neo4jPropertyValueIndexer implements PropertyValueIndexer {

    private static final Logger log = LoggerFactory.getLogger(Neo4jPropertyValueIndexer.class);

    PersistentProperty persistentProperty
    GraphDatabaseService graphDatabaseService

    @Override
    void index(Object value, Object primaryKey) {

        if (value) {
            log.info "index property $persistentProperty.name for node $primaryKey value $value"
            def index = graphDatabaseService.index().forNodes(persistentProperty.owner.name)
            def node = graphDatabaseService.getNodeById(primaryKey)
            index.remove(node, persistentProperty.name)
            index.add(node, persistentProperty.name, value)
        } else {
            log.info "skipping indexing property $persistentProperty.name for node $primaryKey since value = $value"
        }
    }

    @Override
    List query(Object value) {
        throw new NotImplementedException() // TODO: implement me
    }

    @Override
    List query(Object value, int offset, int max) {
        throw new NotImplementedException() // TODO: implement me
    }

    @Override
    String getIndexName(Object value) {
        throw new NotImplementedException() // TODO: implement me
    }

    @Override
    void deindex(Object value, Object primaryKey) {
        def index = graphDatabaseService.index().forNodes(persistentProperty.owner.name)
        def node = graphDatabaseService.getNodeById(primaryKey)
        index.remove(node, persistentProperty.name, value)
    }


}
