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
package org.grails.datastore.gorm.neo4j.engine

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.util.logging.Commons
import org.grails.datastore.gorm.neo4j.CypherBuilder
import org.grails.datastore.gorm.neo4j.GraphPersistentEntity
import org.grails.datastore.gorm.neo4j.Neo4jSession
import org.grails.datastore.gorm.neo4j.Neo4jUtils
import org.grails.datastore.gorm.neo4j.RelationshipUtils
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.ToOne
import org.grails.datastore.mapping.query.AssociationQuery
import org.grails.datastore.mapping.query.Query
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Label
import org.neo4j.graphdb.Node

/**
 * perform criteria queries on a Neo4j backend
 *
 * @author Stefan Armbruster <stefan@armbruster-it.de>
 */
@CompileStatic
@Commons
class Neo4jQuery extends Query {

    private static final String ORDER_BY_CLAUSE = " ORDER BY "
    private static final String BLANK = ""

    final Neo4jEntityPersister neo4jEntityPersister


    protected Neo4jQuery(Session session, PersistentEntity entity, Neo4jEntityPersister neo4jEntityPersister) {
        super(session, entity)
        this.neo4jEntityPersister = neo4jEntityPersister
    }

    private static Map<Class<? extends Query.Criterion>, String> COMPARISON_OPERATORS = [
            (Query.GreaterThanEqualsProperty): ">=".intern(),
            (Query.EqualsProperty): "=".intern(),
            (Query.NotEqualsProperty): "<>".intern(),
            (Query.LessThanEqualsProperty): "<=".intern(),
            (Query.LessThanProperty): "<".intern(),
            (Query.GreaterThanProperty): ">".intern()
    ]

    protected static Map<Class<? extends Query.Projection>, ProjectionHandler> PROJECT_HANDLERS = [
            (Query.CountProjection): new ProjectionHandler<Query.CountProjection>() {
                @Override
                @CompileStatic
                String handle(PersistentEntity entity, Query.CountProjection projection, CypherBuilder builder) {
                    return ProjectionHandler.COUNT
                }
            },
            (Query.CountDistinctProjection): new ProjectionHandler<Query.CountDistinctProjection>() {
                @Override
                @CompileStatic
                String handle(PersistentEntity entity, Query.CountDistinctProjection projection, CypherBuilder builder) {
                    return "count( distinct n.${projection.propertyName})"
                }
            },
            (Query.MinProjection): new ProjectionHandler<Query.MinProjection>() {
                @Override
                @CompileStatic
                String handle(PersistentEntity entity, Query.MinProjection projection, CypherBuilder builder) {
                    return "min(n.${projection.propertyName})"
                }
            },
            (Query.MaxProjection): new ProjectionHandler<Query.MaxProjection>() {
                @Override
                @CompileStatic
                String handle(PersistentEntity entity, Query.MaxProjection projection, CypherBuilder builder) {
                    return "max(n.${projection.propertyName})"
                }
            },
            (Query.SumProjection): new ProjectionHandler<Query.SumProjection>() {
                @Override
                @CompileStatic
                String handle(PersistentEntity entity, Query.SumProjection projection, CypherBuilder builder) {
                    return "sum(n.${projection.propertyName})"
                }
            },
            (Query.AvgProjection): new ProjectionHandler<Query.AvgProjection>() {
                @Override
                @CompileStatic
                String handle(PersistentEntity entity, Query.AvgProjection projection, CypherBuilder builder) {
                    return "avg(n.${projection.propertyName})"
                }
            },
            (Query.PropertyProjection): new ProjectionHandler<Query.PropertyProjection>() {
                @Override
                @CompileStatic
                String handle(PersistentEntity entity, Query.PropertyProjection projection, CypherBuilder builder) {
                    def propertyName = ((Query.PropertyProjection) projection).propertyName
                    def association = entity.getPropertyByName(propertyName)
                    if (association instanceof Association) {
                        def targetNodeName = "${association.name}_${builder.getNextMatchNumber()}"
                        builder.addMatch("(n)${matchForAssociation(association)}(${targetNodeName})")
                        return targetNodeName
                    } else {
                        return "n.${propertyName}"
                    }
                }
            }
    ]

