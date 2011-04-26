package org.grails.datastore.gorm.neo4j

import org.springframework.datastore.mapping.engine.AssociationIndexer
import org.springframework.datastore.mapping.model.PersistentEntity
import org.springframework.datastore.mapping.model.types.Association
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.apache.commons.lang.NotImplementedException
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.DynamicRelationshipType

/**
 * Created by IntelliJ IDEA.
 * User: stefan
 * Date: 26.04.11
 * Time: 14:45
 * To change this template use File | Settings | File Templates.
 */
class Neo4jAssociationIndexer implements AssociationIndexer {

    private static final Logger LOG = LoggerFactory.getLogger(Neo4jAssociationIndexer.class);

    Node nativeEntry
    Association association

    void index(Object primaryKey, List foreignKeys) {
        assert nativeEntry.id == primaryKey
        LOG.info("indexing $primaryKey, $foreignKeys")

        foreignKeys.each {
            throw new NotImplementedException()
        }
        //To change body of implemented methods use File | Settings | File Templates.
    }

    List query(Object primaryKey) {
        LOG.info("query $primaryKey")
        nativeEntry.getRelationships(DynamicRelationshipType.withName(association.referencedPropertyName)).collect { it.endNode.id}
    }

    PersistentEntity getIndexedEntity() {
        LOG.info("getIndexEntity")
        association.associatedEntity
    }

    void index(Object primaryKey, Object foreignKey) {
        LOG.info("indexing $primaryKey, $foreignKey")
        throw new NotImplementedException()
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
