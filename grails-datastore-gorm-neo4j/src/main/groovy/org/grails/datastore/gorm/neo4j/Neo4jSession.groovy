package org.grails.datastore.gorm.neo4j

import org.springframework.datastore.mapping.core.AbstractSession
import org.springframework.datastore.mapping.engine.Persister
import org.springframework.datastore.mapping.model.MappingContext
import org.springframework.datastore.mapping.transactions.Transaction
import org.springframework.datastore.mapping.core.Datastore
import org.springframework.context.ApplicationEventPublisher
import org.springframework.datastore.mapping.model.PersistentEntity
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.DynamicRelationshipType
import org.neo4j.graphdb.Direction
import org.neo4j.graphdb.Relationship

/**
 * Created by IntelliJ IDEA.
 * User: stefan
 * Date: 25.04.11
 * Time: 12:25
 * To change this template use File | Settings | File Templates.
 */
class Neo4jSession extends AbstractSession {

    def subReferenceNodes // maps entity class names to neo4j subreference node

    public Neo4jSession(Datastore datastore, MappingContext mappingContext, ApplicationEventPublisher publisher) {
        super(datastore, mappingContext, publisher);

        subReferenceNodes = findSubReferenceNodes()

/*        this.mongoDatastore = datastore;
        try {
            getNativeInterface().requestStart();
        }
        catch (IllegalStateException ignored) {
            // can't call authenticate() twice, and it's probably been called at startup
        }*/
    }

    def findSubReferenceNodes() {
        def map = [:]
        Node referenceNode = nativeInterface.referenceNode
        for (Relationship rel in referenceNode.getRelationships(GrailsRelationshipTypes.SUBREFERENCE, Direction.OUTGOING)) {
            def endNode = rel.endNode
            def clazz = endNode.getProperty("__subreference__")
            map[clazz] = endNode
        }
        map
    }

    @Override
    protected Persister createPersister(Class cls, MappingContext mappingContext) {
        final PersistentEntity entity = mappingContext.getPersistentEntity(cls.getName());
        return entity == null ? null : new Neo4jEntityPersister(mappingContext, entity, this, publisher);
    }

    @Override
    protected Transaction beginTransactionInternal() {
        return null  //To change body of implemented methods use File | Settings | File Templates.
    }


    Object getNativeInterface() {
        datastore.graphDatabaseService
    }
}