    public static Map<Class<? extends Query.Criterion>, CriterionHandler> CRITERION_HANDLERS = [
            (Query.Conjunction): new CriterionHandler<Query.Conjunction>() {
                @Override
                @CompileStatic
                CypherExpression handle(PersistentEntity entity, Query.Conjunction criterion, CypherBuilder builder, String prefix) {
                    def inner = ((Query.Junction)criterion).criteria
                            .collect { Query.Criterion it ->
                                def handler = CRITERION_HANDLERS.get(it.getClass())
                                if(handler == null) {
                                    throw new UnsupportedOperationException("Criterion of type ${it.class.name} are not supported by GORM for Neo4j")
                                }
                                handler.handle(entity, it, builder, prefix).toString()
                            }
                            .join( CriterionHandler.OPERATOR_AND )
                    return new CypherExpression(inner ? "( $inner )" : inner)
                }
            },
            (Query.Disjunction): new CriterionHandler<Query.Disjunction>() {
                @Override
                @CompileStatic
                CypherExpression handle(PersistentEntity entity, Query.Disjunction criterion, CypherBuilder builder, String prefix) {
                    def inner = ((Query.Junction)criterion).criteria
                            .collect { Query.Criterion it ->
                        def handler = CRITERION_HANDLERS.get(it.getClass())
                        if(handler == null) {
                            throw new UnsupportedOperationException("Criterion of type ${it.class.name} are not supported by GORM for Neo4j")
                        }
                        handler.handle(entity, it, builder, prefix).toString()
                    }
                    .join( CriterionHandler.OPERATOR_OR )
                    return new CypherExpression(inner ? "( $inner )" : inner)
                }
            },
            (Query.Negation): new CriterionHandler<Query.Negation>() {
                @Override
                @CompileStatic
                CypherExpression handle(PersistentEntity entity, Query.Negation criterion, CypherBuilder builder, String prefix) {
                    List<Query.Criterion> criteria = criterion.criteria
                    def disjunction = new Query.Disjunction(criteria)
                    CriterionHandler<Query.Disjunction> handler = { ->
                        CRITERION_HANDLERS.get(Query.Disjunction)
                    }.call()
                    new CypherExpression("NOT (${handler.handle(entity, disjunction, builder, prefix)})")
                }
            },
            (Query.Equals): new CriterionHandler<Query.Equals>() {
                @Override
                @CompileStatic
                CypherExpression handle(PersistentEntity entity, Query.Equals criterion, CypherBuilder builder, String prefix) {
                    int paramNumber = builder.addParam(Neo4jUtils.mapToAllowedNeo4jType(criterion.value, entity.mappingContext))
                    def association = entity.getPropertyByName(criterion.property)
                    String lhs
                    if (association instanceof Association) {
                        def targetNodeName = "m_${builder.getNextMatchNumber()}"
                        builder.addMatch("(${prefix})${matchForAssociation(association)}(${targetNodeName})")
                        lhs = "${targetNodeName}.${CypherBuilder.IDENTIFIER}"
                    } else {
                        lhs = criterion.property == "id" ? "${prefix}.${CypherBuilder.IDENTIFIER}" : "${prefix}.${criterion.property}"
                    }

                    return new CypherExpression(lhs, "{$paramNumber}", CriterionHandler.OPERATOR_EQUALS)
                }
            },
            (Query.IdEquals): new CriterionHandler<Query.IdEquals>() {
                @Override
                @CompileStatic
                CypherExpression handle(PersistentEntity entity, Query.IdEquals criterion, CypherBuilder builder, String prefix) {
                    int paramNumber = builder.addParam(Neo4jUtils.mapToAllowedNeo4jType(criterion.value, entity.mappingContext))
                    return new CypherExpression("${prefix}.${CypherBuilder.IDENTIFIER}", "{$paramNumber}", CriterionHandler.OPERATOR_EQUALS)
                }
            },
            (Query.Like): new CriterionHandler<Query.Like>() {
                @Override
                @CompileStatic
                CypherExpression handle(PersistentEntity entity, Query.Like criterion, CypherBuilder builder, String prefix) {
                    int paramNumber = builder.addParam(Neo4jUtils.mapToAllowedNeo4jType(criterion.value, entity.mappingContext))
                    builder.replaceParamAt(paramNumber, criterion.value.toString().replaceAll("%", ".*"))
                    return new CypherExpression("${prefix}.$criterion.property", "{$paramNumber}", CriterionHandler.OPERATOR_LIKE)
                }
            },
            (Query.In): new CriterionHandler<Query.In>() {
                @Override
                @CompileStatic
                CypherExpression handle(PersistentEntity entity, Query.In criterion, CypherBuilder builder, String prefix) {
                    int paramNumber = builder.addParam(Neo4jUtils.mapToAllowedNeo4jType(criterion.value, entity.mappingContext))
                    String lhs = criterion.property == "id" ? "${prefix}.__id__" : "${prefix}.$criterion.property"
                    builder.replaceParamAt(paramNumber, convertEnumsInList(((Query.In) criterion).values))
                    return new CypherExpression(lhs, "{$paramNumber}", CriterionHandler.OPERATOR_IN)
                }
            },
            (Query.IsNull): new CriterionHandler<Query.IsNull>() {
                @Override
                @CompileStatic
                CypherExpression handle(PersistentEntity entity, Query.IsNull criterion, CypherBuilder builder, String prefix) {
                    return new CypherExpression("has($prefix.${criterion.property})")
                }
            },
            (AssociationQuery): new AssociationQueryHandler(),
            (Query.GreaterThan): ComparisonCriterionHandler.GREATER_THAN,
            (Query.GreaterThanEquals): ComparisonCriterionHandler.GREATER_THAN_EQUALS,
            (Query.LessThan): ComparisonCriterionHandler.LESS_THAN,
            (Query.LessThanEquals): ComparisonCriterionHandler.LESS_THAN_EQUALS,
            (Query.NotEquals): ComparisonCriterionHandler.NOT_EQUALS,

            (Query.GreaterThanProperty): PropertyComparisonCriterionHandler.GREATER_THAN,
            (Query.GreaterThanEqualsProperty): PropertyComparisonCriterionHandler.GREATER_THAN_EQUALS,
            (Query.LessThanProperty): PropertyComparisonCriterionHandler.LESS_THAN,
            (Query.LessThanEqualsProperty): PropertyComparisonCriterionHandler.LESS_THAN_EQUALS,
            (Query.NotEqualsProperty): PropertyComparisonCriterionHandler.NOT_EQUALS,
            (Query.EqualsProperty): PropertyComparisonCriterionHandler.EQUALS,

            (Query.Between): new CriterionHandler<Query.Between>() {
                @Override
                @CompileStatic
                CypherExpression handle(PersistentEntity entity, Query.Between criterion, CypherBuilder builder, String prefix) {
                    int paramNumber = builder.addParam(Neo4jUtils.mapToAllowedNeo4jType(criterion.value, entity.mappingContext))
                    int paramNumberFrom = builder.addParam(Neo4jUtils.mapToAllowedNeo4jType(criterion.from, entity.mappingContext))
                    int parmaNumberTo = builder.addParam(Neo4jUtils.mapToAllowedNeo4jType(criterion.to, entity.mappingContext))
                    new CypherExpression( "{$paramNumberFrom}<=${prefix}.$criterion.property and ${prefix}.$criterion.property<={$parmaNumberTo}")
                }
            },
            (Query.SizeLessThanEquals): SizeCriterionHandler.LESS_THAN_EQUALS,
            (Query.SizeLessThan): SizeCriterionHandler.LESS_THAN,
            (Query.SizeEquals): SizeCriterionHandler.EQUALS,
            (Query.SizeNotEquals): SizeCriterionHandler.NOT_EQUALS,
            (Query.SizeGreaterThan): SizeCriterionHandler.GREATER_THAN,
            (Query.SizeGreaterThanEquals): SizeCriterionHandler.GREATER_THAN_EQUALS

    ]


