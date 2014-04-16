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
package org.grails.datastore.mapping.mongo.query;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;

import com.mongodb.*;
import grails.mongodb.geo.*;
import groovy.lang.Closure;
import org.bson.BasicBSONObject;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.grails.datastore.gorm.mongo.geo.GeoJSONType;
import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.core.SessionImplementor;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.grails.datastore.mapping.engine.internal.MappingUtils;
import org.grails.datastore.mapping.engine.types.CustomTypeMarshaller;
import org.grails.datastore.mapping.model.EmbeddedPersistentEntity;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.types.Association;
import org.grails.datastore.mapping.model.types.Custom;
import org.grails.datastore.mapping.model.types.Embedded;
import org.grails.datastore.mapping.model.types.EmbeddedCollection;
import org.grails.datastore.mapping.model.types.ToOne;
import org.grails.datastore.mapping.mongo.MongoSession;
import org.grails.datastore.mapping.mongo.config.MongoAttribute;
import org.grails.datastore.mapping.mongo.config.MongoCollection;
import org.grails.datastore.mapping.mongo.engine.MongoEntityPersister;
import org.grails.datastore.mapping.query.AssociationQuery;
import org.grails.datastore.mapping.query.Query;
import org.grails.datastore.mapping.query.Restrictions;
import org.grails.datastore.mapping.query.api.QueryArgumentsAware;
import org.grails.datastore.mapping.query.projections.ManualProjections;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.data.mongodb.core.DbCallback;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

