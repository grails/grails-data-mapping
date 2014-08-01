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
            cypher += orderBy.collect { Order order -> "n.${order.property} $order.direction" }.join(", ")
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
    protected List executeQuery(PersistentEntity persistentEntity, Junction criteria) {

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

    String buildProjection(Projection projection, CypherBuilder cypherBuilder) {
        switch (projection) {
            case CountProjection:
                return "count(*)"
                break
            case CountDistinctProjection:
                def propertyName =  ((PropertyProjection)projection).propertyName
                return "count( distinct n.${propertyName})"
                break
            case MinProjection:
                def propertyName =  ((PropertyProjection)projection).propertyName
                return "min(n.${propertyName})"
                break
            case MaxProjection:
                def propertyName = ((PropertyProjection) projection).propertyName
                return "max(n.${propertyName})"
                break
            case PropertyProjection:
                def propertyName = ((PropertyProjection) projection).propertyName
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

    String buildConditions(Criterion criterion, CypherBuilder builder, String prefix) {
        switch (criterion) {
            case PropertyCriterion:
                return buildConditionsPropertyCriterion( (PropertyCriterion)criterion, builder, prefix)
                break
            case Conjunction:
            case Disjunction:
                def inner = ((Junction)criterion).criteria
                        .collect { Criterion it -> buildConditions(it, builder, prefix)}
                        .join( criterion instanceof Conjunction ? ' AND ' : ' OR ')
                return inner ? "( $inner )" : inner
                break
            case Negation:
                List<Criterion> criteria = ((Negation) criterion).criteria
                return "NOT (${buildConditions(new Disjunction(criteria), builder, prefix)})"
                break
            case PropertyComparisonCriterion:
                return buildConditionsPropertyComparisonCriterion(criterion as PropertyComparisonCriterion, prefix)
                break
            case PropertyNameCriterion:
                PropertyNameCriterion pnc = criterion as PropertyNameCriterion
                switch (pnc) {
                    case IsNull:
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

    def buildConditionsPropertyComparisonCriterion(PropertyComparisonCriterion pcc, String prefix) {
        def operator
        switch (pcc) {
            case GreaterThanEqualsProperty:
                operator = ">="
                break
            case EqualsProperty:
                operator = "="
                break
            case NotEqualsProperty:
                operator = "<>"
                break
            case LessThanEqualsProperty:
                operator = "<="
                break
            case LessThanProperty:
                operator = "<"
                break
            case GreaterThanProperty:
                operator = ">"
                break
            default:
                throw new UnsupportedOperationException("${pcc}")
        }
        return "$prefix.${pcc.property}${operator}n.${pcc.otherProperty}"
    }

    def buildConditionsPropertyCriterion( PropertyCriterion pnc, CypherBuilder builder, String prefix) {
        int paramNumber = builder.addParam(Neo4jUtils.mapToAllowedNeo4jType(pnc.value, entity.mappingContext))
        def rhs
        def lhs
        def operator

        switch (pnc) {
            case Equals:
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
            case IdEquals:
                lhs = "${prefix}.__id__"
                operator = "="
                rhs = "{$paramNumber}"
                break
            case Like:
                lhs = "${prefix}.$pnc.property"
                operator = "=~"
                rhs = "{$paramNumber}"
                builder.replaceParamAt(paramNumber, pnc.value.toString().replaceAll("%", ".*"))
                break
            case In:
                lhs = pnc.property == "id" ? "${prefix}.__id__" : "${prefix}.$pnc.property"
                operator = " IN "
                rhs = "{$paramNumber}"
                builder.replaceParamAt(paramNumber, convertEnumsInList(((In) pnc).values))
                break
            case GreaterThan:
                lhs = "${prefix}.${pnc.property}"
                operator = ">"
                rhs = "{$paramNumber}"
                break
            case GreaterThanEquals:
                lhs = "${prefix}.${pnc.property}"
                operator = ">="
                rhs = "{$paramNumber}"
                break
            case LessThan:
                lhs = "${prefix}.${pnc.property}"
                operator = "<"
                rhs = "{$paramNumber}"
                break
            case LessThanEquals:
                lhs = "${prefix}.${pnc.property}"
                operator = "<="
                rhs = "{$paramNumber}"
                break
            case NotEquals:
                lhs = "${prefix}.${pnc.property}"
                operator = "<>"
                rhs = "{$paramNumber}"
                break
            case Between:
                Between b = (Between) pnc


                int paramNumberFrom = builder.addParam(Neo4jUtils.mapToAllowedNeo4jType(b.from, entity.mappingContext))
                int parmaNumberTo = builder.addParam(Neo4jUtils.mapToAllowedNeo4jType(b.to, entity.mappingContext))
                return "{$paramNumberFrom}<=${prefix}.$pnc.property and ${prefix}.$pnc.property<={$parmaNumberTo}"
                break
            case SizeLessThanEquals:
                Association association = entity.getPropertyByName(pnc.property) as Association
                builder.addMatch("(${prefix})${matchForAssociation(association)}() WITH ${prefix},count(*) as count")
                lhs = "count"
                operator = "<="
                rhs = "{$paramNumber}"
                break
            case SizeLessThan:
                Association association = entity.getPropertyByName(pnc.property) as Association
                builder.addMatch("(${prefix})${matchForAssociation(association)}() WITH ${prefix},count(*) as count")
                lhs = "count"
                operator = "<"
                rhs = "{$paramNumber}"
                break
            case SizeGreaterThan:
                Association association = entity.getPropertyByName(pnc.property) as Association
                builder.addMatch("(${prefix})${matchForAssociation(association)}() WITH ${prefix},count(*) as count")
                lhs = "count"
                operator = ">"
                rhs = "{$paramNumber}"
                break
            case SizeGreaterThanEquals:
                Association association = entity.getPropertyByName(pnc.property) as Association
                builder.addMatch("(${prefix})${matchForAssociation(association)}() WITH ${prefix},count(*) as count")
                lhs = "count"
                operator = ">="
                rhs = "{$paramNumber}"
                break
            case SizeEquals:
                Association association = entity.getPropertyByName(pnc.property) as Association
                builder.addMatch("(${prefix})${matchForAssociation(association)}() WITH ${prefix},count(*) as count")
                lhs = "count"
                operator = "="
                rhs = "{$paramNumber}"
                break
            case SizeNotEquals:   // occurs multiple times
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