    private String applyOrderAndLimits(CypherBuilder cypherBuilder) {
        StringBuilder cypher = new StringBuilder(BLANK)
        if (!orderBy.empty) {
            cypher << ORDER_BY_CLAUSE
            cypher << orderBy.collect { Query.Order order -> "n.${order.property} $order.direction" }.join(", ")
        }

        if (offset != 0) {
            int skipParam = cypherBuilder.addParam(offset)
            cypher << " SKIP {$skipParam}"
        }

        if (max != -1) {
            int limitParam = cypherBuilder.addParam(max)
            cypher << " LIMIT {$limitParam}"
        }
        cypher.toString()
    }

    @Override
    protected List executeQuery(PersistentEntity persistentEntity, Query.Junction criteria) {

        CypherBuilder cypherBuilder = new CypherBuilder(((GraphPersistentEntity)persistentEntity).getLabelsAsString());
        def conditions = buildConditions(criteria, cypherBuilder, "n")
        cypherBuilder.setConditions(conditions)
        cypherBuilder.setOrderAndLimits(applyOrderAndLimits(cypherBuilder))

        def projectionList = projections.projectionList
        for (projection in projectionList) {
            cypherBuilder.addReturnColumn(buildProjection(projection, cypherBuilder))
        }


        def cypher = cypherBuilder.build()
        def params = cypherBuilder.getParams()

        log.debug("Executing Cypher Query [$cypher] for params [$params]")

        def executionResult = graphDatabaseService.execute(cypher, params)
        if (projectionList.empty) {
            // TODO: potential performance problem here: for each instance we unmarshall seperately, better: use one combined statement to get 'em all
            return executionResult.collect { Map<String,Object> map ->

                Long id = map.id as Long
                Collection<String> labels = map.labels as Collection<String>
                Node data = (Node) map.data
                neo4jEntityPersister.unmarshallOrFromCache(persistentEntity, id, labels, data)
            }
        } else {
            def columnNames = executionResult.columns()
            def projectedResults = executionResult.collect { Map<String, Object> row ->

                columnNames.collect { String columnName ->
                    def value = row.get(columnName)
                    if(value instanceof Node) {
                        // if a Node has been project then this is an association
                        def propName = columnName.substring(0, columnName.lastIndexOf('_'))
                        def prop = persistentEntity.getPropertyByName(propName)
                        if(prop instanceof ToOne) {
                            Association association = (Association)prop
                            Node childNode = (Node)value

                            def persister = getSession().getEntityPersister(association.type)
                            // TODO: potential performance problem here: for each instance we unmarshall seperately, better: use one combined statement to get 'em all
                            return persister.unmarshallOrFromCache(
                                                association.associatedEntity,
                                                (Long)childNode.getProperty(CypherBuilder.IDENTIFIER),
                                                childNode.labels.collect() { Label label -> label.name() },
                                                childNode)
                        }
                    }
                    return value
                }
            } as List
            if(projectionList.size() == 1 || projectedResults.size() == 1) {
                return projectedResults.flatten()
            }
            else {
                return projectedResults
            }
        }
    }