/**
 * A {@link org.grails.datastore.mapping.query.Query} implementation for the Mongo document store.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings("rawtypes")
public class MongoQuery extends Query implements QueryArgumentsAware {

    public static final String PROJECT_OPERATOR = "$project";
    public static final String SORT_OPERATOR = "$sort";
    private static Map<Class, QueryHandler> queryHandlers = new HashMap<Class, QueryHandler>();
    private static Map<Class, QueryHandler> negatedHandlers = new HashMap<Class, QueryHandler>();
    private static Map<Class, ProjectionHandler> groupByProjectionHandlers = new HashMap<Class, ProjectionHandler>();
    private static Map<Class, ProjectionHandler> projectProjectionHandlers = new HashMap<Class, ProjectionHandler>();


    public static final String MONGO_IN_OPERATOR = "$in";
    public static final String MONGO_OR_OPERATOR = "$or";
    public static final String MONGO_AND_OPERATOR = "$and";
    public static final String MONGO_GTE_OPERATOR = "$gte";
    public static final String MONGO_LTE_OPERATOR = "$lte";
    public static final String MONGO_GT_OPERATOR = "$gt";
    public static final String MONGO_LT_OPERATOR = "$lt";
    public static final String MONGO_NE_OPERATOR = "$ne";
    public static final String MONGO_NIN_OPERATOR = "$nin";
    public static final String MONGO_ID_REFERENCE_SUFFIX = ".$id";
    public static final String MONGO_WHERE_OPERATOR = "$where";

    private static final String MONGO_THIS_PREFIX = "this.";
    public static final String HINT_ARGUMENT = "hint";

    private Map queryArguments = Collections.emptyMap();

    public static final String NEAR_OPERATOR = "$near";

    public static final String BOX_OPERATOR = "$box";

    public static final String POLYGON_OPERATOR = "$polygon";

    public static final String WITHIN_OPERATOR = "$within";

    public static final String CENTER_OPERATOR = "$center";

    public static final String GEO_WITHIN_OPERATOR = "$geoWithin";

    public static final String GEOMETRY_OPERATOR = "$geometry";

    public static final String CENTER_SPHERE_OPERATOR = "$centerSphere";

    public static final String GEO_INTERSECTS_OPERATOR = "$geoIntersects";

    public static final String MAX_DISTANCE_OPERATOR = "$maxDistance";

    public static final String NEAR_SPHERE_OPERATOR = "$nearSphere";

    public static final String MONGO_REGEX_OPERATOR = "$regex";

    public static final String MATCH_OPERATOR = "$match";

    public static final String AVERAGE_OPERATOR = "$avg";

    public static final String GROUP_OPERATOR = "$group";

    public static final String SUM_OPERATOR = "$sum";

    public static final String MIN_OPERATOR = "$min";

    public static final String MAX_OPERATOR = "$max";

    static {
        queryHandlers.put(IdEquals.class, new QueryHandler<IdEquals>() {
            public void handle(PersistentEntity entity, IdEquals criterion, DBObject query) {
                query.put(MongoEntityPersister.MONGO_ID_FIELD, criterion.getValue());
            }
        });

        queryHandlers.put(AssociationQuery.class, new QueryHandler<AssociationQuery>() {
            public void handle(PersistentEntity entity, AssociationQuery criterion, DBObject query) {
                Association<?> association = criterion.getAssociation();
                PersistentEntity associatedEntity = association.getAssociatedEntity();
                if (association instanceof EmbeddedCollection) {
                    BasicDBObject associationCollectionQuery = new BasicDBObject();
                    populateMongoQuery(associatedEntity, associationCollectionQuery, criterion.getCriteria());
                    BasicDBObject collectionQuery = new BasicDBObject("$elemMatch", associationCollectionQuery);
                    String propertyKey = getPropertyName(entity, association.getName());
                    query.put(propertyKey, collectionQuery);
                }
                else if (associatedEntity instanceof EmbeddedPersistentEntity || association instanceof Embedded ) {
                    BasicDBObject associatedEntityQuery = new BasicDBObject();
                    populateMongoQuery(associatedEntity, associatedEntityQuery, criterion.getCriteria());
                    for (String property : associatedEntityQuery.keySet()) {
                        String propertyKey = getPropertyName(entity, association.getName());
                        query.put(propertyKey + '.' + property, associatedEntityQuery.get(property));
                    }
                }
                else {
                    throw new UnsupportedOperationException("Join queries are not supported by MongoDB");
                }
            }
        });

        queryHandlers.put(Equals.class, new QueryHandler<Equals>() {
            public void handle(PersistentEntity entity, Equals criterion, DBObject query) {
                String propertyName = getPropertyName(entity, criterion);
                Object value = criterion.getValue();
                if(value instanceof Pattern) {
                    Pattern pattern = (Pattern) value;
                    query.put(propertyName, new BasicDBObject(MONGO_REGEX_OPERATOR, pattern.toString()));
                }
                else {
                    MongoEntityPersister.setDBObjectValue(query, propertyName, value, entity.getMappingContext());
                }
            }
        });

        queryHandlers.put(IsNull.class, new QueryHandler<IsNull>() {
            @SuppressWarnings("unchecked")
            public void handle(PersistentEntity entity, IsNull criterion, DBObject query) {
                queryHandlers.get(Equals.class).handle(entity, new Equals(criterion.getProperty(), null), query);
            }
        });
        queryHandlers.put(IsNotNull.class, new QueryHandler<IsNotNull>() {
            @SuppressWarnings("unchecked")
            public void handle(PersistentEntity entity, IsNotNull criterion, DBObject query) {
                queryHandlers.get(NotEquals.class).handle(entity, new NotEquals(criterion.getProperty(), null), query);
            }
        });
        queryHandlers.put(EqualsProperty.class, new QueryHandler<EqualsProperty>() {
            public void handle(PersistentEntity entity, EqualsProperty criterion, DBObject query) {
                String propertyName = getPropertyName(entity, criterion);
                String otherPropertyName = getPropertyName(entity, criterion.getOtherProperty());
                addWherePropertyComparison(query, propertyName, otherPropertyName, "==");
            }
        });
        queryHandlers.put(NotEqualsProperty.class, new QueryHandler<NotEqualsProperty>() {
            public void handle(PersistentEntity entity, NotEqualsProperty criterion, DBObject query) {
                String propertyName = getPropertyName(entity, criterion);
                String otherPropertyName = getPropertyName(entity, criterion.getOtherProperty());
                addWherePropertyComparison(query, propertyName, otherPropertyName, "!=");
            }
        });
        queryHandlers.put(GreaterThanProperty.class, new QueryHandler<GreaterThanProperty>() {
            public void handle(PersistentEntity entity, GreaterThanProperty criterion, DBObject query) {
                String propertyName = getPropertyName(entity, criterion);
                String otherPropertyName = getPropertyName(entity, criterion.getOtherProperty());
                addWherePropertyComparison(query, propertyName, otherPropertyName, ">");
            }
        });
        queryHandlers.put(LessThanProperty.class, new QueryHandler<LessThanProperty>() {
            public void handle(PersistentEntity entity, LessThanProperty criterion, DBObject query) {
                String propertyName = getPropertyName(entity, criterion);
                String otherPropertyName = getPropertyName(entity, criterion.getOtherProperty());
                addWherePropertyComparison(query, propertyName, otherPropertyName, "<");
            }
        });
        queryHandlers.put(GreaterThanEqualsProperty.class, new QueryHandler<GreaterThanEqualsProperty>() {
            public void handle(PersistentEntity entity, GreaterThanEqualsProperty criterion, DBObject query) {
                String propertyName = getPropertyName(entity, criterion);
                String otherPropertyName = getPropertyName(entity, criterion.getOtherProperty());
                addWherePropertyComparison(query, propertyName, otherPropertyName, ">=");
            }
        });
        queryHandlers.put(LessThanEqualsProperty.class, new QueryHandler<LessThanEqualsProperty>() {
            public void handle(PersistentEntity entity, LessThanEqualsProperty criterion, DBObject query) {
                String propertyName = getPropertyName(entity, criterion);
                String otherPropertyName = getPropertyName(entity, criterion.getOtherProperty());
                addWherePropertyComparison(query, propertyName, otherPropertyName, "<=");
            }
        });

        queryHandlers.put(NotEquals.class, new QueryHandler<NotEquals>() {
            public void handle(PersistentEntity entity, NotEquals criterion, DBObject query) {
                String propertyName = getPropertyName(entity, criterion);
                DBObject notEqualQuery = getOrCreatePropertyQuery(query, propertyName);
                MongoEntityPersister.setDBObjectValue(notEqualQuery, MONGO_NE_OPERATOR, criterion.getValue(), entity.getMappingContext());

                query.put(propertyName, notEqualQuery);
            }
        });

        queryHandlers.put(Like.class, new QueryHandler<Like>() {
            public void handle(PersistentEntity entity, Like like, DBObject query) {
                handleLike(entity, like, query, true);
            }
        });

      queryHandlers.put(ILike.class, new QueryHandler<ILike>() {
            public void handle(PersistentEntity entity, ILike like, DBObject query) {
                handleLike(entity, like, query, false);
            }
        });

        queryHandlers.put(RLike.class, new QueryHandler<RLike>() {
            public void handle(PersistentEntity entity, RLike like, DBObject query) {
                Object value = like.getValue();
                if (value == null) value = "null";
                final String expr = value.toString();
                Pattern regex = Pattern.compile(expr);
                String propertyName = getPropertyName(entity, like);
                query.put(propertyName, regex);
            }
        });

        queryHandlers.put(In.class, new QueryHandler<In>() {
            public void handle(PersistentEntity entity, In in, DBObject query) {
                DBObject inQuery = new BasicDBObject();
                List values = getInListQueryValues(entity, in);
                inQuery.put(MONGO_IN_OPERATOR, values);
                String propertyName = getPropertyName(entity, in);
                query.put(propertyName, inQuery);
            }
        });



        queryHandlers.put(WithinBox.class, new QueryHandler<WithinBox>() {
            public void handle(PersistentEntity entity, WithinBox withinBox, DBObject query) {
                DBObject nearQuery = new BasicDBObject();
                DBObject box = new BasicDBObject();
                MongoEntityPersister.setDBObjectValue(box, BOX_OPERATOR, withinBox.getValues(), entity.getMappingContext());
                nearQuery.put(WITHIN_OPERATOR, box);
                String propertyName = getPropertyName(entity, withinBox);
                query.put(propertyName, nearQuery);
            }
        });

        queryHandlers.put(WithinPolygon.class, new QueryHandler<WithinPolygon>() {
            public void handle(PersistentEntity entity, WithinPolygon withinPolygon, DBObject query) {
                DBObject nearQuery = new BasicDBObject();
                DBObject box = new BasicDBObject();
                MongoEntityPersister.setDBObjectValue(box, POLYGON_OPERATOR, withinPolygon.getValues(), entity.getMappingContext());
                nearQuery.put(WITHIN_OPERATOR, box);
                String propertyName = getPropertyName(entity, withinPolygon);
                query.put(propertyName, nearQuery);
            }
        });

        queryHandlers.put(WithinCircle.class, new QueryHandler<WithinCircle>() {
            public void handle(PersistentEntity entity, WithinCircle withinCentre, DBObject query) {
                DBObject nearQuery = new BasicDBObject();
                DBObject center = new BasicDBObject();
                MongoEntityPersister.setDBObjectValue(center, CENTER_OPERATOR, withinCentre.getValues(), entity.getMappingContext());
                nearQuery.put(WITHIN_OPERATOR, center);
                String propertyName = getPropertyName(entity, withinCentre);
                query.put(propertyName, nearQuery);
            }
        });

        QueryHandler<Near> nearHandler = new QueryHandler<Near>() {
            public void handle(PersistentEntity entity, Near near, DBObject query) {
                DBObject nearQuery = new BasicDBObject();
                Object value = near.getValue();
                String nearOperator = near instanceof NearSphere ? NEAR_SPHERE_OPERATOR : NEAR_OPERATOR;
                if ((value instanceof List) || (value instanceof Map)) {
                    MongoEntityPersister.setDBObjectValue(nearQuery, nearOperator, value, entity.getMappingContext());
                } else if (value instanceof Point) {
                    BasicBSONObject geoJson = GeoJSONType.convertToGeoJSON((Point) value);
                    BasicDBObject geometry = new BasicDBObject();
                    geometry.put(GEOMETRY_OPERATOR, geoJson);
                    if (near.maxDistance != null) {
                        geometry.put(MAX_DISTANCE_OPERATOR, near.maxDistance.getValue());
                    }
                    nearQuery.put(nearOperator, geometry);
                }

                String propertyName = getPropertyName(entity, near);
                query.put(propertyName, nearQuery);
            }
        };
        queryHandlers.put(Near.class, nearHandler);
        queryHandlers.put(NearSphere.class, nearHandler);

        queryHandlers.put(GeoWithin.class, new QueryHandler<GeoWithin>() {
            public void handle(PersistentEntity entity, GeoWithin geoWithin, DBObject query) {
                DBObject queryRoot = new BasicDBObject();
                BasicDBObject queryGeoWithin = new BasicDBObject();
                queryRoot.put(GEO_WITHIN_OPERATOR, queryGeoWithin);
                String targetProperty = getPropertyName(entity, geoWithin);
                Object value = geoWithin.getValue();
                if(value instanceof Shape) {
                    Shape shape = (Shape)value;
                    if(shape instanceof Polygon) {
                        Polygon p = (Polygon) shape;
                        BasicBSONObject geoJson = GeoJSONType.convertToGeoJSON(p);
                        queryGeoWithin.put(GEOMETRY_OPERATOR, geoJson);
                    }
                    else if(shape instanceof Box) {
                        queryGeoWithin.put(BOX_OPERATOR, shape.asList());
                    }
                    else if(shape instanceof Circle) {
                        queryGeoWithin.put(CENTER_OPERATOR, shape.asList());
                    }
                    else if(shape instanceof Sphere) {
                        queryGeoWithin.put(CENTER_SPHERE_OPERATOR, shape.asList());
                    }
                }
                else if(value instanceof Map) {
                    queryGeoWithin.putAll((Map)value);
                }

                query.put(targetProperty, queryRoot);
            }
        });

        queryHandlers.put(GeoIntersects.class, new QueryHandler<GeoIntersects>() {
            public void handle(PersistentEntity entity, GeoIntersects geoIntersects, DBObject query) {
                DBObject queryRoot = new BasicDBObject();
                BasicDBObject queryGeoWithin = new BasicDBObject();
                queryRoot.put(GEO_INTERSECTS_OPERATOR, queryGeoWithin);
                String targetProperty = getPropertyName(entity, geoIntersects);
                Object value = geoIntersects.getValue();
                if(value instanceof GeoJSON) {
                    Shape shape = (Shape)value;
                    BasicBSONObject geoJson = GeoJSONType.convertToGeoJSON(shape);
                    queryGeoWithin.put(GEOMETRY_OPERATOR, geoJson);
                }
                else if(value instanceof Map) {
                    queryGeoWithin.putAll((Map)value);
                }

                query.put(targetProperty, queryRoot);
            }
        });

        queryHandlers.put(Between.class, new QueryHandler<Between>() {
            public void handle(PersistentEntity entity, Between between, DBObject query) {
                DBObject betweenQuery = new BasicDBObject();
                MongoEntityPersister.setDBObjectValue(betweenQuery, MONGO_GTE_OPERATOR, between.getFrom(), entity.getMappingContext());
                MongoEntityPersister.setDBObjectValue(betweenQuery, MONGO_LTE_OPERATOR, between.getTo(), entity.getMappingContext());
                String propertyName = getPropertyName(entity, between);
                query.put(propertyName, betweenQuery);
            }
        });

        queryHandlers.put(GreaterThan.class, new QueryHandler<GreaterThan>() {
            public void handle(PersistentEntity entity, GreaterThan criterion, DBObject query) {
                String propertyName = getPropertyName(entity, criterion);
                DBObject greaterThanQuery = getOrCreatePropertyQuery(query, propertyName);
                MongoEntityPersister.setDBObjectValue(greaterThanQuery, MONGO_GT_OPERATOR, criterion.getValue(), entity.getMappingContext());

                query.put(propertyName, greaterThanQuery);
            }
        });

        queryHandlers.put(GreaterThanEquals.class, new QueryHandler<GreaterThanEquals>() {
            public void handle(PersistentEntity entity, GreaterThanEquals criterion, DBObject query) {
                String propertyName = getPropertyName(entity, criterion);
                DBObject greaterThanQuery = getOrCreatePropertyQuery(query, propertyName);
                MongoEntityPersister.setDBObjectValue(greaterThanQuery, MONGO_GTE_OPERATOR, criterion.getValue(), entity.getMappingContext());

                query.put(propertyName, greaterThanQuery);
            }
        });

        queryHandlers.put(LessThan.class, new QueryHandler<LessThan>() {
            public void handle(PersistentEntity entity, LessThan criterion, DBObject query) {
                String propertyName = getPropertyName(entity, criterion);
                DBObject lessThanQuery = getOrCreatePropertyQuery(query, propertyName);
                MongoEntityPersister.setDBObjectValue(lessThanQuery, MONGO_LT_OPERATOR, criterion.getValue(), entity.getMappingContext());

                query.put(propertyName, lessThanQuery);
            }
        });

        queryHandlers.put(LessThanEquals.class, new QueryHandler<LessThanEquals>() {
            public void handle(PersistentEntity entity, LessThanEquals criterion, DBObject query) {
                String propertyName = getPropertyName(entity, criterion);
                DBObject lessThanQuery = getOrCreatePropertyQuery(query, propertyName);
                MongoEntityPersister.setDBObjectValue(lessThanQuery, MONGO_LTE_OPERATOR, criterion.getValue(), entity.getMappingContext());

                query.put(propertyName, lessThanQuery);
            }
        });

        queryHandlers.put(Conjunction.class, new QueryHandler<Conjunction>() {
            public void handle(PersistentEntity entity, Conjunction criterion, DBObject query) {
                populateMongoQuery(entity, query, criterion);
            }
        });

        queryHandlers.put(Negation.class, new QueryHandler<Negation>() {
            @SuppressWarnings("unchecked")
            public void handle(PersistentEntity entity, Negation criteria, DBObject query) {
                for (Criterion criterion : criteria.getCriteria()) {
                    final QueryHandler queryHandler = negatedHandlers.get(criterion.getClass());
                    if (queryHandler != null) {
                        queryHandler.handle(entity, criterion, query);
                    }
                    else {
                        throw new UnsupportedOperationException("Query of type "+criterion.getClass().getSimpleName()+" cannot be negated");
                    }
                }
            }
        });

        queryHandlers.put(Disjunction.class, new QueryHandler<Disjunction>() {
            @SuppressWarnings("unchecked")
            public void handle(PersistentEntity entity, Disjunction criterion, DBObject query) {
                populateMongoQuery(entity, query, criterion);
            }
        });

        negatedHandlers.put(Equals.class, new QueryHandler<Equals>() {
            @SuppressWarnings("unchecked")
            public void handle(PersistentEntity entity, Equals criterion, DBObject query) {
                queryHandlers.get(NotEquals.class).handle(entity, Restrictions.ne(criterion.getProperty(), criterion.getValue()), query);
            }
        });

        negatedHandlers.put(NotEquals.class, new QueryHandler<NotEquals>() {
            @SuppressWarnings("unchecked")
            public void handle(PersistentEntity entity, NotEquals criterion, DBObject query) {
                queryHandlers.get(Equals.class).handle(entity, Restrictions.eq(criterion.getProperty(), criterion.getValue()), query);
            }
        });

        negatedHandlers.put(In.class, new QueryHandler<In>() {
            public void handle(PersistentEntity entity, In in, DBObject query) {
                Object nativePropertyValue = getInListQueryValues(entity, in);
                String property = getPropertyName(entity, in);
                DBObject inQuery = getOrCreatePropertyQuery(query, property);
                inQuery.put(MONGO_NIN_OPERATOR, nativePropertyValue);
                query.put(property, inQuery);
            }
        });

        negatedHandlers.put(Between.class, new QueryHandler<Between>() {
            public void handle(PersistentEntity entity, Between between, DBObject query) {
                String property = getPropertyName(entity, between);
                DBObject betweenQuery = getOrCreatePropertyQuery(query, property);
                betweenQuery.put(MONGO_LTE_OPERATOR, between.getFrom());
                betweenQuery.put(MONGO_GTE_OPERATOR, between.getTo());
                query.put(property, betweenQuery);
            }
        });

        negatedHandlers.put(GreaterThan.class, new QueryHandler<GreaterThan>() {
            @SuppressWarnings("unchecked")
            public void handle(PersistentEntity entity, GreaterThan criterion, DBObject query) {
                queryHandlers.get(LessThan.class).handle(entity, Restrictions.lt(criterion.getProperty(), criterion.getValue()), query);
            }
        });

        negatedHandlers.put(GreaterThanEquals.class, new QueryHandler<GreaterThanEquals>() {
            @SuppressWarnings("unchecked")
            public void handle(PersistentEntity entity, GreaterThanEquals criterion, DBObject query) {
                queryHandlers.get(LessThanEquals.class).handle(entity, Restrictions.lte(criterion.getProperty(), criterion.getValue()), query);
            }
        });

        negatedHandlers.put(LessThan.class, new QueryHandler<LessThan>() {
            @SuppressWarnings("unchecked")
            public void handle(PersistentEntity entity, LessThan criterion, DBObject query) {
                queryHandlers.get(GreaterThan.class).handle(entity, Restrictions.gt(criterion.getProperty(), criterion.getValue()), query);
            }
        });

        negatedHandlers.put(LessThanEquals.class, new QueryHandler<LessThanEquals>() {
            @SuppressWarnings("unchecked")
            public void handle(PersistentEntity entity, LessThanEquals criterion, DBObject query) {
                queryHandlers.get(GreaterThanEquals.class).handle(entity, Restrictions.gte(criterion.getProperty(), criterion.getValue()), query);
            }
        });

        groupByProjectionHandlers.put(AvgProjection.class, new ProjectionHandler<AvgProjection>() {
            @Override
            public String handle(PersistentEntity entity, DBObject projectObject, DBObject groupBy, AvgProjection projection) {
                return addProjectionToGroupBy(projectObject, groupBy,projection, AVERAGE_OPERATOR, "avg_");
            }
        });
        groupByProjectionHandlers.put(CountProjection.class, new ProjectionHandler<CountProjection>() {
            @Override
            public String handle(PersistentEntity entity, DBObject projectObject, DBObject groupBy, CountProjection projection) {
                projectObject.put(MongoEntityPersister.MONGO_ID_FIELD, 1);
                String projectionKey = "count";
                groupBy.put(projectionKey, new BasicDBObject(SUM_OPERATOR, 1));
                return projectionKey;
            }
        });
        groupByProjectionHandlers.put(CountDistinctProjection.class, new ProjectionHandler<CountDistinctProjection>() {
            @Override
            // equivalent of "select count (distinct fieldName) from someTable". Example:
            // db.someCollection.aggregate([{ $group: { _id: "$fieldName"}  },{ $group: { _id: 1, count: { $sum: 1 } } } ])
            public String handle(PersistentEntity entity, DBObject projectObject, DBObject groupBy, CountDistinctProjection projection) {
                projectObject.put(projection.getPropertyName(), 1);
                String property = projection.getPropertyName();
                String projectionValueKey = "countDistinct_" + property;
                DBObject id = getIdObjectForGroupBy(groupBy);
                id.put(property, "$"+property);
                return projectionValueKey;
            }
        });

        groupByProjectionHandlers.put(MinProjection.class, new ProjectionHandler<MinProjection>() {
            @Override
            public String handle(PersistentEntity entity, DBObject projectObject, DBObject groupBy, MinProjection projection) {
                return addProjectionToGroupBy(projectObject, groupBy, projection, MIN_OPERATOR, "min_");
            }
        });
        groupByProjectionHandlers.put(MaxProjection.class, new ProjectionHandler<MaxProjection>() {
            @Override
            public String handle(PersistentEntity entity, DBObject projectObject, DBObject groupBy, MaxProjection projection) {
                return addProjectionToGroupBy(projectObject, groupBy, projection, MAX_OPERATOR, "max_");
            }
        });
        groupByProjectionHandlers.put(SumProjection.class, new ProjectionHandler<SumProjection>() {
            @Override
            public String handle(PersistentEntity entity, DBObject projectObject, DBObject groupBy, SumProjection projection) {
                return addProjectionToGroupBy(projectObject, groupBy, projection, SUM_OPERATOR, "sum_");
            }
        });

        projectProjectionHandlers.put(DistinctPropertyProjection.class, new ProjectionHandler<DistinctPropertyProjection>() {
            @Override
            public String handle(PersistentEntity entity, DBObject projectObject, DBObject groupBy, DistinctPropertyProjection projection) {
                String property = projection.getPropertyName();
                projectObject.put(property, 1);
                DBObject id = getIdObjectForGroupBy(groupBy);
                id.put(property, "$"+property);
                return property;
            }
        });

        projectProjectionHandlers.put(PropertyProjection.class, new ProjectionHandler<PropertyProjection>() {
            @Override
            public String handle(PersistentEntity entity, DBObject projectObject, DBObject groupBy, PropertyProjection projection) {
                String property = projection.getPropertyName();
                projectObject.put(property, 1);
                DBObject id = getIdObjectForGroupBy(groupBy);
                id.put(property, "$"+property);
                // we add the id to the grouping to make it not distinct
                id.put(MongoEntityPersister.MONGO_ID_FIELD, "$"+MongoEntityPersister.MONGO_ID_FIELD);
                return property;
            }
        });

        projectProjectionHandlers.put(IdProjection.class, new ProjectionHandler<IdProjection>() {
            @Override
            public String handle(PersistentEntity entity, DBObject projectObject, DBObject groupBy, IdProjection projection) {
                projectObject.put(MongoEntityPersister.MONGO_ID_FIELD, 1);
                DBObject id = getIdObjectForGroupBy(groupBy);
                id.put(MongoEntityPersister.MONGO_ID_FIELD, "$_id");

                return MongoEntityPersister.MONGO_ID_FIELD;
            }
        });

    }

    private static DBObject getIdObjectForGroupBy(DBObject groupBy) {
        Object value = groupBy.get(MongoEntityPersister.MONGO_ID_FIELD);
        DBObject id;
        if(value instanceof DBObject) {
            id = (DBObject) value;
        }
        else {
            id = new BasicDBObject();
            groupBy.put(MongoEntityPersister.MONGO_ID_FIELD, id);
        }
        return id;
    }

    private static String addProjectionToGroupBy(DBObject projectObject, DBObject groupBy, PropertyProjection projection, String operator, String prefix) {
        projectObject.put(projection.getPropertyName(), 1);
        String property = projection.getPropertyName();
        String projectionValueKey = prefix + property;
        BasicDBObject averageProjection = new BasicDBObject(operator, "$" + property);
        groupBy.put(projectionValueKey, averageProjection);
        return projectionValueKey;
    }

    private static DBObject getOrCreatePropertyQuery(DBObject query, String propertyName) {
        Object existing = query.get(propertyName);
        DBObject queryObject = existing instanceof DBObject ? (DBObject) existing : null;
        if (queryObject == null) {
            queryObject = new BasicDBObject();
        }
        return queryObject;
    }

    private static void addWherePropertyComparison(DBObject query, String propertyName, String otherPropertyName, String operator) {
        query.put(MONGO_WHERE_OPERATOR, new StringBuilder(MONGO_THIS_PREFIX).append(propertyName).append(operator).append(MONGO_THIS_PREFIX).append(otherPropertyName).toString());
    }

    /**
     * Get the list of native values to use in the query. This converts entities to ids and other types to
     * their persisted types.
     * @param entity The entity
     * @param in The criterion
     * @return The list of native values suitable for passing to Mongo.
     */
    private static List<Object> getInListQueryValues(PersistentEntity entity, In in) {
        List<Object> values = new ArrayList<Object>(in.getValues().size());
        for (Object value : in.getValues()) {
            if (entity.getMappingContext().isPersistentEntity(value)) {
                PersistentEntity pe = entity.getMappingContext().getPersistentEntity(
                        value.getClass().getName());
                values.add(new EntityAccess(pe, value).getIdentifier());
            } else {
                value = MongoEntityPersister.getSimpleNativePropertyValue(value, entity.getMappingContext());
                values.add(value);
            }
        }
        return values;
    }

    private static void handleLike(PersistentEntity entity, Like like, DBObject query, boolean caseSensitive) {
        Object value = like.getValue();
        if (value == null) value = "null";

        String[] array = value.toString().split("%", -1);
        for (int i=0; i<array.length; i++) {
            array[i] = Pattern.quote(array[i]);
        }
        String expr = StringUtils.arrayToDelimitedString(array, ".*");
        if (!expr.startsWith(".*")) {
            expr = '^'+expr;
        }
        if (!expr.endsWith(".*")) {
            expr = expr+'$';
        }

        Pattern regex = caseSensitive ? Pattern.compile(expr) : Pattern.compile(expr, Pattern.CASE_INSENSITIVE);
        String propertyName = getPropertyName(entity, like);
        query.put(propertyName, regex);
    }

    private MongoSession mongoSession;
    private MongoEntityPersister mongoEntityPersister;
    private ManualProjections manualProjections;

    public MongoQuery(MongoSession session, PersistentEntity entity) {
        super(session, entity);
        this.mongoSession = session;
        this.manualProjections = new ManualProjections(entity);
        this.mongoEntityPersister = (MongoEntityPersister) session.getPersister(entity);
    }

    @Override
    protected void flushBeforeQuery() {
        // with Mongo we only flush the session if a transaction is not active to allow for session-managed transactions
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            super.flushBeforeQuery();
        }
    }

    /**
     * Gets the Mongo query for this query instance
     *
     * @return The Mongo query
     */
    public DBObject getMongoQuery() {
        DBObject query = createQueryObject(entity);
        populateMongoQuery(entity, query,criteria);
        return query;
    }

    @Override
    protected List executeQuery(final PersistentEntity entity, final Junction criteria) {
        final MongoTemplate template = mongoSession.getMongoTemplate(entity);

        return template.execute(new DbCallback<List>() {
            @SuppressWarnings("unchecked")
            public List doInDB(DB db) throws MongoException, DataAccessException {

                final DBCollection collection = db.getCollection(mongoEntityPersister.getCollectionName(entity));
                if (uniqueResult) {
                    final DBObject dbObject;
                    if (criteria.isEmpty()) {
                        if (entity.isRoot()) {
                            dbObject = collection.findOne();
                        }
                        else {
                            dbObject = collection.findOne(new BasicDBObject(
                                  MongoEntityPersister.MONGO_CLASS_FIELD, entity.getDiscriminator()));
                        }
                    }
                    else {
                        dbObject = collection.findOne(getMongoQuery());
                    }
                    return wrapObjectResultInList(createObjectFromDBObject(dbObject));
                }

                DBCursor cursor;
                DBObject query = createQueryObject(entity);


                final List<Projection> projectionList = projections().getProjectionList();
                if (projectionList.isEmpty()) {
                    cursor = executeQuery(entity, criteria, collection, query);
                    return (List)new MongoResultList(cursor,offset, mongoEntityPersister).clone();
                }

                populateMongoQuery(entity, query, criteria);
                List projectedResults = new ArrayList();
                List<DBObject> aggregationPipeline = new ArrayList<DBObject>();

                if(!query.keySet().isEmpty()) {
                    aggregationPipeline.add(new BasicDBObject(MATCH_OPERATOR, query));
                }

                List<Order> orderBy = getOrderBy();
                if(!orderBy.isEmpty()) {
                    BasicDBObject sortBy = new BasicDBObject();
                    DBObject sort = new BasicDBObject(SORT_OPERATOR, sortBy);
                    for (Order order : orderBy) {
                        sortBy.put(order.getProperty(), order.getDirection() == Order.Direction.ASC ? 1 : -1);
                    }

                    aggregationPipeline.add(sort);
                }


                List<ProjectedProperty> projectedKeys = new ArrayList<ProjectedProperty>();
                boolean singleResult = true;

                BasicDBObject projectObject = new BasicDBObject();



                BasicDBObject groupByObject = new BasicDBObject();
                groupByObject.put(MongoEntityPersister.MONGO_ID_FIELD, 0);
                BasicDBObject additionalGroupBy = null;


                for (Projection projection : projectionList) {
                    ProjectionHandler projectionHandler = projectProjectionHandlers.get(projection.getClass());
                    ProjectedProperty projectedProperty = new ProjectedProperty();
                    projectedProperty.projection = projection;
                    if(projection instanceof PropertyProjection) {
                        PropertyProjection propertyProjection = (PropertyProjection) projection;
                        PersistentProperty property = entity.getPropertyByName(propertyProjection.getPropertyName());
                        if(property != null) {
                            projectedProperty.property = property;
                        }
                        else {
                            throw new InvalidDataAccessResourceUsageException("Attempt to project on a non-existent project ["+propertyProjection.getPropertyName()+"]");
                        }
                    }
                    if(projectionHandler != null) {
                        singleResult = false;

                        String aggregationKey = projectionHandler.handle(entity, projectObject,groupByObject, projection);
                        aggregationKey = "id." + aggregationKey;
                        projectedProperty.projectionKey = aggregationKey;
                        projectedKeys.add(projectedProperty);
                    }
                    else {

                        projectionHandler = groupByProjectionHandlers.get(projection.getClass());
                        if(projectionHandler != null) {
                            projectedProperty.projectionKey = projectionHandler.handle(entity, projectObject,groupByObject, projection);
                            projectedKeys.add(projectedProperty);

                            if(projection instanceof CountDistinctProjection) {
                                BasicDBObject finalCount = new BasicDBObject(MongoEntityPersister.MONGO_ID_FIELD, 1);
                                finalCount.put(projectedProperty.projectionKey, new BasicDBObject(SUM_OPERATOR, 1));
                                additionalGroupBy = new BasicDBObject(GROUP_OPERATOR, finalCount);
                            }
                        }

                    }
                }

                if(!projectObject.isEmpty()) {
                    aggregationPipeline.add(new BasicDBObject(PROJECT_OPERATOR, projectObject));
                }

                aggregationPipeline.add(new BasicDBObject(GROUP_OPERATOR, groupByObject));

                if(additionalGroupBy != null) {
                    aggregationPipeline.add(additionalGroupBy);
                }


                if(max > 0) {
                    aggregationPipeline.add(new BasicDBObject("$limit", max));
                }
                if(offset > 0) {
                    aggregationPipeline.add(new BasicDBObject("$skip", offset));
                }



                Cursor aggregatedResults = collection.aggregate(aggregationPipeline, AggregationOptions.builder().build());

                if(singleResult && aggregatedResults.hasNext()) {
                    DBObject dbo = aggregatedResults.next();
                    for (ProjectedProperty projectedProperty : projectedKeys) {
                        Object value = dbo.get(projectedProperty.projectionKey);
                        PersistentProperty property = projectedProperty.property;
                        if(value != null) {
                            if(property instanceof ToOne ) {
                                projectedResults.add( session.retrieve(property.getType(), (Serializable) value));
                            }
                            else {
                                projectedResults.add(value);
                            }
                        }
                        else {
                            if(projectedProperty.projection instanceof CountProjection) {
                                projectedResults.add(0);
                            }
                        }
                    }
                }
                else {
                   return new AggregatedResultList(session, aggregatedResults, projectedKeys);
                }

                return projectedResults;
            }

            protected DBCursor executeQuery(final PersistentEntity entity,
                    final Junction criteria, final DBCollection collection, DBObject query) {
                final DBCursor cursor;
                if (criteria.isEmpty()) {
                    cursor = executeQueryAndApplyPagination(collection, query);
                }
                else {
                    populateMongoQuery(entity, query, criteria);
                    cursor = executeQueryAndApplyPagination(collection, query);
                }

                if (queryArguments != null) {
                    if (queryArguments.containsKey(HINT_ARGUMENT)) {
                        Object hint = queryArguments.get(HINT_ARGUMENT);
                        if (hint instanceof Map) {
                            cursor.hint(new BasicDBObject((Map)hint));
                        }
                        else if (hint != null) {
                            cursor.hint(hint.toString());
                        }
                    }
                }
                return cursor;
            }

            protected DBCursor executeQueryAndApplyPagination(final DBCollection collection, DBObject query) {
                final DBCursor cursor;
                cursor = collection.find(query);
                if (offset > 0) {
                    cursor.skip(offset);
                }
                if (max > -1) {
                    cursor.limit(max);
                }

                if (!orderBy.isEmpty()) {
                    DBObject orderObject = new BasicDBObject();
                    for (Order order : orderBy) {
                        String property = order.getProperty();
                        property = getPropertyName(entity, property);
                        orderObject.put(property, order.getDirection() == Order.Direction.DESC ? -1 : 1);
                    }
                    cursor.sort(orderObject);
                }
                else {
                    MongoCollection coll = (MongoCollection) entity.getMapping().getMappedForm();
                    if (coll != null && coll.getSort() != null) {
                        DBObject orderObject = new BasicDBObject();
                        Order order = coll.getSort();
                        String property = order.getProperty();
                        property = getPropertyName(entity, property);
                        orderObject.put(property, order.getDirection() == Order.Direction.DESC ? -1 : 1);
                        cursor.sort(orderObject);
                    }
                }

                return cursor;
            }
        });
    }

    private DBObject createQueryObject(PersistentEntity persistentEntity) {
        DBObject query;
        if (persistentEntity.isRoot()) {
            query = new BasicDBObject();
        }
        else {
            query = new BasicDBObject(MongoEntityPersister.MONGO_CLASS_FIELD, persistentEntity.getDiscriminator());
        }
        return query;
    }

    @SuppressWarnings("unchecked")
    public static void populateMongoQuery(PersistentEntity entity, DBObject query, Junction criteria) {

        List subList = null;
        // if a query combines more than 1 item, wrap the items in individual $and or $or arguments
        // so that property names can't clash (e.g. for an $and containing two $ors)
        if (criteria.getCriteria().size() > 1) {
            if (criteria instanceof Disjunction) {
                subList = new ArrayList();
                query.put(MONGO_OR_OPERATOR, subList);
            } else if (criteria instanceof Conjunction) {
                subList = new ArrayList();
                query.put(MONGO_AND_OPERATOR, subList);
            }
        }
        for (Criterion criterion : criteria.getCriteria()) {
            final QueryHandler queryHandler = queryHandlers.get(criterion.getClass());
            if (queryHandler != null) {
                DBObject dbo = query;
                if (subList != null) {
                    dbo = new BasicDBObject();
                    subList.add(dbo);
                }

                if (criterion instanceof PropertyCriterion && !(criterion instanceof GeoCriterion)) {
                    PropertyCriterion pc = (PropertyCriterion) criterion;
                    PersistentProperty property = entity.getPropertyByName(pc.getProperty());
                    if (property instanceof Custom) {
                        CustomTypeMarshaller customTypeMarshaller = ((Custom) property).getCustomTypeMarshaller();
                        customTypeMarshaller.query(property, pc, query);
                        continue;
                    }
                }
                queryHandler.handle(entity, criterion, dbo);
            }
            else {
                throw new InvalidDataAccessResourceUsageException("Queries of type "+criterion.getClass().getSimpleName()+" are not supported by this implementation");
            }
        }
    }

    protected static String getPropertyName(PersistentEntity entity, PropertyNameCriterion criterion) {
        String propertyName = criterion.getProperty();
        return getPropertyName(entity, propertyName);
    }

    private static String getPropertyName(PersistentEntity entity, String propertyName) {
        if (entity.isIdentityName(propertyName)) {
            propertyName = MongoEntityPersister.MONGO_ID_FIELD;
        }
        else {
            PersistentProperty property = entity.getPropertyByName(propertyName);
            if (property != null) {
                propertyName = MappingUtils.getTargetKey(property);
                if (property instanceof ToOne && !(property instanceof Embedded)) {
                    ToOne association = (ToOne) property;
                    MongoAttribute attr = (MongoAttribute) association.getMapping().getMappedForm();
                    boolean isReference = attr == null || attr.isReference();
                    if (isReference) {
                        propertyName = propertyName + MONGO_ID_REFERENCE_SUFFIX;
                    }
                }
            }
        }

        return propertyName;
    }

    private Object createObjectFromDBObject(DBObject dbObject) {
        // we always use the session cached version where available.
        final Object id = dbObject.get(MongoEntityPersister.MONGO_ID_FIELD);
        Class type = mongoEntityPersister.getPersistentEntity().getJavaClass();
        Object instance = mongoSession.getCachedInstance(type, (Serializable) id);
        if (instance == null) {
            instance = mongoEntityPersister.createObjectFromNativeEntry(
                    mongoEntityPersister.getPersistentEntity(), (Serializable) id, dbObject);
            mongoSession.cacheInstance(type, (Serializable) id, instance);
        }
        // note cached instances may be stale, but user can call 'refresh' to fix that.
        return instance;
    }

    @SuppressWarnings("unchecked")
    private List wrapObjectResultInList(Object object) {
        List result = new ArrayList();
        result.add(object);
        return result;
    }

    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this
     */
    public Query near(String property, List value) {
        add(new Near(property, value));
        return this;
    }

    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this
     */
    public Query near(String property, Point value) {
        add(new Near(property, value));
        return this;
    }


    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this
     */
    public Query near(String property, List value, Distance maxDistance) {
        add(new Near(property, value, maxDistance));
        return this;
    }

    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this
     */
    public Query near(String property, Point value, Distance maxDistance) {
        add(new Near(property, value, maxDistance));
        return this;
    }


    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this
     */
    public Query near(String property, List value, Number maxDistance) {
        add(new Near(property, value, maxDistance));
        return this;
    }

    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this
     */
    public Query near(String property, Point value, Number maxDistance) {
        add(new Near(property, value, maxDistance));
        return this;
    }

    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this
     */
    public Query nearSphere(String property, List value) {
        add(new NearSphere(property, value));
        return this;
    }

    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this
     */
    public Query nearSphere(String property, Point value) {
        add(new NearSphere(property, value));
        return this;
    }


    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this
     */
    public Query nearSphere(String property, List value, Distance maxDistance) {
        add(new NearSphere(property, value, maxDistance));
        return this;
    }

    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this
     */
    public Query nearSphere(String property, Point value, Distance maxDistance) {
        add(new NearSphere(property, value, maxDistance));
        return this;
    }


    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this
     */
    public Query nearSphere(String property, List value, Number maxDistance) {
        add(new NearSphere(property, value, maxDistance));
        return this;
    }

    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this
     */
    public Query nearSphere(String property, Point value, Number maxDistance) {
        add(new NearSphere(property, value, maxDistance));
        return this;
    }
    /**
     * Geospacial query for values within a given box. A box is defined as a multi-dimensional list in the form
     *
     * [[40.73083, -73.99756], [40.741404,  -73.988135]]
     *
     * @param property The property
     * @param value A multi-dimensional list of values
     * @return This query
     */
    public Query withinBox(String property, List value) {
        add(new WithinBox(property, value));
        return this;
    }

    /**
     * Geospacial query for values within the given shape
     *
     * @param property The property
     * @param shape The shape
     * @return The query instance
     */
    public Query geoWithin(String property, Shape shape) {
        add(new GeoWithin(property, shape));
        return this;
    }

    /**
     * Geospacial query for values within the given shape
     *
     * @param property The property
     * @param shape The shape
     * @return The query instance
     */
    public Query geoIntersects(String property, GeoJSON shape) {
        add(new GeoIntersects(property, shape));
        return this;
    }

    /**
     * Geospacial query for values within a given polygon. A polygon is defined as a multi-dimensional list in the form
     *
     * [[0, 0], [3, 6], [6, 0]]
     *
     * @param property The property
     * @param value A multi-dimensional list of values
     * @return This query
     */
    public Query withinPolygon(String property, List value) {
        add(new WithinPolygon(property, value));
        return this;
    }

    /**
     * Geospacial query for values within a given circle. A circle is defined as a multi-dimensial list containing the position of the center and the radius:
     *
     * [[50, 50], 10]
     *
     * @param property The property
     * @param value A multi-dimensional list of values
     * @return This query
     */
    public Query withinCircle(String property, List value) {
        add(new WithinBox(property, value));
        return this;
    }

    /**
     * @param arguments The query arguments
     */
    public void setArguments(Map arguments) {
        this.queryArguments = arguments;
    }

    /**
     * Used for Geospacial querying
     *
     * @author Graeme Rocher
     * @since 1.0
     */
    public static class Near extends GeoCriterion {

        Distance maxDistance = null;

        public Near(String name, Object value) {
            super(name, value);
        }

        public Near(String name, Object value, Distance maxDistance) {
            super(name, value);
            this.maxDistance = maxDistance;
        }

        public Near(String name, Object value, Number maxDistance) {
            super(name, value);
            this.maxDistance = Distance.valueOf(maxDistance.doubleValue());
        }

        public void setMaxDistance(Distance maxDistance) {
            this.maxDistance = maxDistance;
        }
    }


    /**
     * Used for Geospacial querying with the $nearSphere operator
     *
     * @author Graeme Rocher
     * @since 1.0
     */
    public static class NearSphere extends Near {
        public NearSphere(String name, Object value) {
            super(name, value);
        }

        public NearSphere(String name, Object value, Distance maxDistance) {
            super(name, value, maxDistance);
        }

        public NearSphere(String name, Object value, Number maxDistance) {
            super(name, value, maxDistance);
        }
    }

    /**
     * Used for Geospacial querying of boxes
     *
     * @author Graeme Rocher
     * @since 1.0
     */
    public static class WithinBox extends PropertyCriterion {

        public WithinBox(String name, List value) {
            super(name, value);
        }

        public List getValues() {
            return (List) getValue();
        }

        public void setValue(List matrix) {
            this.value = matrix;
        }
    }

    /**
     * Used for Geospacial querying of polygons
     */
    public static class WithinPolygon extends PropertyCriterion {

        public WithinPolygon(String name, List value) {
            super(name, value);
        }

        public List getValues() {
            return (List) getValue();
        }

        public void setValue(List value) {
            this.value = value;
        }
    }

    /**
     * Used for Geospacial querying of circles
     *
     * @author Graeme Rocher
     * @since 1.0
     */
    public static class WithinCircle extends PropertyCriterion {

        public WithinCircle(String name, List value) {
            super(name, value);
        }

        public List getValues() {
            return (List) getValue();
        }

        public void setValue(List matrix) {
            this.value = matrix;
        }
    }

    /**
     * Used for all GeoSpacial queries using 2dsphere indexes
     */
    public static class GeoCriterion extends PropertyCriterion {

        public GeoCriterion(String name, Object value) {
            super(name, value);
        }
    }

    public static class GeoWithin extends GeoCriterion {
        public GeoWithin(String name, Object value) {
            super(name, value);
        }
    }

    public static class GeoIntersects extends GeoCriterion {
        public GeoIntersects(String name, Object value) {
            super(name, value);
        }
    }

    private static interface QueryHandler<T> {
        public void handle(PersistentEntity entity, T criterion, DBObject query);
    }

    private static interface ProjectionHandler<T extends Projection> {
        /**
         * Handles a projection modifying the aggregation pipeline appropriately
         *
         * @param entity The entity
         * @param groupByObject The group by object
         * @param projection The projection
         * @return The key to be used to obtain the projected value from the pipeline results
         */
        public String handle(PersistentEntity entity, DBObject projectObject, DBObject groupByObject, T projection);
    }


    public static class AggregatedResultList extends AbstractList implements Closeable {

        private Cursor cursor;
        private List<ProjectedProperty> projectedProperties;
        private List initializedObjects = new ArrayList();
        private int internalIndex = 0;
        private boolean initialized = false;
        private boolean containsAssociations = false;
        private Session session;

        public AggregatedResultList(Session session, Cursor cursor, List<ProjectedProperty> projectedProperties) {
            this.cursor = cursor;
            this.projectedProperties = projectedProperties;
            this.session = session;
            for (ProjectedProperty projectedProperty : projectedProperties) {
                if(projectedProperty.property instanceof Association) {
                    this.containsAssociations = true;
                    break;
                }
            }
        }

        @Override
        public String toString() {
            return initializedObjects.toString();
        }

        @Override
        public Object get(int index) {
            if(containsAssociations) initializeFully();
            if(initializedObjects.size() > index) {
                return initializedObjects.get(index);
            }
            else if(!initialized) {
                boolean hasResults = false;
                while(cursor.hasNext()) {
                    hasResults = true;
                    DBObject dbo = cursor.next();
                    Object projected = addInitializedObject(dbo);
                    if(index == internalIndex) {
                        return projected;
                    }
                }
                if(!hasResults) handleNoResults();
                initialized = true;
            }
            throw new ArrayIndexOutOfBoundsException("Index value " + index + " exceeds size of aggregate list");
        }


        @Override
        public Object set(int index, Object element) {
            initializeFully();
            return initializedObjects.set(index, element);
        }

        @Override
        public ListIterator listIterator() {
            return listIterator(0);
        }

        @Override
        public ListIterator listIterator(int index) {
            initializeFully();
            return initializedObjects.listIterator(index);
        }

        protected void initializeFully() {
            if(initialized) return;
            if(containsAssociations) {
                if(projectedProperties.size() == 1) {
                    ProjectedProperty projectedProperty = projectedProperties.get(0);
                    PersistentProperty property = projectedProperty.property;
                    List<Serializable> identifiers = new ArrayList<Serializable>();
                    boolean hasResults = false;
                    while(cursor.hasNext()) {
                        hasResults = true;
                        DBObject dbo = cursor.next();
                        Object id = getProjectedValue(dbo, projectedProperty.projectionKey);
                        identifiers.add((Serializable) id);
                    }
                    if(!hasResults) {
                        handleNoResults();
                    }
                    else {
                        this.initializedObjects = session.retrieveAll(property.getType(), identifiers);
                    }
                }
                else {
                    Map<Integer, Map<Class, List<Serializable>>> associationMap = createAssociationMap();

                    boolean hasResults = false;
                    while(cursor.hasNext()) {
                        hasResults = true;
                        DBObject dbo = cursor.next();
                        List<Object> projectedResult = new ArrayList<Object>();
                        int index = 0;
                        for (ProjectedProperty projectedProperty : projectedProperties) {
                            PersistentProperty property = projectedProperty.property;
                            Object value = getProjectedValue(dbo, projectedProperty.projectionKey);
                            if(property instanceof Association) {
                                Map<Class, List<Serializable>> identifierMap = associationMap.get(index);
                                Class type = ((Association) property).getAssociatedEntity().getJavaClass();
                                identifierMap.get(type).add((Serializable) value);
                            }
                            projectedResult.add(value);
                            index++;
                        }

                        initializedObjects.add(projectedResult);
                    }

                    if(!hasResults) {
                        handleNoResults();
                        return;
                    }

                    Map<Integer, List> finalResults = new HashMap<Integer, List>();
                    for (Integer index : associationMap.keySet()) {
                        Map<Class, List<Serializable>> associatedEntityIdentifiers = associationMap.get(index);
                        for (Class associationClass : associatedEntityIdentifiers.keySet()) {
                            List<Serializable> identifiers = associatedEntityIdentifiers.get(associationClass);
                            finalResults.put(index, session.retrieveAll(associationClass, identifiers));
                        }
                    }

                    for (Object initializedObject : initializedObjects) {
                        List projected = (List) initializedObject;
                        for (Integer index : finalResults.keySet()) {
                            List resultsByIndex = finalResults.get(index);
                            if(index < resultsByIndex.size() ) {
                                projected.set(index, resultsByIndex.get(index));
                            }
                            else {
                                projected.set(index, null);
                            }
                        }

                    }
                }
            }
            else {
                boolean hasResults = false;
                while(cursor.hasNext()) {
                    hasResults = true;
                    DBObject dbo = cursor.next();
                    addInitializedObject(dbo);
                }
                if(!hasResults) {
                    handleNoResults();
                }
            }
            initialized = true;
        }

        protected void handleNoResults() {
            ProjectedProperty projectedProperty = projectedProperties.get(0);
            if(projectedProperty.projection instanceof CountProjection) {
                initializedObjects.add(0);
            }
        }

        private Map<Integer, Map<Class, List<Serializable>>> createAssociationMap() {
            Map<Integer, Map<Class, List<Serializable>>> associationMap = new HashMap<Integer, Map<Class, List<Serializable>>>();
            associationMap = DefaultGroovyMethods.withDefault(associationMap, new Closure(this) {
                public Object doCall(Object o) {
                    Map<Class, List<Serializable>> subMap = new HashMap<Class, List<Serializable>>();
                    subMap = DefaultGroovyMethods.withDefault(subMap, new Closure(this) {
                        public Object doCall(Object o) {
                            return new ArrayList<Serializable>();
                        }
                    });
                    return subMap;
                }
            });
            return associationMap;
        }

        @Override
        public Iterator iterator() {
            if(initialized || containsAssociations || internalIndex>0) {
                initializeFully();
                return initializedObjects.iterator();
            }

            if(!cursor.hasNext()) {
                handleNoResults();
                return initializedObjects.iterator();
            }

            return new Iterator() {
                @Override
                public boolean hasNext() {
                    boolean hasMore = cursor.hasNext();
                    if(!hasMore) initialized = true;
                    return hasMore;
                }

                @Override
                public Object next() {
                    DBObject dbo = cursor.next();
                    return addInitializedObject(dbo);
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("Aggregate result list cannot be mutated.");
                }
            };
        }

        private Object addInitializedObject(DBObject dbo) {
            if(projectedProperties.size() > 1) {

                List<Object> projected = new ArrayList<Object>();
                for (ProjectedProperty projectedProperty : projectedProperties) {
                    Object value;
                    value = getProjectedValue(dbo, projectedProperty.projectionKey);
                    projected.add(value);
                }
                initializedObjects.add(internalIndex, projected);
                internalIndex++;
                return projected;
            }
            else {
                ProjectedProperty projectedProperty = projectedProperties.get(0);
                Object projected = getProjectedValue(dbo, projectedProperty.projectionKey);
                initializedObjects.add(internalIndex, projected);
                internalIndex++;
                return projected;
            }
        }

        private Object getProjectedValue(DBObject dbo, String projectionKey) {
            Object value;
            if(projectionKey.startsWith("id.")) {
                projectionKey = projectionKey.substring(3);
                DBObject id = (DBObject) dbo.get(MongoEntityPersister.MONGO_ID_FIELD);
                value = id.get(projectionKey);
            }
            else {
                value = dbo.get(projectionKey);
            }
            return value;
        }

        @Override
        public int size() {
            initializeFully();
            return initializedObjects.size();
        }

        @Override
        public void close() throws IOException {
            cursor.close();
        }
    }

    @SuppressWarnings("serial")
    public static class MongoResultList extends AbstractList implements Closeable {

        private MongoEntityPersister mongoEntityPersister;
        private Cursor cursor;
        private int offset = 0;
        private int internalIndex;
        private List initializedObjects = new ArrayList();
        private Integer size;
        private boolean initialized = false;

        @SuppressWarnings("unchecked")
        public MongoResultList(Cursor cursor, int offset, MongoEntityPersister mongoEntityPersister) {
            this.cursor = cursor;
            this.mongoEntityPersister = mongoEntityPersister;
            this.offset = offset;
        }

        @Override
        public String toString() {
            initializeFully();
            return initializedObjects.toString();
        }

        /**
         * @return The underlying MongoDB cursor instance
         */
        public Cursor getCursor() {
            return cursor;
        }

        @Override
        public boolean isEmpty() {
            return initializedObjects.isEmpty() && !cursor.hasNext();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object get(int index) {
            if(initializedObjects.size() > index) {
                return initializedObjects.get(index);
            }
            else if(!initialized) {
                while(cursor.hasNext()) {
                    if(internalIndex > index) throw new ArrayIndexOutOfBoundsException("Cannot retrieve element at index " + index + " for cursor size " + size());
                    Object o = convertDBObject(cursor.next());
                    initializedObjects.add(internalIndex,o);
                    if(index == internalIndex++) {
                        return o;
                    }
                }
                initialized = true;
            }
            throw new ArrayIndexOutOfBoundsException("Cannot retrieve element at index " + index + " for cursor size " + size());
        }

        @Override
        public Object set(int index, Object o) {
            if(index > (size()-1)) {
                throw new ArrayIndexOutOfBoundsException("Cannot set element at index " + index + " for cursor size " + size());
            }
            else {
                // initialize
                get(index);
                return initializedObjects.set(index, o);
            }
        }

        @Override
        public ListIterator listIterator() {
            return listIterator(0);
        }

        @Override
        public ListIterator listIterator(int index) {
            initializeFully();
            return initializedObjects.listIterator(0);
        }

        private void initializeFully() {
            if(initialized) return;

            while(cursor.hasNext()) {
                DBObject dbo = cursor.next();
                Object current = convertDBObject(dbo);
                initializedObjects.add(current);
            }
            initialized = true;
        }

        /**
         * Override to transform elements if necessary during iteration.
         * @return an iterator over the elements in this list in proper sequence
         */
        @Override
        public Iterator iterator() {
            if(initialized) {
                return initializedObjects.iterator();
            }

            final Cursor cursor;
            if(this.cursor instanceof DBCursor) {
                DBCursor dbCursor = (DBCursor) this.cursor;
                DBCursor newDbCursor = dbCursor.copy();
                cursor = newDbCursor;
                newDbCursor.skip(offset);
            }
            else {
                cursor = this.cursor;
            }


            return new Iterator() {
                Object current;

                public boolean hasNext() {
                    boolean hasMore = cursor.hasNext();
                    if(!hasMore) {
                        initialized = true;
                    }
                    return hasMore;
                }

                @SuppressWarnings("unchecked")
                public Object next() {
                    DBObject dbo = cursor.next();
                    current = convertDBObject(dbo);
                    initializedObjects.add(current);
                    return current;
                }

                public void remove() {
                    initializedObjects.remove(current);
                }
            };
        }

        @Override
        public int size() {
            if(initialized) {
                return initializedObjects.size();
            }
            if(this.size == null) {
                if(cursor instanceof DBCursor) {
                    this.size = ((DBCursor)cursor).size();
                }
                else {

                    this.size = 0;
                    while(cursor.hasNext()) {
                        DBObject object = cursor.next();
                        initializedObjects.add( convertDBObject(object) );
                        this.size++;
                    }
                    initialized = true;
                }
            }
            return size;
        }

        protected Object convertDBObject(Object object) {
            final DBObject dbObject = (DBObject) object;
            Object id = dbObject.get(MongoEntityPersister.MONGO_ID_FIELD);
            SessionImplementor session = (SessionImplementor) mongoEntityPersister.getSession();
            Class type = mongoEntityPersister.getPersistentEntity().getJavaClass();
            Object instance = session.getCachedInstance(type, (Serializable) id);
            if (instance == null) {
                instance = mongoEntityPersister.createObjectFromNativeEntry(
                    mongoEntityPersister.getPersistentEntity(), (Serializable) id, dbObject);
                session.cacheInstance(type, (Serializable) id, instance);
            }
            return instance;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object clone() {
            return new MongoResultList(cursor,offset, mongoEntityPersister);
        }

        @Override
        public void close() throws IOException {
            cursor.close();
        }
    }

    private static class ProjectedProperty {
        Projection projection;
        String projectionKey;
        PersistentProperty property;
    }
}
