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

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.neo4j.engine.CypherEngine
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.query.AssociationQuery
import org.grails.datastore.mapping.query.Query
import static org.grails.datastore.mapping.query.Query.*

/**
 * perform criteria queries on a Neo4j backend
 *
 * @author Stefan Armbruster <stefan@armbruster-it.de>
 */
@CompileStatic
@Slf4j
class Neo4jQuery extends Query {

    final Neo4jEntityPersister neo4jEntityPersister

    protected Neo4jQuery(Session session, PersistentEntity entity, Neo4jEntityPersister neo4jEntityPersister) {
        super(session, entity)
        this.neo4jEntityPersister = neo4jEntityPersister
    }

    private String applyOrderAndLimits(CypherBuilder cypherBuilder) {
        def cypher = ""
        if (!orderBy.empty) {
            cypher += " ORDER BY "
            cypher += orderBy.collect { Query.Order order -> "n.${order.property} $order.direction" }.join(", ")
        }

        if (offset != 0) {
            int skipParam = cypherBuilder.addParam(offset)
            cypher += " SKIP {$skipParam}"
        }

        if (max != -1) {
            int limtiParam = cypherBuilder.addParam(max)
            cypher += " LIMIT {$limtiParam}"
        }
        cypher
    }

    @Override
    protected List executeQuery(PersistentEntity persistentEntity, Query.Junction criteria) {

        CypherBuilder cypherBuilder = new CypherBuilder(((GraphPersistentEntity)persistentEntity).getLabelsAsString());
        def conditions = buildConditions(criteria, cypherBuilder, "n")
        cypherBuilder.setConditions(conditions)
        cypherBuilder.setOrderAndLimits(applyOrderAndLimits(cypherBuilder))
        for (projection in projections.projectionList) {
            cypherBuilder.addReturnColumn(buildProjection(projection, cypherBuilder))
        }

        def executionResult = cypherEngine.execute(cypherBuilder.build(), cypherBuilder.getParams())
        if (projections.projectionList.empty) {
            return executionResult.collect { Map<String,Object> map ->   // TODO: potential performance problem here: for each instance we unmarshall seperately, better: use one combined statement to get 'em all

                Long id = map.id as Long
                Collection<String> labels = map.labels as Collection<String>
                Map<String,Object> data = map.data as Map<String, Object>
                neo4jEntityPersister.unmarshallOrFromCache(persistentEntity, id, labels, data)
            }
        } else {

            executionResult.collect { Map<String, Object> row ->
                executionResult.columns.collect {
                    row[it]
                }
            }.flatten() as List
        }
    }

    String buildProjection(Query.Projection projection, CypherBuilder cypherBuilder) {
        switch (projection) {
            case Query.CountProjection:
                return "count(*)"
                break
            case Query.CountDistinctProjection:
                def propertyName =  ((Query.PropertyProjection)projection).propertyName
                return "count( distinct n.${propertyName})"
                break
            case Query.MinProjection:
                def propertyName =  ((Query.PropertyProjection)projection).propertyName
                return "min(n.${propertyName})"
                break
            case Query.MaxProjection:
                def propertyName = ((Query.PropertyProjection) projection).propertyName
                return "max(n.${propertyName})"
                break
            case Query.PropertyProjection:
                def propertyName = ((Query.PropertyProjection) projection).propertyName
                def association = entity.getPropertyByName(propertyName)
                if (association instanceof Association) {
                    def targetNodeName = "m_${cypherBuilder.getNextMatchNumber()}"
                    cypherBuilder.addMatch("(n)${matchForAssociation(association)}(${targetNodeName})")
                    return targetNodeName
                } else {
                    return "n.${propertyName}"
                }
                break

            default:
                throw new UnsupportedOperationException("projection ${projection.class}")
        }
    }

