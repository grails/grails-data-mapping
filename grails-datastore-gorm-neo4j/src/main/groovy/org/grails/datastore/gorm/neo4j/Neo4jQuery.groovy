package org.grails.datastore.gorm.neo4j

import org.springframework.datastore.mapping.query.Query
import org.springframework.datastore.mapping.model.PersistentEntity
import org.springframework.datastore.mapping.engine.EntityPersister
import org.neo4j.graphdb.Node
import org.springframework.datastore.mapping.engine.NativeEntryEntityPersister

/**
 * Created by IntelliJ IDEA.
 * User: stefan
 * Date: 25.04.11
 * Time: 21:54
 * To change this template use File | Settings | File Templates.
 */
class Neo4jQuery extends Query {

    NativeEntryEntityPersister entityPersister

    public Neo4jQuery(Neo4jSession session, PersistentEntity entity, EntityPersister entityPersister) {
        super(session, entity);
        this.entityPersister = entityPersister
    }

    @Override
    protected List executeQuery(PersistentEntity entity, Query.Junction criteria) {
        // TODO: for now return all nodes, handle subreference nodes
        def result = []
        for (Node n in session.nativeInterface.getAllNodes()) {
            if (n.getProperty("__type__", null) == entityPersister.entityFamily) {
                result << entityPersister.createObjectFromNativeEntry(entity, n.id, n)
            }
        }
        result
    }
}
