package org.grails.datastore.gorm.neo4j

import org.springframework.datastore.mapping.query.Query
import org.springframework.datastore.mapping.model.PersistentEntity
import org.springframework.datastore.mapping.engine.EntityPersister
import org.neo4j.graphdb.Node
import org.springframework.datastore.mapping.engine.NativeEntryEntityPersister
import org.neo4j.graphdb.Relationship
import org.neo4j.graphdb.Direction
import org.apache.commons.lang.NotImplementedException
import org.slf4j.LoggerFactory
import org.slf4j.Logger

/**
 * Created by IntelliJ IDEA.
 * User: stefan
 * Date: 25.04.11
 * Time: 21:54
 * To change this template use File | Settings | File Templates.
 */
class Neo4jQuery extends Query {

    private static final Logger LOG = LoggerFactory.getLogger(Neo4jQuery.class);

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

        def result = []
        for (Relationship rel in subReferenceNode.getRelationships(GrailsRelationshipTypes.INSTANCE, Direction.OUTGOING)) {
            Node n = rel.endNode
            assert n.getProperty(Neo4jEntityPersister.TYPE_PROPERTY_NAME, null) == entityPersister.entityFamily

            if (invokeMethod("matchesCriterion${criteria.class.simpleName}", [n, criteria])) {
                result << entityPersister.createObjectFromNativeEntry(entity, n.id, n)
            }
        }

	    if (projections.projectionList) {      // TODO: optimize, for count we do not need to create all objects
		    assert projections.projectionList.size()==1, "only a single projection supported for now"
		    assert projections.projectionList[0] instanceof Query.CountProjection
		    return [result.size()]
	    } else {
		    paginate(result)
	    }
    }

	def paginate(collection) {
		if (((max==-1) && (offset==0)) || collection.empty) return collection

		def lastIndex = (max==-1) ? collection.size() : offset+max
		collection[offset..lastIndex-1]
	}



    boolean matchesJunction(Node node, Query.Junction junction) {
        if (junction.empty) {
            true
        } else {
            switch (junction) {
                case Query.Disjunction:
                    return junction.criteria.any { invokeMethod("matchesCriterion${it.class.simpleName}", [node,it])}
                    break
                case Query.Conjunction:
                    return junction.criteria.every {
                        LOG.info "criterion is ${it.class.simpleName}"
                        invokeMethod("matchesCriterion${it.class.simpleName}", [node,it])
                        //matchesCriteria(node, it)
                        }
                    break
                case Query.Negation:
                    assert junction.criteria.size()==1
                    def firstCriterion = junction.criteria.first()
                    return !invokeMethod("matchesCriterion${firstCriterion.class.simpleName}", [node,firstCriterion])
                    break
                default:
                    throw new NotImplementedException("couldn't handle junction ${junction.class}")

            }
        }
    }

	boolean matchesCriterionDisjunction(Node node, Query.Junction criterion) {
		criterion.criteria.any { invokeMethod("matchesCriterion${it.class.simpleName}", [node,it])}
	}

	boolean matchesCriterionConjunction(Node node, Query.Junction criterion) {
		criterion.criteria.every { invokeMethod("matchesCriterion${it.class.simpleName}", [node,it])}
	}

	boolean matchesCriterionNegation(Node node, Query.Junction criterion) {
		return !matchesCriterionDisjunction(node, criterion)
	}

    boolean matchesCriterionEquals(Node node, Query.Criterion criterion) {
        node.getProperty(criterion.name, null) == criterion.value
    }

	boolean matchesCriterionNotEquals(Node node, Query.Criterion criterion) {
	    node.getProperty(criterion.name, null) != criterion.value
	}

	boolean matchesCriterionIn(Node node, Query.In criterion) {
		node.getProperty(criterion.name, null) in criterion.values
	}

	boolean matchesCriterionLike(Node node, Query.Like criterion) {
		def value = criterion.value.replaceAll('%','.*')
	    node.getProperty(criterion.name, null) =~ /$value/
	}


}
