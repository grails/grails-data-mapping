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
import org.springframework.datastore.mapping.model.MappingContext
import org.neo4j.graphdb.DynamicRelationshipType

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
		    projection(result)
	    } else {
		    orderBy(paginate(result))
	    }
    }

	def projection(collection) {
		projections.projectionList.collect { projection ->
			switch (projection) {
				case Query.CountProjection:
					return collection.size()
					break
				case Query.MinProjection:
					return collection.collect { it."$projection.propertyName" }.min()
					break
				case Query.MaxProjection:
					return collection.collect { it."$projection.propertyName" }.max()
					break
				case Query.PropertyProjection:
					return collection.collect { it."$projection.propertyName" }
					break
				default:
				    throw new NotImplementedException("projection do support ${projection.class}")
			}
		}.flatten()
	}

	def paginate(collection) {
		if (((max==-1) && (offset==0)) || collection.empty) return collection

		def lastIndex = (max==-1) ? collection.size() : offset+max
		collection[offset..lastIndex-1]
	}

	def orderBy(collection) {
		if (orderBy.empty) return collection
		assert orderBy.size() == 1, "for now only sorting a single property is allowd"
		collection.sort { a,b ->
			for (Query.Order order in orderBy) {
				def cmp = a."$order.property" <=> b."$order.property"
				if (cmp) {
					return order.direction == org.springframework.datastore.mapping.query.Query.Order.Direction.ASC ? cmp : -cmp
				}
			}
		}
	}


    /*boolean matchesJunction(Node node, Query.Junction junction) {
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
    } */

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
        if (entityPersister.persistentEntity.associations.any { it.name == criterion.name}) {
            node.getSingleRelationship(DynamicRelationshipType.withName(criterion.name), Direction.BOTH)?.getOtherNode(node).id == criterion.value
        } else {
            node.getProperty(criterion.name, null) == criterion.value
        }
    }

	boolean matchesCriterionNotEquals(Node node, Query.Criterion criterion) {
	    node.getProperty(criterion.name, null) != criterion.value
	}

	boolean matchesCriterionIn(Node node, Query.In criterion) {
		node.getProperty(criterion.name, null) in criterion.values
	}

	boolean matchesCriterionLike(Node node, Query.Like criterion) {
		def value = criterion.value.replaceAll('%','.*')
	    node.getProperty(criterion.name, null) ==~ /$value/
	}

	boolean matchesCriterionBetween(Node node, Query.Between criterion) {
		def value = getNodePropertyAsType(node, criterion.property, criterion.from.class)
		return ((value >= criterion.from) && (value <= criterion.to))
	}

	boolean matchesCriterionGreaterThan(Node node, Query.GreaterThan criterion) {
		getNodePropertyAsType(node, criterion.name, criterion.value?.class) > criterion.value
	}

	boolean matchesCriterionGreaterThanEquals(Node node, Query.GreaterThanEquals criterion) {
		getNodePropertyAsType(node, criterion.name, criterion.value?.class) >= criterion.value
	}

	boolean matchesCriterionLessThan(Node node, Query.LessThan criterion) {
		getNodePropertyAsType(node, criterion.name, criterion.value?.class) < criterion.value
	}

	boolean matchesCriterionLessThanEquals(Node node, Query.LessThanEquals criterion) {
		getNodePropertyAsType(node, criterion.name, criterion.value?.class) <= criterion.value
	}

	boolean matchesCriterionIdEquals(Node node, Query.IdEquals criterion) {
		node.id == criterion.value
	}

	private getNodePropertyAsType(Node node, String propertyName, Class targetClass) {
		def val = node.getProperty(propertyName, null)
		session.mappingContext.conversionService.convert(val, targetClass)
	}


}
