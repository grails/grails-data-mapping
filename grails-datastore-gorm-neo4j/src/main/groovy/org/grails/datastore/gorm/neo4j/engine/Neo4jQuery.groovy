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
import org.grails.datastore.gorm.neo4j.*
import org.grails.datastore.gorm.neo4j.collection.Neo4jResultList
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.ToMany
import org.grails.datastore.mapping.model.types.ToOne
import org.grails.datastore.mapping.query.AssociationQuery
import org.grails.datastore.mapping.query.Query
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.Result

/**
 * perform criteria queries on a Neo4j backend
 *
 * @author Stefan Armbruster <stefan@armbruster-it.de>
 * @author Graeme Rocher
 */
@CompileStatic
@Commons
class Neo4jQuery extends Query {

    private static final String ORDER_BY_CLAUSE = " ORDER BY "
    private static final String BLANK = ""
    private static final String ID_EQUALS = "ID(n)"

    final Neo4jEntityPersister neo4jEntityPersister


    public Neo4jQuery(Neo4jSession session, PersistentEntity entity, Neo4jEntityPersister neo4jEntityPersister) {
        super(session, entity)
        session.assertTransaction();
        this.neo4jEntityPersister = neo4jEntityPersister
    }

    private static Map<Class<? extends Query.Criterion>, String> COMPARISON_OPERATORS = [
            (Query.GreaterThanEqualsProperty): CriterionHandler.OPERATOR_GREATER_THAN_EQUALS,
            (Query.EqualsProperty): CriterionHandler.OPERATOR_EQUALS,
            (Query.NotEqualsProperty): CriterionHandler.OPERATOR_NOT_EQUALS,
            (Query.LessThanEqualsProperty): CriterionHandler.OPERATOR_LESS_THAN_EQUALS,
            (Query.LessThanProperty): CriterionHandler.OPERATOR_LESS_THAN,
            (Query.GreaterThanProperty): CriterionHandler.OPERATOR_GREATER_THAN
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
                    Neo4jMappingContext mappingContext = (Neo4jMappingContext)entity.mappingContext
                    int paramNumber = builder.addParam( mappingContext.convertToNative(criterion.value) )
                    def association = entity.getPropertyByName(criterion.property)
                    String lhs
                    if (association instanceof Association) {
                        def targetNodeName = "m_${builder.getNextMatchNumber()}"
                        builder.addMatch("(${prefix})${matchForAssociation(association)}(${targetNodeName})")

                        def graphEntity = (GraphPersistentEntity) ((Association) association).associatedEntity
                        if(graphEntity.idGenerator == null) {
                            lhs = "ID(${targetNodeName})"
                        }
                        else {
                            lhs = "${targetNodeName}.${CypherBuilder.IDENTIFIER}"
                        }
                    } else {
                        def graphEntity = (GraphPersistentEntity) entity
                        if(graphEntity.idGenerator == null) {
                            lhs = criterion.property == "id" ? "ID(${prefix})" : "${prefix}.${criterion.property}"
                        }
                        else {
                            lhs = criterion.property == "id" ? "${prefix}.${CypherBuilder.IDENTIFIER}" : "${prefix}.${criterion.property}"
                        }
                    }

                    return new CypherExpression(lhs, "{$paramNumber}", CriterionHandler.OPERATOR_EQUALS)
                }
            },
            (Query.IdEquals): new CriterionHandler<Query.IdEquals>() {
                @Override
                @CompileStatic
                CypherExpression handle(PersistentEntity entity, Query.IdEquals criterion, CypherBuilder builder, String prefix) {
                    int paramNumber = addBuildParameterForCriterion(builder, entity, criterion)
                    GraphPersistentEntity graphPersistentEntity = (GraphPersistentEntity)entity
                    if(graphPersistentEntity.idGenerator == null) {
                        return new CypherExpression(ID_EQUALS, "{$paramNumber}", CriterionHandler.OPERATOR_EQUALS)
                    }
                    else {
                        return new CypherExpression("${prefix}.${CypherBuilder.IDENTIFIER}", "{$paramNumber}", CriterionHandler.OPERATOR_EQUALS)
                    }
                }
            },
            (Query.Like): new CriterionHandler<Query.Like>() {
                @Override
                @CompileStatic
                CypherExpression handle(PersistentEntity entity, Query.Like criterion, CypherBuilder builder, String prefix) {
                    int paramNumber = addBuildParameterForCriterion(builder, entity, criterion)
                    builder.replaceParamAt(paramNumber, Query.patternToRegex(criterion.value))
                    return new CypherExpression("${prefix}.$criterion.property", "{$paramNumber}", CriterionHandler.OPERATOR_LIKE)
                }
            },
            (Query.ILike): new CriterionHandler<Query.ILike>() {
                @Override
                @CompileStatic
                CypherExpression handle(PersistentEntity entity, Query.ILike criterion, CypherBuilder builder, String prefix) {
                    int paramNumber = addBuildParameterForCriterion(builder, entity, criterion)
                    def pattern = Query.patternToRegex(criterion.value)
                    builder.replaceParamAt(paramNumber, "(?i)${pattern}".toString())
                    return new CypherExpression("${prefix}.$criterion.property", "{$paramNumber}", CriterionHandler.OPERATOR_LIKE)
                }
            },
            (Query.RLike): new CriterionHandler<Query.RLike>() {
                @Override
                @CompileStatic
                CypherExpression handle(PersistentEntity entity, Query.RLike criterion, CypherBuilder builder, String prefix) {
                    int paramNumber = addBuildParameterForCriterion(builder, entity, criterion)
                    return new CypherExpression("${prefix}.$criterion.property", "{$paramNumber}", CriterionHandler.OPERATOR_LIKE)
                }
            },
            (Query.In): new CriterionHandler<Query.In>() {
                @Override
                @CompileStatic
                CypherExpression handle(PersistentEntity entity, Query.In criterion, CypherBuilder builder, String prefix) {
                    int paramNumber = addBuildParameterForCriterion(builder, entity, criterion)
                    GraphPersistentEntity graphPersistentEntity = (GraphPersistentEntity)entity
                    String lhs
                    if(graphPersistentEntity.idGenerator == null) {
                        lhs = criterion.property == "id" ? "ID(${prefix})" : "${prefix}.$criterion.property"
                    }
                    else {
                        lhs = criterion.property == "id" ? "${prefix}.${CypherBuilder.IDENTIFIER}" : "${prefix}.$criterion.property"
                    }
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
                    int paramNumber = addBuildParameterForCriterion(builder, entity, criterion)
                    Neo4jMappingContext mappingContext = (Neo4jMappingContext)entity.mappingContext
                    int paramNumberFrom = builder.addParam( mappingContext.convertToNative(criterion.from) )
                    int parmaNumberTo = builder.addParam( mappingContext.convertToNative(criterion.to) )
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

        CypherBuilder cypherBuilder = buildBaseQuery(persistentEntity, criteria)
        cypherBuilder.setOrderAndLimits(applyOrderAndLimits(cypherBuilder))

        def projectionList = projections.projectionList

        if(projectionList.isEmpty()) {
             if(persistentEntity.associations.size() > 0) {
                 int i = 0;
                 List<String> rs = []
                 List<String> os = []
                 cypherBuilder.addReturnColumn(CypherBuilder.DEFAULT_RETURN_TYPES)

                 for(Association a in persistentEntity.associations) {
                     def associationMatch = matchForAssociation(a)
                     def fetchType = fetchStrategy(a.name)
                     String r = "r${i++}";
                     String o = "o${i}";
                     def associationName = a.name
                     GraphPersistentEntity graphPersistentEntity = (GraphPersistentEntity)a.associatedEntity

                     if( a instanceof ToMany ) {
                         boolean isLazy = ((ToMany)a).lazy
                         if(fetchType.is(fetchType.EAGER)) {


                             rs.add(r)
                             os.add(o)
                             // if there are associations, add a join to get them
                             cypherBuilder.addOptionalMatch("(n)${associationMatch}(${associationName}Node)")
                             if(isLazy) {
                                 if(graphPersistentEntity.idGenerator == null) {
                                     cypherBuilder.addReturnColumn("collect(DISTINCT ID(${associationName}Node)) as ${associationName}Ids")
                                 }
                                 else {
                                     cypherBuilder.addReturnColumn("collect(DISTINCT ${associationName}Node.${CypherBuilder.IDENTIFIER}) as ${associationName}Ids")
                                 }
                             }
                             else {
                                 cypherBuilder.addReturnColumn("collect(DISTINCT ${associationName}Node) as ${associationName}Nodes")
                             }
                         }
                     }
                     else if(a instanceof ToOne) {

                         rs.add(r)
                         os.add(o)
                         // if there are associations, add a join to get them
                         cypherBuilder.addOptionalMatch("(n)${associationMatch}(${associationName}Node)")
                         if(!fetchType.is(fetchType.EAGER)) {
                             if(graphPersistentEntity.idGenerator == null) {
                                 cypherBuilder.addReturnColumn("collect(DISTINCT ID(${associationName}Node)) as ${associationName}Ids")
                             }
                             else {
                                 cypherBuilder.addReturnColumn("collect(DISTINCT ${associationName}Node.${CypherBuilder.IDENTIFIER}) as ${associationName}Ids")
                             }

                         }
                         else {
                             cypherBuilder.addReturnColumn("collect(DISTINCT ${associationName}Node) as ${associationName}Nodes")
                         }
                     }
                 }
             }
        }
        else {
            for (projection in projectionList) {
                cypherBuilder.addReturnColumn(buildProjection(projection, cypherBuilder))
            }
        }


        def cypher = cypherBuilder.build()
        def params = cypherBuilder.getParams()

        log.debug("QUERY Cypher [$cypher] for params [$params]")

        Result executionResult = params.isEmpty() ? graphDatabaseService.execute(cypher) : graphDatabaseService.execute(cypher, params)
        if (projectionList.empty) {
            return new Neo4jResultList(offset, executionResult, neo4jEntityPersister, lockResult)
        } else {

            def projectedResults = executionResult.collect { Map<String, Object> row ->
                def columnNames = executionResult.columns()
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

                            def data = Collections.<String,Object>singletonMap( CypherBuilder.NODE_DATA, childNode)
                            return persister.unmarshallOrFromCache(
                                                association.associatedEntity, data)
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

    /**
     * Obtains the root query for this Neo4jQuery instance without any RETURN statements, projections or limits applied
     *
     * @return The base query containing only the conditions
     */
    CypherBuilder getBaseQuery() {
        buildBaseQuery(entity, criteria)
    }

    protected CypherBuilder buildBaseQuery(PersistentEntity persistentEntity, Query.Junction criteria) {
        CypherBuilder cypherBuilder = new CypherBuilder(((GraphPersistentEntity) persistentEntity).getLabelsAsString());
        def conditions = buildConditions(criteria, cypherBuilder, CypherBuilder.NODE_VAR)
        cypherBuilder.setConditions(conditions)
        cypherBuilder
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

    public static String matchForAssociation(Association association, String var = "") {
        def relationshipType = RelationshipUtils.relationshipTypeUsedFor(association)
        def reversed = RelationshipUtils.useReversedMappingFor(association)
        StringBuilder sb = new StringBuilder();
        if (reversed) {
            sb.append('<')
        }
        sb.append("-[$var:").append(relationshipType).append("]-")
        if (!reversed) {
            sb.append('>')
        }
        sb.toString()
    }

    private static int addBuildParameterForCriterion(CypherBuilder builder, PersistentEntity entity, Query.PropertyCriterion criterion) {
        Neo4jMappingContext mappingContext = (Neo4jMappingContext)entity.mappingContext
        return builder.addParam( mappingContext.convertToNative(criterion.value) )
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
            int paramNumber = addBuildParameterForCriterion(builder, entity, criterion)
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
            int paramNumber = addBuildParameterForCriterion(builder, entity, criterion)
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