    String buildConditions(Query.Criterion criterion, CypherBuilder builder, String prefix) {
        switch (criterion) {
            case Query.PropertyCriterion:
                return buildConditionsPropertyCriterion( (Query.PropertyCriterion)criterion, builder, prefix)
                break
            case Query.Conjunction:
            case Query.Disjunction:
                def inner = ((Query.Junction)criterion).criteria
                        .collect { Query.Criterion it -> buildConditions(it, builder, prefix)}
                        .join( criterion instanceof Query.Conjunction ? ' AND ' : ' OR ')
                return inner ? "( $inner )" : inner
                break
            case Query.Negation:
                List<Query.Criterion> criteria = ((Query.Negation) criterion).criteria
                return "NOT (${buildConditions(new Query.Disjunction(criteria), builder, prefix)})"
                break
            case Query.PropertyComparisonCriterion:
                return buildConditionsPropertyComparisonCriterion(criterion as Query.PropertyComparisonCriterion, prefix)
                break
            case Query.PropertyNameCriterion:
                Query.PropertyNameCriterion pnc = criterion as Query.PropertyNameCriterion
                switch (pnc) {
                    case Query.IsNull:
                        return "has($prefix.${pnc.property})"
                        break
                    default:
                        throw new UnsupportedOperationException("${criterion}")
                }
            case AssociationQuery:
                AssociationQuery aq = criterion as AssociationQuery
                def targetNodeName = "m_${builder.getNextMatchNumber()}"
                builder.addMatch("(n)${matchForAssociation(aq.association)}(${targetNodeName})")
                def s = buildConditions(aq.criteria, builder, targetNodeName)
                return s
                break
            default:
                throw new UnsupportedOperationException("${criterion}")
        }
    }

    def buildConditionsPropertyComparisonCriterion(Query.PropertyComparisonCriterion pcc, String prefix) {
        def operator
        switch (pcc) {
            case Query.GreaterThanEqualsProperty:
                operator = ">="
                break
            case Query.EqualsProperty:
                operator = "="
                break
            case Query.NotEqualsProperty:
                operator = "<>"
                break
            case Query.LessThanEqualsProperty:
                operator = "<="
                break
            case Query.LessThanProperty:
                operator = "<"
                break
            case Query.GreaterThanProperty:
                operator = ">"
                break
            default:
                throw new UnsupportedOperationException("${pcc}")
        }
        return "$prefix.${pcc.property}${operator}n.${pcc.otherProperty}"
    }

