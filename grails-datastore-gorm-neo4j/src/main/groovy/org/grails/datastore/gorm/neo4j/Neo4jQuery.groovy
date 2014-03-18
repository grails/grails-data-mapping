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
import org.grails.datastore.gorm.neo4j.simplegraph.Relationship
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.query.Query
import org.neo4j.cypher.javacompat.ExecutionResult
import static org.grails.datastore.mapping.query.Query.*

/**
 * perform criteria queries on a Neo4j backend
 *
 * @author Stefan Armbruster <stefan@armbruster-it.de>
 */
@CompileStatic
@Slf4j
class Neo4jQuery extends Query {

    public static TYPE = "type"
    public static END = "end"
    public static START = "start"

    final Neo4jEntityPersister neo4jEntityPersister

    protected Neo4jQuery(Session session, PersistentEntity entity, Neo4jEntityPersister neo4jEntityPersister) {
        super(session, entity)
        this.neo4jEntityPersister = neo4jEntityPersister
    }


    private String applyOrderAndLimits(Map params) {
        def cypher = ""
        if (!orderBy.empty) {
            cypher += " ORDER BY "
            cypher += orderBy.collect { Order order -> "data.${order.property} $order.direction" }.join(", ")
        }

        if (offset != 0) {
            cypher += " SKIP {__skip__}"
            params["__skip__"] = offset
        }

        if (max != -1) {
            cypher += " LIMIT {__limit__}"
            params["__limit__"] = max
        }

        cypher
    }

    @Override
    protected List executeQuery(PersistentEntity persistentEntity, Junction criteria) {

        def params = [:] as Map<String,Object>
        def matches = ["(n:$persistentEntity.discriminator)"] as Set
        def conditions = buildConditions(criteria, params, matches)
        def cypher = """MATCH ${matches.join(",")} ${conditions ? "WHERE " + conditions : " "}"""

        if (projections.projectionList.empty) {
            cypher += """WITH id(n) as id, labels(n) as labels, n as data
OPTIONAL MATCH (n)-[r]-()
WITH id, labels, data, type(r) as t, collect(id(endnode(r))) as endNodeIds, collect(id(startnode(r))) as startNodeIds
RETURN id, labels, data, collect( {$TYPE: t, $END: endNodeIds, $START: startNodeIds}) as relationships
${applyOrderAndLimits(params)}"""

        } else {
            def returnColumns = projections.projectionList
                                .collect { Projection projection -> buildProjection(projection) }
                                .join(", ")
            cypher += "RETURN $returnColumns ${applyOrderAndLimits(params)}"
        }

        def executionResult = cypherEngine.execute(cypher, params)
        if (projections.projectionList.empty) {
            return executionResult.collect { Map<String,Object> map ->

                Long id = map.id as Long
                Collection<String> labels = map.labels as Collection<String>
                Map<String,Object> data = map.data as Map<String, Object>

                Collection<Relationship> relationships = new HashSet<>()
                for (m in map.relationships) {
                    def relTypeMap = m as Map
                    String relType = relTypeMap[TYPE]
                    Collection<Long> startIds = relTypeMap[START] as Collection<Long>
                    Collection<Long> endIds = relTypeMap[END] as Collection<Long>
                    assert endIds.size() == startIds.size()
                    [startIds, endIds].transpose().each { List it ->
                        assert it.size()==2
                        relationships << new Relationship(it[0] as Long, it[1] as Long, relType)
                    }
                }

                log.debug "relationships = $relationships"

                neo4jEntityPersister.retrieveEntityAccess(persistentEntity, id, labels, data, relationships).entity
            }
        } else {

            executionResult.collect { Map<String, Object> row ->
                executionResult.columns.collect {
                    row[it]
                }
            }.flatten() as List
        }
    }

