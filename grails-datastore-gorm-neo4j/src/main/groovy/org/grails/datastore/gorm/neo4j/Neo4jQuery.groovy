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

import static org.grails.datastore.mapping.query.Query.*
import static org.apache.lucene.search.BooleanClause.Occur.*
import static org.grails.datastore.mapping.query.Query.Order.Direction.*

import org.neo4j.graphdb.Direction
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.Relationship

import org.grails.datastore.mapping.model.PersistentEntity
import org.springframework.util.Assert
import java.util.regex.Pattern

import org.grails.datastore.mapping.query.AssociationQuery;
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.mapping.query.Restrictions
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.query.projections.ManualProjections
import org.grails.datastore.mapping.model.types.Association
import org.neo4j.graphdb.index.IndexManager
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.BooleanClause
import org.apache.commons.lang.NotImplementedException
import org.apache.lucene.search.TermQuery
import org.apache.lucene.index.Term
import org.slf4j.LoggerFactory
import org.slf4j.Logger
import org.apache.lucene.search.MatchAllDocsQuery
import org.grails.datastore.mapping.model.types.Simple
import org.neo4j.kernel.AbstractGraphDatabase
import org.neo4j.helpers.collection.IteratorUtil

/**
 * perform criteria queries on a Neo4j backend
 *
 * @author Stefan Armbruster <stefan@armbruster-it.de>
 */
class Neo4jQuery extends Query {

    protected final Logger log = LoggerFactory.getLogger(getClass())

    Neo4jQuery(Neo4jSession session, PersistentEntity entity) {
        super(session, entity)
    }

    @Override
    protected List executeQuery(PersistentEntity entity, Junction criteria) {

        Assert.notNull( entity, "Entity must not be null" )

        if (indexQueryPossible(entity, criteria)) {
            try {
                executeQueryViaIndex(entity, criteria)
            } catch (NotImplementedException e) {
                executeQueryViaRelationships(entity, criteria)
            }
        } else {
            executeQueryViaRelationships(entity, criteria)
        }
    }

    protected List executeQueryViaIndex(PersistentEntity persistentEntity, Junction junction) {
        IndexManager indexManager = session.datastore.indexManager
        def query = new BooleanQuery()

        def typeQuery = new BooleanQuery()
        for (PersistentEntity pe in session.datastore.domainSubclasses[persistentEntity]) {
            typeQuery.add(new TermQuery(new Term(Neo4jSession.TYPE_PROPERTY_NAME, pe.name)), SHOULD)
        }
        query.add(typeQuery, MUST)
//        query.add(new TermQuery(new Term(Neo4jSession.TYPE_PROPERTY_NAME, persistentEntity.name)), MUST)
        query.add(buildIndexQuery(persistentEntity, junction), MUST)
//        def query = buildIndexQuery(persistentEntity, junction)
        log.info("lucene query: $query")

        orderBy(paginate(
        indexManager.nodeAutoIndexer.autoIndex.query(query.toString()).iterator().collect {
            session.retrieve(persistentEntity.javaClass, it.id)
        }))
    }

    static MAP_JUNCTION_TO_BOOLEAN_CLAUSE = [
            Conjunction: MUST,
            Disjunction: SHOULD,
            Negation: MUST_NOT
    ]

    protected org.apache.lucene.search.Query buildIndexQuery(PersistentEntity persistentEntity, Negation negation) {
        org.apache.lucene.search.Query query = new BooleanQuery()
        query.add(new MatchAllDocsQuery(), MUST)
        query.add(buildIndexQuery(persistentEntity, new Disjunction(negation.criteria)), MUST_NOT)
        query
    }

    protected org.apache.lucene.search.Query buildIndexQuery(PersistentEntity persistentEntity, Junction junction) {
        org.apache.lucene.search.Query query = new BooleanQuery()
        BooleanClause.Occur queryType = MAP_JUNCTION_TO_BOOLEAN_CLAUSE[junction.class.simpleName]
        assert queryType

        for (Criterion criterion in junction.criteria) {
            query.add(buildIndexQuery(persistentEntity, criterion), queryType)
        }
        query
    }

    protected org.apache.lucene.search.Query buildIndexQuery(PersistentEntity persistentEntity, PropertyNameCriterion criterion) {
        switch (criterion) {
            case Equals:
                def value = criterion.value
                if (value instanceof String) {
                    value = "\"$value\""
                } else {
                    value = value?.toString()
                }
                return new TermQuery(new Term(criterion.name, value?.toString()))
                break
            // TODO: amend other cases
            default:
                throw new NotImplementedException("criterion $criterion for index queries".toString())
        }
    }