    def buildConditionsPropertyCriterion( Query.PropertyCriterion pnc, CypherBuilder builder, String prefix) {
        int paramNumber = builder.addParam(Neo4jUtils.mapToAllowedNeo4jType(pnc.value, entity.mappingContext))
        def rhs
        def lhs
        def operator

        switch (pnc) {
            case Query.Equals:
                def association = entity.getPropertyByName(pnc.property)
                if (association instanceof Association) {
                    def targetNodeName = "m_${builder.getNextMatchNumber()}"
                    builder.addMatch("(${prefix})${matchForAssociation(association)}(${targetNodeName})")
                    lhs = "${targetNodeName}.__id__"
                } else {
                    lhs = pnc.property == "id" ? "${prefix}.__id__" : "${prefix}.${pnc.property}"
                }
                operator = "="
                rhs = "{$paramNumber}"
                break
            case Query.IdEquals:
                lhs = "${prefix}.__id__"
                operator = "="
                rhs = "{$paramNumber}"
                break
            case Query.Like:
                lhs = "${prefix}.$pnc.property"
                operator = "=~"
                rhs = "{$paramNumber}"
                builder.replaceParamAt(paramNumber, pnc.value.toString().replaceAll("%", ".*"))
                break
            case Query.In:
                lhs = pnc.property == "id" ? "${prefix}.__id__" : "${prefix}.$pnc.property"
                operator = " IN "
                rhs = "{$paramNumber}"
                builder.replaceParamAt(paramNumber, convertEnumsInList(((Query.In) pnc).values))
                break
            case Query.GreaterThan:
                lhs = "${prefix}.${pnc.property}"
                operator = ">"
                rhs = "{$paramNumber}"
                break
            case Query.GreaterThanEquals:
                lhs = "${prefix}.${pnc.property}"
                operator = ">="
                rhs = "{$paramNumber}"
                break
            case Query.LessThan:
                lhs = "${prefix}.${pnc.property}"
                operator = "<"
                rhs = "{$paramNumber}"
                break
            case Query.LessThanEquals:
                lhs = "${prefix}.${pnc.property}"
                operator = "<="
                rhs = "{$paramNumber}"
                break
            case Query.NotEquals:
                lhs = "${prefix}.${pnc.property}"
                operator = "<>"
                rhs = "{$paramNumber}"
                break
            case Query.Between:
                Query.Between b = (Query.Between) pnc


                int paramNumberFrom = builder.addParam(Neo4jUtils.mapToAllowedNeo4jType(b.from, entity.mappingContext))
                int parmaNumberTo = builder.addParam(Neo4jUtils.mapToAllowedNeo4jType(b.to, entity.mappingContext))
                return "{$paramNumberFrom}<=${prefix}.$pnc.property and ${prefix}.$pnc.property<={$parmaNumberTo}"
                break
            case Query.SizeLessThanEquals:
                Association association = entity.getPropertyByName(pnc.property) as Association
                builder.addMatch("(${prefix})${matchForAssociation(association)}() WITH ${prefix},count(*) as count")
                lhs = "count"
                operator = "<="
                rhs = "{$paramNumber}"
                break
            case Query.SizeLessThan:
                Association association = entity.getPropertyByName(pnc.property) as Association
                builder.addMatch("(${prefix})${matchForAssociation(association)}() WITH ${prefix},count(*) as count")
                lhs = "count"
                operator = "<"
                rhs = "{$paramNumber}"
                break
            case Query.SizeGreaterThan:
                Association association = entity.getPropertyByName(pnc.property) as Association
                builder.addMatch("(${prefix})${matchForAssociation(association)}() WITH ${prefix},count(*) as count")
                lhs = "count"
                operator = ">"
                rhs = "{$paramNumber}"
                break
            case Query.SizeGreaterThanEquals:
                Association association = entity.getPropertyByName(pnc.property) as Association
                builder.addMatch("(${prefix})${matchForAssociation(association)}() WITH ${prefix},count(*) as count")
                lhs = "count"
                operator = ">="
                rhs = "{$paramNumber}"
                break
            case Query.SizeEquals:
                Association association = entity.getPropertyByName(pnc.property) as Association
                builder.addMatch("(${prefix})${matchForAssociation(association)}() WITH ${prefix},count(*) as count")
                lhs = "count"
                operator = "="
                rhs = "{$paramNumber}"
                break
            case Query.SizeNotEquals:   // occurs multiple times
                Association association = entity.getPropertyByName(pnc.property) as Association
                builder.addMatch("(${prefix})${matchForAssociation(association)}() WITH ${prefix},count(*) as count")
                lhs = "count"
                operator = "<>"
                rhs = "{$paramNumber}"
                break
            default:
                throw new UnsupportedOperationException("propertycriterion ${pnc.class}")
        }

        return "$lhs$operator$rhs"
    }

    Collection convertEnumsInList(Collection collection) {
        collection.collect {
            it.getClass().isEnum() ? it.toString() : it
        }
    }

    private def matchForAssociation(Association association) {
        def relationshipType = RelationshipUtils.relationshipTypeUsedFor(association)
        def reversed = RelationshipUtils.useReversedMappingFor(association)
        StringBuilder sb = new StringBuilder();
        if (reversed) {
            sb.append("<")
        }
        sb.append("-[:").append(relationshipType).append("]-")
        if (!reversed) {
            sb.append(">")
        }
        sb.toString()
    }

    CypherEngine getCypherEngine() {
        session.nativeInterface as CypherEngine
    }
}


