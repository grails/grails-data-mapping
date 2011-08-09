/* Copyright (C) 2010 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.datastore.gorm.neo4j

import org.neo4j.graphdb.Direction
import org.neo4j.graphdb.DynamicRelationshipType
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.Relationship
import org.grails.datastore.mapping.engine.EntityPersister
import org.grails.datastore.mapping.engine.NativeEntryEntityPersister
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.query.Query
import org.springframework.util.Assert
import java.util.regex.Pattern
import org.grails.datastore.mapping.query.Restrictions
import org.grails.datastore.mapping.query.Query.PropertyCriterion
import org.grails.datastore.mapping.model.PersistentProperty

/**
 * perform criteria queries on a Neo4j backend
 *
 * @author Stefan Armbruster <stefan@armbruster-it.de>
 */
class Neo4jQuery extends Query {

    NativeEntryEntityPersister entityPersister

    Neo4jQuery(Neo4jSession session, PersistentEntity entity, EntityPersister entityPersister) {
        super(session, entity)
        this.entityPersister = entityPersister
    }

    @Override
    protected List executeQuery(PersistentEntity entity, Query.Junction criteria) {

        Assert.notNull( entity, "Entity should not be null" )
        List result = []
        List<Node> subReferenceNodes = getSubreferencesOfSelfAndDerived(entity)
        List<String> validClassNames = subReferenceNodes.collect { it.getProperty(Neo4jEntityPersister.SUBREFERENCE_PROPERTY_NAME)}
        for (Node subReferenceNode in subReferenceNodes) {
            for (Relationship rel in subReferenceNode.getRelationships(GrailsRelationshipTypes.INSTANCE, Direction.OUTGOING)) {
                Node n = rel.endNode
                Assert.isTrue n.getProperty(Neo4jEntityPersister.TYPE_PROPERTY_NAME, null) in validClassNames

                if (invokeMethod("matchesCriterion${criteria.getClass().simpleName}", [n, criteria])) {
                    result << session.retrieve(entity.javaClass, n.id)
                }
            }
        }

        if (projections.projectionList) { // TODO: optimize, for count we do not need to create all objects
            projection(result)
        } else {
            orderBy(paginate(result))
        }
    }

