package org.grails.datastore.gorm.neo4j

import org.springframework.datastore.mapping.query.Query
import org.springframework.datastore.mapping.model.PersistentEntity
import org.springframework.datastore.mapping.engine.EntityPersister
import org.neo4j.graphdb.Node
import org.springframework.datastore.mapping.engine.NativeEntryEntityPersister
import org.neo4j.graphdb.Relationship
import org.neo4j.graphdb.Direction
import org.apache.commons.lang.NotImplementedException

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

        assert entity
        Node subReferenceNode = session.subReferenceNodes[entity.name]
        assert subReferenceNode

        // TODO: for now return all nodes, handle subreference nodes
        def result = []
        for (Relationship rel in subReferenceNode.getRelationships(GrailsRelationshipTypes.INSTANCE, Direction.OUTGOING)) {
            Node n = rel.endNode
            assert n.getProperty("__type__", null) == entityPersister.entityFamily

            if (matchesJunction(n, criteria)) {
                result << entityPersister.createObjectFromNativeEntry(entity, n.id, n)
            }
        }
        result
    }

    boolean matchesJunction(Node node, Query.Junction junction) {
        if (junction.empty) {
            true
        } else {
            switch (junction) {
                case Query.Disjunction:
                    return junction.criteria.any { matchesCriteria(node, it)}
                    break
                case Query.Conjunction:
                    return junction.criteria.every { matchesCriteria(node, it)}
                    break
                case Query.Negation:
                    return !matchesCriteria(junction.criteria.first())
                    break
                default:
                    throw new NotImplementedException("couldn't handle junction ${junction.class}")

            }
        }
    }

    boolean matchesCriteria(Node node, Query.Criterion criterion) {
        assert criterion instanceof Query.Equals
        node.getProperty(criterion.name, null) == criterion.value
    }
}