    String buildProjection(Query.Projection projection, CypherBuilder cypherBuilder) {
        def handler = PROJECT_HANDLERS.get(projection.getClass())
        if(handler != null) {
            return handler.handle(entity, projection, cypherBuilder)
        }
        else {
            throw new UnsupportedOperationException("projection ${projection.class} not supported by GORM for Neo4j")
        }
    }

    String buildConditions(Query.Criterion criterion, CypherBuilder builder, String prefix) {
        def handler = CRITERION_HANDLERS.get(criterion.getClass())
        if(handler != null) {
            return handler.handle(entity, criterion, builder, prefix).toString()
        }
        else {
            throw new UnsupportedOperationException("Criterion of type ${criterion.class.name} are not supported by GORM for Neo4j")
        }
    }

    private static Collection convertEnumsInList(Collection collection) {
        collection.collect {
            it.getClass().isEnum() ? it.toString() : it
        }
    }

    private static String matchForAssociation(Association association) {
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

    @Override
    Neo4jSession getSession() {
        return (Neo4jSession)super.getSession()
    }

    GraphDatabaseService getGraphDatabaseService() {
        return (GraphDatabaseService)getSession().getNativeInterface()
    }

    /**
     * Interface for handling projections when building Cypher queries
     *
     * @param < T > The projection type
     */
    static interface ProjectionHandler<T extends Query.Projection> {
        String COUNT = "count(*)"
        String handle(PersistentEntity entity, T projection, CypherBuilder builder)
    }

    /**
     * Interface for handling criterion when building Cypher queries
     *
     * @param < T > The criterion type
     */
    static interface CriterionHandler<T extends Query.Criterion> {
        String COUNT = "count"
        String OPERATOR_EQUALS = '='
        String OPERATOR_NOT_EQUALS = '<>'
        String OPERATOR_LIKE = "=~"
        String OPERATOR_IN = " IN "
        String OPERATOR_AND = " AND "
        String OPERATOR_OR = " OR "
        String OPERATOR_GREATER_THAN = ">"
        String OPERATOR_LESS_THAN = "<"
        String OPERATOR_GREATER_THAN_EQUALS = ">="
        String OPERATOR_LESS_THAN_EQUALS = "<="

        CypherExpression handle(PersistentEntity entity, T criterion, CypherBuilder builder, String prefix)
    }

    /**
     * Handles AssociationQuery instances
     */
    @CompileStatic
    static class AssociationQueryHandler implements CriterionHandler<AssociationQuery> {
        @Override
        CypherExpression handle(PersistentEntity entity, AssociationQuery criterion, CypherBuilder builder, String prefix) {
            AssociationQuery aq = criterion as AssociationQuery
            def targetNodeName = "m_${builder.getNextMatchNumber()}"
            builder.addMatch("(n)${matchForAssociation(aq.association)}(${targetNodeName})")

            def s = CRITERION_HANDLERS.get(aq.criteria.getClass()).handle(entity, aq.criteria, builder, targetNodeName)
            return new CypherExpression(s)

        }
    }

    /**
     * A criterion handler for comparison criterion
     *
     * @param < T >
     */
    @CompileStatic
    static class ComparisonCriterionHandler<T extends Query.PropertyCriterion> implements CriterionHandler<T> {
        public static final ComparisonCriterionHandler<Query.GreaterThanEquals> GREATER_THAN_EQUALS = new ComparisonCriterionHandler<Query.GreaterThanEquals>(CriterionHandler.OPERATOR_GREATER_THAN_EQUALS)
        public static final ComparisonCriterionHandler<Query.GreaterThan> GREATER_THAN = new ComparisonCriterionHandler<Query.GreaterThan>(CriterionHandler.OPERATOR_GREATER_THAN)
        public static final ComparisonCriterionHandler<Query.LessThan> LESS_THAN = new ComparisonCriterionHandler<Query.LessThan>(CriterionHandler.OPERATOR_LESS_THAN)
        public static final ComparisonCriterionHandler<Query.LessThanEquals> LESS_THAN_EQUALS = new ComparisonCriterionHandler<Query.LessThanEquals>(CriterionHandler.OPERATOR_LESS_THAN_EQUALS)
        public static final ComparisonCriterionHandler<Query.NotEquals> NOT_EQUALS = new ComparisonCriterionHandler<Query.NotEquals>(CriterionHandler.OPERATOR_NOT_EQUALS)
        public static final ComparisonCriterionHandler<Query.Equals> EQUALS = new ComparisonCriterionHandler<Query.Equals>(CriterionHandler.OPERATOR_EQUALS)

        final String operator

        ComparisonCriterionHandler(String operator) {
            this.operator = operator
        }

        @Override
        CypherExpression handle(PersistentEntity entity, T criterion, CypherBuilder builder, String prefix) {
            int paramNumber = builder.addParam(Neo4jUtils.mapToAllowedNeo4jType(criterion.value, entity.mappingContext))
            String lhs = "${prefix}.${criterion.property}"
            return new CypherExpression(lhs, "{$paramNumber}", operator)
        }
    }


    /**
     * A criterion handler for comparison criterion
     *
     * @param < T >
     */
    @CompileStatic
    static class PropertyComparisonCriterionHandler<T extends Query.PropertyComparisonCriterion> implements CriterionHandler<T> {
        public static final PropertyComparisonCriterionHandler<Query.GreaterThanEqualsProperty> GREATER_THAN_EQUALS = new PropertyComparisonCriterionHandler<Query.GreaterThanEqualsProperty>(CriterionHandler.OPERATOR_GREATER_THAN_EQUALS)
        public static final PropertyComparisonCriterionHandler<Query.GreaterThanProperty> GREATER_THAN = new PropertyComparisonCriterionHandler<Query.GreaterThanProperty>(CriterionHandler.OPERATOR_GREATER_THAN)
        public static final PropertyComparisonCriterionHandler<Query.LessThanProperty> LESS_THAN = new PropertyComparisonCriterionHandler<Query.LessThanProperty>(CriterionHandler.OPERATOR_LESS_THAN)
        public static final PropertyComparisonCriterionHandler<Query.LessThanEqualsProperty> LESS_THAN_EQUALS = new PropertyComparisonCriterionHandler<Query.LessThanEqualsProperty>(CriterionHandler.OPERATOR_LESS_THAN_EQUALS)
        public static final PropertyComparisonCriterionHandler<Query.NotEqualsProperty> NOT_EQUALS = new PropertyComparisonCriterionHandler<Query.NotEqualsProperty>(CriterionHandler.OPERATOR_NOT_EQUALS)
        public static final PropertyComparisonCriterionHandler<Query.EqualsProperty> EQUALS = new PropertyComparisonCriterionHandler<Query.EqualsProperty>(CriterionHandler.OPERATOR_EQUALS)

        final String operator

        PropertyComparisonCriterionHandler(String operator) {
            this.operator = operator
        }

        @Override
        CypherExpression handle(PersistentEntity entity, T criterion, CypherBuilder builder, String prefix) {
            def operator = COMPARISON_OPERATORS.get(criterion.getClass())
            if(operator == null) {
                throw new UnsupportedOperationException("Unsupported Neo4j property comparison: ${criterion}")
            }
            return new CypherExpression("$prefix.${criterion.property}${operator}n.${criterion.otherProperty}")
        }
    }
    /**
     * A citerion handler for size related queries
     *
     * @param < T >
     */
    @CompileStatic
    static class SizeCriterionHandler<T extends Query.PropertyCriterion> implements CriterionHandler<T> {

        public static final SizeCriterionHandler<Query.SizeEquals> EQUALS = new SizeCriterionHandler<Query.SizeEquals>(CriterionHandler.OPERATOR_EQUALS)
        public static final SizeCriterionHandler<Query.SizeNotEquals> NOT_EQUALS = new SizeCriterionHandler<Query.SizeNotEquals>(CriterionHandler.OPERATOR_NOT_EQUALS)
        public static final SizeCriterionHandler<Query.SizeGreaterThan> GREATER_THAN= new SizeCriterionHandler<Query.SizeGreaterThan>(CriterionHandler.OPERATOR_GREATER_THAN)
        public static final SizeCriterionHandler<Query.SizeGreaterThanEquals> GREATER_THAN_EQUALS = new SizeCriterionHandler<Query.SizeGreaterThanEquals>(CriterionHandler.OPERATOR_GREATER_THAN_EQUALS)
        public static final SizeCriterionHandler<Query.SizeLessThan> LESS_THAN = new SizeCriterionHandler<Query.SizeLessThan>(CriterionHandler.OPERATOR_LESS_THAN)
        public static final SizeCriterionHandler<Query.SizeLessThanEquals> LESS_THAN_EQUALS = new SizeCriterionHandler<Query.SizeLessThanEquals>(CriterionHandler.OPERATOR_LESS_THAN_EQUALS)

        final String operator;

        SizeCriterionHandler(String operator) {
            this.operator = operator
        }

        @Override
        CypherExpression handle(PersistentEntity entity, T criterion, CypherBuilder builder, String prefix) {
            int paramNumber = builder.addParam(Neo4jUtils.mapToAllowedNeo4jType(criterion.value, entity.mappingContext))
            Association association = entity.getPropertyByName(criterion.property) as Association
            builder.addMatch("(${prefix})${matchForAssociation(association)}() WITH ${prefix},count(*) as count")
            return new CypherExpression(CriterionHandler.COUNT, "{$paramNumber}", operator)
        }
    }

    @CompileStatic
    @EqualsAndHashCode
    static class CypherExpression {

        @Delegate final CharSequence expression

        CypherExpression(String lhs, String rhs, String operator) {
            this.expression = "$lhs$operator$rhs".toString()
        }

        CypherExpression(CharSequence expression) {
            this.expression = expression
        }

        @Override
        String toString() {
            this.expression
        }
    }
}