    def buildProjection(Projection projection) {
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
                return "n.${propertyName}"
                break

            default:
                throw new UnsupportedOperationException("projection ${projection.class}")
        }
    }

    def buildConditions(Criterion criterion, Map params, Collection matches) {
        switch (criterion) {
            case PropertyCriterion:
                return buildConditionsPropertyCriterion(params, (PropertyCriterion)criterion, matches)
                break
            case Conjunction:
            case Disjunction:
                def inner = ((Junction)criterion).criteria
                        .collect { Criterion it -> buildConditions(it, params, matches)}
                        .join( criterion instanceof Conjunction ? ' AND ' : ' OR ')
                return inner ? "( $inner )" : inner
                break
            case Negation:
                List<Criterion> criteria = ((Negation) criterion).criteria
                return "NOT (${buildConditions(new Conjunction(criteria), params, matches)})"
                break
            case PropertyComparisonCriterion:
                return buildConditionsPropertyComparisonCriterion(criterion as PropertyComparisonCriterion)
                break
            case PropertyNameCriterion:
                PropertyNameCriterion pnc = criterion as PropertyNameCriterion
                switch (pnc) {
                    case IsNull:
                        return "has(n.${pnc.property})"
                        break
                    default:
                        throw new UnsupportedOperationException("${criterion}")
                }

            default:
                throw new UnsupportedOperationException("${criterion}")
        }
    }

    def buildConditionsPropertyComparisonCriterion(PropertyComparisonCriterion pcc) {
        def operator = ""
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
        return "n.${pcc.property}${operator}n.${pcc.otherProperty}"
    }

    def buildConditionsPropertyCriterion(Map params, PropertyCriterion pnc, Collection matches) {
        def paramName = "param_${params.size()}" as String
        params[paramName] = Neo4jUtils.mapToAllowedNeo4jType(pnc.value, entity.mappingContext)
        def rhs
        def lhs
        def operator

        switch (pnc) {
            case Equals:
                def association = entity.getPropertyByName(pnc.property)
                if (association instanceof Association) {
                    def targetNodeName = "m_${matches.size()}"
                    matches << "(n)${matchForAssociation(association)}(${targetNodeName})"
                    lhs = "id(${targetNodeName})"
                } else {
                    lhs = "n.$pnc.property"
                }
                operator = "="
                rhs = "{$paramName}"
                break
            case IdEquals:
                lhs = "id(n)"
                operator = "="
                rhs = "{$paramName}"
                break
            case Like:
                lhs = "n.$pnc.property"
                operator = "=~"
                rhs = "{$paramName}"
                params[paramName] = pnc.value.toString().replaceAll("%", ".*")
                break
            case In:
                lhs = pnc.property == "id" ? "ID(n)" : "n.$pnc.property"
                operator = " IN "
                rhs = "{$paramName}"
                params[paramName] = ((In) pnc).values
                break
            case GreaterThan:
                lhs = "n.$pnc.property"
                operator = ">"
                rhs = "{$paramName}"
                break
            case GreaterThanEquals:
                lhs = "n.$pnc.property"
                operator = ">="
                rhs = "{$paramName}"
                break
            case LessThan:
                lhs = "n.$pnc.property"
                operator = "<"
                rhs = "{$paramName}"
                break
            case LessThanEquals:
                lhs = "n.$pnc.property"
                operator = "<="
                rhs = "{$paramName}"
                break
            case NotEquals:
                lhs = "n.$pnc.property"
                operator = "<>"
                rhs = "{$paramName}"
                break
            case Between:
                Between b = (Between) pnc
                params["${paramName}_from".toString()] = Neo4jUtils.mapToAllowedNeo4jType(b.from, entity.mappingContext)
                params["${paramName}_to".toString()] = Neo4jUtils.mapToAllowedNeo4jType(b.to, entity.mappingContext)
                return "{${paramName}_from}<=n.$pnc.property and n.$pnc.property<={${paramName}_to}"
                break
            case SizeLessThanEquals:
                Association association = entity.getPropertyByName(pnc.property) as Association
                matches << "(n)${matchForAssociation(association)}() WITH n,count(*) as count"
                lhs = "count"
                operator = "<="
                rhs = "{$paramName}"
                break
            case SizeLessThan:
                Association association = entity.getPropertyByName(pnc.property) as Association
                matches << "(n)${matchForAssociation(association)}() WITH n,count(*) as count"
                lhs = "count"
                operator = "<"
                rhs = "{$paramName}"
                break
            case SizeGreaterThan:
                Association association = entity.getPropertyByName(pnc.property) as Association
                matches << "(n)${matchForAssociation(association)}() WITH n,count(*) as count"
                lhs = "count"
                operator = ">"
                rhs = "{$paramName}"
                break
            case SizeGreaterThanEquals:
                Association association = entity.getPropertyByName(pnc.property) as Association
                matches << "(n)${matchForAssociation(association)}() WITH n,count(*) as count"
                lhs = "count"
                operator = ">="
                rhs = "{$paramName}"
                break
            case SizeEquals:
                Association association = entity.getPropertyByName(pnc.property) as Association
                matches << "(n)${matchForAssociation(association)}() WITH n,count(*) as count"
                lhs = "count"
                operator = "="
                rhs = "{$paramName}"
                break
            case SizeNotEquals:   // occurs multiple times
                Association association = entity.getPropertyByName(pnc.property) as Association
                matches << "(n)${matchForAssociation(association)}() WITH n,count(*) as count"
                lhs = "count"
                operator = "<>"
                rhs = "{$paramName}"
                break
            default:
                throw new UnsupportedOperationException("propertycriterion ${pnc.class}")
        }

        return "$lhs$operator$rhs"
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