    List<Node> getSubreferencesOfSelfAndDerived(entity) {
        Map<Class, Node> subReferenceNodes = session.datastore.subReferenceNodes
        // TODO: handle inheritence recursively
        List<Node> result = entityPersister.mappingContext.persistentEntities.findAll { it.parentEntity == entity }.collect {
            subReferenceNodes[it.name]
        }
        if (subReferenceNodes.containsKey(entity.name)) {
            result << subReferenceNodes[entity.name]
        }
        result
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
                case Query.IdProjection:
                    return collection.collect { it."${entity.identity.name}" }
                    break
                default:
                    throw new IllegalArgumentException("projections do not support ${projection.getClass().name}")
            }
        }.flatten()
    }

    def paginate(collection) {
        if ((max == -1 && offset == 0) || collection.empty) return collection

        int lastIndex = (max == -1) ? collection.size() : Math.min(collection.size(), offset + max)
        collection[offset..lastIndex - 1]
    }

    def orderBy(collection) {
        if (orderBy.empty) return collection
//        assert orderBy.size() == 1, "for now only sorting a single property is allowd"
        collection.sort { a,b ->
            for (Query.Order order in orderBy) {
                int cmp = a."$order.property" <=> b."$order.property"
                if (cmp) {
                    return order.direction == org.grails.datastore.mapping.query.Query.Order.Direction.ASC ? cmp : -cmp
                }
            }
        }
    }

    boolean matchesCriterionDisjunction(Node node, Query.Junction criterion) {
        criterion.criteria.any { invokeMethod("matchesCriterion${it.getClass().simpleName}", [node,it])}
    }

    boolean matchesCriterionConjunction(Node node, Query.Junction criterion) {
        criterion.criteria.every { invokeMethod("matchesCriterion${it.getClass().simpleName}", [node,it])}
    }

    boolean matchesCriterionNegation(Node node, Query.Junction criterion) {
        return !matchesCriterionDisjunction(node, criterion)
    }

    boolean matchesCriterionEquals(Node node, Query.Criterion criterion) {
        if (entityPersister.persistentEntity.associations.any { it.name == criterion.name}) {
            node.getSingleRelationship(DynamicRelationshipType.withName(criterion.name), Direction.BOTH)?.getOtherNode(node).id == criterion.value
        } else {
            getNodeProperty(node, criterion.name) == criterion.value
        }
    }

    boolean matchesCriterionNotEquals(Node node, Query.Criterion criterion) {
        getNodeProperty(node, criterion.name) != criterion.value
    }

    boolean matchesCriterionIn(Node node, Query.In criterion) {
        getNodeProperty(node, criterion.name) in criterion.values
    }

    boolean matchesCriterionLike(Node node, Query.Like criterion) {
        def value = criterion.value.replaceAll('%','.*')
        getNodeProperty(node, criterion.name) ==~ /$value/
    }

    boolean matchesCriterionILike(Node node, Query.ILike criterion) {
        def value = criterion.value.replaceAll('%','.*')
        def pattern = Pattern.compile(value, Pattern.CASE_INSENSITIVE)
        pattern.matcher(getNodeProperty(node, criterion.name)).matches()
    }

    boolean matchesCriterionBetween(Node node, Query.Between criterion) {
        def value = getNodePropertyAsType(node, criterion.property, criterion.from.getClass())
        return ((value >= criterion.from) && (value <= criterion.to))
    }

    boolean matchesCriterionGreaterThan(Node node, Query.GreaterThan criterion) {
        getNodePropertyAsType(node, criterion.name, criterion.value?.getClass()) > criterion.value
    }

    boolean matchesCriterionGreaterThanEquals(Node node, Query.GreaterThanEquals criterion) {
        getNodePropertyAsType(node, criterion.name, criterion.value?.getClass()) >= criterion.value
    }

    boolean matchesCriterionLessThan(Node node, Query.LessThan criterion) {
        getNodePropertyAsType(node, criterion.name, criterion.value?.getClass()) < criterion.value
    }

    boolean matchesCriterionLessThanEquals(Node node, Query.LessThanEquals criterion) {
        getNodePropertyAsType(node, criterion.name, criterion.value?.getClass()) <= criterion.value
    }

    boolean matchesCriterionIdEquals(Node node, Query.IdEquals criterion) {
        node.id == criterion.value
    }

    boolean matchesCriterionIdEqualsWithName(Node node, IdEqualsWithName criterion) {
        PersistentProperty persistentProperty = entity.getPropertyByName(criterion.name)
        String relationshipTypeName = persistentProperty.name
        Direction direction = Direction.OUTGOING

        if (persistentProperty.bidirectional && !persistentProperty.owningSide) {
            relationshipTypeName = persistentProperty.referencedPropertyName
            direction = Direction.INCOMING
        }
        DynamicRelationshipType relationshipType = DynamicRelationshipType.withName(relationshipTypeName)

        Relationship rel = node.getSingleRelationship(relationshipType, direction)

        def result = rel?.getOtherNode(node).id == criterion.value
        result
    }

    protected getNodePropertyAsType(Node node, String propertyName, Class targetClass) {
        def val = getNodeProperty(node, propertyName)
        session.mappingContext.conversionService.convert(val, targetClass)
    }

    protected getNodeProperty(Node node, String propertyName) {
        if (propertyName == 'id') {
            node.id
        } else {
            node.getProperty(propertyName, null)
        }
    }

    @Override
    Query eq(String property, Object value) {
        Object resolved = resolveIdIfEntity(value)
        if (resolved == value) {
           criteria.add(Restrictions.eq(property, value))
        }
        else {
           criteria.add(new IdEqualsWithName(property, resolved))
        }
        this
    }
}

public static class IdEqualsWithName extends PropertyCriterion {
    def IdEqualsWithName(property, value) {
        super(property,value)
    }
}