    boolean indexQueryPossible(PersistentEntity persistentEntity, Junction junction) {

        if ((!projections.empty) || (junction.criteria.empty)) {
            return false
        }

        // REST database -> no indexing (RestIndexManager does not support this
        if (!(session.nativeInterface instanceof AbstractGraphDatabase)) {
            return false
        }

        Collection indexedPropertyNames = entity.persistentProperties.findAll {
            (it instanceof Simple) && (it.propertyMapping.mappedForm.index)
        }.collect {it.name}
        return !hasNonIndexedPropertyCriterion(indexedPropertyNames, junction)
    }

    boolean hasNonIndexedPropertyCriterion(Collection indexPropertyNames, Junction junction) {
        junction.criteria.any { hasNonIndexedPropertyCriterion(indexPropertyNames, it)}
    }

    boolean hasNonIndexedPropertyCriterion(Collection indexPropertyNames, PropertyNameCriterion propertyNameCriterion) {
        !(propertyNameCriterion.property in indexPropertyNames)
    }

    protected List executeQueryViaRelationships(PersistentEntity entity, Junction criteria) {
        def result = []
        List<Node> subReferenceNodes = getSubreferencesOfSelfAndDerived(entity)
        List<String> validClassNames = subReferenceNodes.collect { it.getProperty(Neo4jSession.SUBREFERENCE_PROPERTY_NAME)}

        // shortcut for count()
        if (criteria.empty && (projections.projectionList?.size()==1) && projections.projectionList[0] instanceof CountProjection) {
            log.error "shortcut for count"
            return [ subReferenceNodes.sum {
                IteratorUtil.count((Iterable)it.getRelationships(Direction.OUTGOING, GrailsRelationshipTypes.INSTANCE))
            } ]
        }

        for (Node subReferenceNode in subReferenceNodes) {
            for (Relationship rel in subReferenceNode.getRelationships(GrailsRelationshipTypes.INSTANCE, Direction.OUTGOING).iterator()) {
                Node n = rel.endNode
                Assert.isTrue n.getProperty(Neo4jSession.TYPE_PROPERTY_NAME, null) in validClassNames

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

    // FIXME: does the same query multiple times
    boolean matchesCriterionAssociationQuery(Node node, AssociationQuery query) {
        def (relationshipType, direction) = Neo4jUtils.relationTypeAndDirection(query.association)
        def value = node.getSingleRelationship(relationshipType, direction)?.getOtherNode(node)
        def q = new Neo4jQuery(query.session, query.entity)
        q.add(query.criteria)
        def list = q.list()
        list.any { it.id == value.id }
    }

    boolean hasNonIndexedPropertyCriterion(Collection indexPropertyNames, AssociationQuery query) {
        true
    }
    
    
    List<Node> getSubreferencesOfSelfAndDerived(entity) {
        Map<Class, Node> subReferenceNodes = session.datastore.subReferenceNodes
        // TODO: handle inheritence recursively
        List<Node> result = session.mappingContext.persistentEntities.findAll { it.parentEntity == entity }.collect {
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
                case CountProjection:
                    return collection.size()
                    break
                case MinProjection:
                    return collection.collect { it."$projection.propertyName" }.min()
                    break
                case MaxProjection:
                    return collection.collect { it."$projection.propertyName" }.max()
                    break
                case CountDistinctProjection:
                    return new ManualProjections(entity).countDistinct(collection, projection.propertyName)
                    break
                case PropertyProjection:
                    return paginate( collection.collect { it."$projection.propertyName" } )
                    break
                case IdProjection:
                    return paginate( collection.collect { it."${entity.identity.name}" } )
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
            for (Order order in orderBy) {
                int cmp = a."$order.property" <=> b."$order.property"
                if (cmp) {
                    return order.direction == ASC ? cmp : -cmp
                }
            }
        }
    }

    boolean matchesCriterionDisjunction(Node node, Junction criterion) {
        criterion.criteria.any { invokeMethod("matchesCriterion${it.getClass().simpleName}", [node,it])}
    }

    boolean matchesCriterionConjunction(Node node, Junction criterion) {
        criterion.criteria.every { invokeMethod("matchesCriterion${it.getClass().simpleName}", [node,it]) }
    }
    
    boolean matchesCriterionNegation(Node node, Junction criterion) {
        return !matchesCriterionDisjunction(node, criterion)
    }

    boolean matchesCriterionEquals(Node node, Criterion criterion) {
        def association = entity.associations.find { it.name == criterion.name}
        if (association) {
            def (relationshipType, direction) = Neo4jUtils.relationTypeAndDirection(association)
            node.getSingleRelationship(relationshipType, direction)?.getOtherNode(node)?.id == criterion.value
        } else {
            getNodeProperty(node, criterion.name) == criterion.value
        }
    }

    boolean matchesCriterionNotEquals(Node node, Criterion criterion) {
        getNodeProperty(node, criterion.name) != criterion.value
    }

    boolean matchesCriterionIn(Node node, In criterion) {
        getNodeProperty(node, criterion.name) in criterion.values
    }

    boolean matchesCriterionLike(Node node, Like criterion) {
        def value = criterion.value.replaceAll('%','.*')
        getNodeProperty(node, criterion.name) ==~ /$value/
    }

    boolean matchesCriterionILike(Node node, ILike criterion) {
        def value = criterion.value.replaceAll('%','.*')
        def pattern = Pattern.compile(value, Pattern.CASE_INSENSITIVE)
        pattern.matcher(getNodeProperty(node, criterion.name)).matches()
    }

    boolean matchesCriterionBetween(Node node, Between criterion) {
        def value = getNodePropertyAsType(node, criterion.property, criterion.from.getClass())
        return ((value >= criterion.from) && (value <= criterion.to))
    }

    boolean matchesCriterionGreaterThan(Node node, GreaterThan criterion) {
        getNodePropertyAsType(node, criterion.name, criterion.value?.getClass()) > criterion.value
    }

    boolean matchesCriterionGreaterThanEquals(Node node, GreaterThanEquals criterion) {
        getNodePropertyAsType(node, criterion.name, criterion.value?.getClass()) >= criterion.value
    }

    boolean matchesCriterionLessThan(Node node, LessThan criterion) {
        getNodePropertyAsType(node, criterion.name, criterion.value?.getClass()) < criterion.value
    }

    boolean matchesCriterionLessThanEquals(Node node, LessThanEquals criterion) {
        getNodePropertyAsType(node, criterion.name, criterion.value?.getClass()) <= criterion.value
    }

    boolean matchesCriterionIdEquals(Node node, IdEquals criterion) {
        node.id == criterion.value
    }

    boolean matchesCriterionIdEqualsWithName(Node node, IdEqualsWithName criterion) {
        PersistentProperty persistentProperty = entity.getPropertyByName(criterion.name)
        def (relationshipType, direction) = Neo4jUtils.relationTypeAndDirection(persistentProperty)
        Relationship rel = node.getSingleRelationship(relationshipType, direction)
        def result = rel?.getOtherNode(node).id == criterion.value
        result
    }

    boolean matchesCriterionNotEqualsProperty(Node node, NotEqualsProperty criterion) {
        getNodeProperty(node, criterion.property) != getNodeProperty(node, criterion.otherProperty)
    }

    boolean matchesCriterionEqualsProperty(Node node, EqualsProperty criterion) {
        getNodeProperty(node, criterion.property) == getNodeProperty(node, criterion.otherProperty)
    }

    boolean matchesCriterionGreaterThanEqualsProperty(Node node, GreaterThanEqualsProperty criterion) {
        getNodeProperty(node, criterion.property) >= getNodeProperty(node, criterion.otherProperty)
    }

    boolean matchesCriterionGreaterThanProperty(Node node, GreaterThanProperty criterion) {
        getNodeProperty(node, criterion.property) > getNodeProperty(node, criterion.otherProperty)
    }

    boolean matchesCriterionLessThanEqualsProperty(Node node, LessThanEqualsProperty criterion) {
        getNodeProperty(node, criterion.property) <= getNodeProperty(node, criterion.otherProperty)
    }

    boolean matchesCriterionLessThanProperty(Node node, LessThanProperty criterion) {
        getNodeProperty(node, criterion.property) < getNodeProperty(node, criterion.otherProperty)
    }
    
    boolean matchesCriterionSizeLessThanEquals(Node node, SizeLessThanEquals criterion) {
        countRelationshipsForProperty(node, criterion.property) <= criterion.value
    }

    boolean matchesCriterionSizeLessThan(Node node, SizeLessThan criterion) {
        countRelationshipsForProperty(node, criterion.property) < criterion.value
    }

    boolean matchesCriterionSizeGreaterThanEquals(Node node, SizeGreaterThanEquals criterion) {
        countRelationshipsForProperty(node, criterion.property) >= criterion.value
    }

    boolean matchesCriterionSizeGreaterThan(Node node, SizeGreaterThan criterion) {
        countRelationshipsForProperty(node, criterion.property) > criterion.value
    }

    boolean matchesCriterionSizeEquals(Node node, SizeEquals criterion) {
        countRelationshipsForProperty(node, criterion.property) == criterion.value
    }

    boolean matchesCriterionSizeNotEquals(Node node, SizeNotEquals criterion) {
        countRelationshipsForProperty(node, criterion.property) != criterion.value
    }

    protected int countRelationshipsForProperty(Node node, String propertyName) {
        Association association = entity.getPropertyByName(propertyName)
        def (relationshipType, direction) = Neo4jUtils.relationTypeAndDirection(association)
        node.getRelationships(relationshipType, direction).iterator().size()
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

public class IdEqualsWithName extends PropertyCriterion {
    def IdEqualsWithName(property, value) {
        super(property,value)
    }
}
