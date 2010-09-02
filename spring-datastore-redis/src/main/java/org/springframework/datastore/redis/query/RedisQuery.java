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
package org.springframework.datastore.redis.query;

import org.springframework.core.convert.ConversionService;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.datastore.engine.PropertyValueIndexer;
import org.springframework.datastore.keyvalue.mapping.KeyValue;
import org.springframework.datastore.mapping.PersistentEntity;
import org.springframework.datastore.mapping.PersistentProperty;
import org.springframework.datastore.mapping.types.Identity;
import org.springframework.datastore.query.Query;
import org.springframework.datastore.query.Restrictions;
import org.springframework.datastore.redis.RedisSession;
import org.springframework.datastore.redis.engine.RedisEntityPersister;
import org.springframework.datastore.redis.engine.RedisPropertyValueIndexer;
import org.springframework.datastore.redis.util.RedisCallback;
import org.springframework.datastore.redis.util.RedisTemplate;
import org.springframework.datastore.redis.util.SortParams;

import java.io.IOException;
import java.util.*;

/**
 * A Query implementation for Redis
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class RedisQuery extends Query {
    private RedisEntityPersister entityPersister;
    private RedisTemplate template;
    private ConversionService conversionService;

    public RedisQuery(RedisSession session, RedisTemplate redisTemplate, PersistentEntity persistentEntity, RedisEntityPersister entityPersister) {
        super(session, persistentEntity);
        this.entityPersister = entityPersister;
        template = redisTemplate;
        conversionService = getSession().getMappingContext().getConversionService();
    }

    @Override
    protected List executeQuery(PersistentEntity entity, Junction criteria) {
        final ProjectionList projectionList = projections();
        String finalKey;
        if(criteria.isEmpty())  {
            finalKey = entityPersister.getAllEntityIndex().getRedisKey();
        }
        else {
            List<Criterion> criteriaList = criteria.getCriteria();
            finalKey = executeSubQuery(criteria, criteriaList);
        }

        if(!getEntity().isRoot()) {
            // if the entity is not a root entity then apply a conjunction to trim
            // any enities not of this type


            final String childEntityResultsKey = finalKey + "-" + getEntity().getDecapitalizedName();
            template.sinterstore(childEntityResultsKey,
                                 finalKey,
                                 entityPersister.getAllEntityIndex().getRedisKey());
            finalKey = childEntityResultsKey;            
        }




        Collection<String> results;
        IdProjection idProjection = null;
        if(projectionList.isEmpty()) {
            results = paginateResults(finalKey);
        }
        else {
            List projectionResults = new ArrayList();
            String postSortAndPaginationKey = null;
            for (Projection projection : projectionList.getProjectionList()) {
                if(projection instanceof CountProjection) {
                    if(postSortAndPaginationKey == null) postSortAndPaginationKey = storeSortedKey(finalKey);
                    projectionResults.add(getCountResult(postSortAndPaginationKey));
                }
                else if(projection instanceof MaxProjection) {
                    MaxProjection max = (MaxProjection) projection;
                    final String sortKey = getValidSortKey(max);
                    if(!shouldSortOrPaginate()) {
                        projectionResults.add(getMaxValueFromSortedSet(sortKey));
                    }
                }
                else if(projection instanceof MinProjection) {
                    MinProjection min = (MinProjection) projection;
                    final String sortKey = getValidSortKey(min);
                    if(!shouldSortOrPaginate()) {
                        projectionResults.add(getMinValueFromSortedSet(sortKey));
                    }
                }
                else {
                    final String projectionType = projection.getClass().getSimpleName();
                    if(projection instanceof SumProjection) {
                        return unsupportedProjection(projectionType);
                    }
                    else if(projection instanceof AvgProjection) {
                        return unsupportedProjection(projectionType);
                    }
                    else if(projection instanceof PropertyProjection) {
                        PropertyProjection propertyProjection = (PropertyProjection) projection;
                        final PersistentProperty validProperty = getValidProperty(propertyProjection);
                        if(postSortAndPaginationKey == null) postSortAndPaginationKey = storeSortedKey(finalKey);

                        String entityKey = entityPersister.getEntityBaseKey();
                        final List<String> values = template.sort(postSortAndPaginationKey, template.sortParams().get(entityKey + ":*->" + validProperty.getName()));
                        List resultList = new ArrayList();
                        Class type = validProperty.getType();
                        final PersistentEntity associatedEntity = getSession().getMappingContext().getPersistentEntity(type.getName());
                        final boolean isEntityType = associatedEntity != null;
                        if(isEntityType) {
                             return getSession().retrieveAll(type, values);
                        }
                        else {
                            for (String value : values) {
                                resultList.add(conversionService.convert(value, type));
                            }

                            return resultList;
                        }

                    }
                    else if(projection instanceof IdProjection) {
                        idProjection = (IdProjection) projection;
                    }
                }
            }

            if(!projectionResults.isEmpty()) return projectionResults;
            else {
                results = paginateResults(finalKey);
            }
        }



        if(results != null) {
            if(idProjection != null) {
                return RedisQueryUtils.transformRedisResults(conversionService, results);
            }
            else {
                return getSession().retrieveAll(getEntity().getJavaClass(), results );
            }
        }
        else {
            return Collections.emptyList();
        }
    }

    private List unsupportedProjection(String projectionType) {
        throw new InvalidDataAccessResourceUsageException("Cannot use ["+ projectionType +"] projection. ["+projectionType+"] projections are not currently supported." );
    }

    private String getValidSortKey(PropertyProjection projection) {
        PersistentProperty prop = getValidProperty(projection);
        return entityPersister.getPropertySortKey(prop);
    }

    private PersistentProperty getValidProperty(PropertyProjection projection) {
        final String propName = projection.getPropertyName();
        PersistentProperty prop = entityPersister.getPersistentEntity().getPropertyByName(propName);
        if(prop == null) {
            throw new InvalidDataAccessResourceUsageException("Cannot use ["+projection.getClass().getSimpleName()+"] projection on non-existent property: " + propName);
        }
        else if(!isIndexed(prop)) {
            throw new InvalidDataAccessResourceUsageException("Cannot use ["+projection.getClass().getSimpleName()+"] projection on non-indexed property: " + propName);
        }
        return prop;
    }

    private String storeSortedKey(String finalKey) {
        if(shouldSortOrPaginate()) {
           StringBuilder builder = new StringBuilder();
            builder.append('~')
                   .append(finalKey)
                   .append('-')
                   .append(offset)
                   .append('-')
                   .append(max)
                   .append('-');

           if(!orderBy.isEmpty()) {
               final Order order = orderBy.get(0);
               builder.append(order.getProperty())
                      .append('-')
                      .append(order.getDirection());
           }

           String sortKey = builder.toString();
           template.sortstore(finalKey, sortKey, getSortAndPaginationParams());
           return sortKey;
        }
        return finalKey;
    }

    private String executeSubQuery(Junction junction, List<Criterion> criteria) {
        List<String> indices = getIndexNames(junction, entityPersister);

        if(indices.isEmpty()) {
            throw new DataRetrievalFailureException("Unsupported Redis query");
        }
        final String[] keyArray = indices.toArray(new String[indices.size()]);
        String finalKey;
        if(junction instanceof Conjunction) {
            finalKey = formulateConjunctionKey(indices);
            template.sinterstore(finalKey, keyArray);
        }
        else {
            finalKey = formulateDisjunctionKey(indices);
            template.sunionstore(finalKey, keyArray);
        }

        // since the keys used for queries are temporary we set Redis to kill them after a while
        template.expire(finalKey, 300);
        return finalKey;
    }

    private Collection<String> paginateResults(final String key) {
        final boolean shouldSort = shouldSortOrPaginate();

        if(shouldSort)
            return template.sort(key, getSortAndPaginationParams());
        else
            return template.smembers(key);
    }

    private boolean shouldSortOrPaginate() {
        return offset > 0 || max > -1 || !orderBy.isEmpty();
    }

    private SortParams getSortAndPaginationParams() {
        SortParams params = template.sortParams();
        if(!orderBy.isEmpty()) {
            Order o = orderBy.get(0); // Redis doesn't really allow multiple orderings
            String orderBy = entityPersister.getEntityBaseKey() + ":*->" + o.getProperty();


            params.by(orderBy);
            if(o.getDirection() == Order.Direction.DESC) {
               params.desc();
            }
            else {
               params.asc();
            }


        }
        if(offset > 0 || max > -1) {
            params.limit(offset, max);

        }
        return params;
    }

    private String formulateDisjunctionKey(String[] indices) {
        final List<String> indicesList = Arrays.asList(indices);
        return formulateDisjunctionKey(indicesList);
    }

    private String formulateDisjunctionKey(List<String> indicesList) {
        return "~!" + indicesList.toString().replaceAll("\\s", "-");
    }

    private String formulateConjunctionKey(List<String> indices) {
        return "~" + indices.toString().replaceAll("\\s", "-");
    }

    private long getCountResult(String redisKey) {
        if(shouldSortOrPaginate()) {
            return template.llen(redisKey);
        }
        else {
            return  template.scard(redisKey);
        }
    }

    private boolean isIndexed(PersistentProperty property) {
        if(property instanceof Identity) return true;
        KeyValue kv = (KeyValue) property.getMapping().getMappedForm();
        return kv.isIndex();
    }


    private final Map<Class, CriterionHandler> criterionHandlers = new HashMap() {{
       put(Like.class,  new CriterionHandler<Like>() {
           public void handle(RedisEntityPersister entityPersister, List<String> indices, Like criterion) {
               String key = executeSubLike(entityPersister, criterion);
               indices.add(key);
           }
       });
       put(Between.class,  new CriterionHandler<Between>() {
           public void handle(RedisEntityPersister entityPersister, List<String> indices, Between criterion) {
               String key = executeSubBetween(entityPersister, criterion);
               indices.add(key);
           }
       });
       put(GreaterThanEquals.class,  new CriterionHandler<GreaterThanEquals>() {
           public void handle(RedisEntityPersister entityPersister, List<String> indices, GreaterThanEquals criterion) {
               String key = executeGreaterThanEquals(entityPersister, criterion);
               indices.add(key);
           }
       });
       put(GreaterThan.class,  new CriterionHandler<GreaterThan>() {
           public void handle(RedisEntityPersister entityPersister, List<String> indices, GreaterThan criterion) {
               String key = executeGreaterThanEquals(entityPersister, criterion);
               indices.add(key);
           }
       });
       put(LessThanEquals.class,  new CriterionHandler<LessThanEquals>() {
           public void handle(RedisEntityPersister entityPersister, List<String> indices, LessThanEquals criterion) {
               String key = executeLessThanEquals(entityPersister, criterion);
               indices.add(key);
           }
       });
       put(LessThan.class,  new CriterionHandler<LessThan>() {
           public void handle(RedisEntityPersister entityPersister, List<String> indices, LessThan criterion) {
               String key = executeLessThanEquals(entityPersister, criterion);
               indices.add(key);
           }
       });
       put(Equals.class,  new CriterionHandler<Equals>() {
           public void handle(RedisEntityPersister entityPersister, List<String> indices, Equals criterion) {
               final String property = criterion.getProperty();
               final Object value = criterion.getValue();
               final String indexName = getIndexName(entityPersister, property, value);
               indices.add(indexName);
           }
       });
       put(In.class,  new CriterionHandler<In>() {
           public void handle(RedisEntityPersister entityPersister, List<String> indices, In criterion) {
               final String property = criterion.getName();
               Disjunction dis = new Disjunction();
               for (Object value : criterion.getValues()) {
                   dis.add(Restrictions.eq(property, value));
               }
               indices.add( executeSubQuery(dis, dis.getCriteria()) );

           }
       });
       put(Conjunction.class,  new CriterionHandler<Junction>() {
           public void handle(RedisEntityPersister entityPersister, List<String> indices, Junction criterion) {
               indices.add( executeSubQuery(criterion, criterion.getCriteria()) );
           }
       });
       put(Disjunction.class,  new CriterionHandler<Junction>() {
           public void handle(RedisEntityPersister entityPersister, List<String> indices, Junction criterion) {
               indices.add( executeSubQuery(criterion, criterion.getCriteria()) );
           }
       });
    }};



    private static interface CriterionHandler<T> {
        void handle(RedisEntityPersister entityPersister, List<String> indices, T criterion);
    }

    private List<String> getIndexNames(Junction criteria, RedisEntityPersister entityPersister) {

        List<Criterion> criteriaList = criteria.getCriteria();
        List<String> indices = new ArrayList<String>();
        for (Criterion criterion : criteriaList) {
            CriterionHandler handler = criterionHandlers.get(criterion.getClass());
            if(handler != null) {
                handler.handle(entityPersister, indices, criterion);
            }

        }
        return indices;
    }


    protected String executeLessThanEquals(RedisEntityPersister entityPersister, PropertyCriterion criterion) {

        final String property = criterion.getProperty();
        PersistentProperty prop = getAndValidateProperty(entityPersister, property);

        return executeBetweenInternal(entityPersister, prop, 0, criterion.getValue(), false, true);
    }

    protected String executeGreaterThanEquals(RedisEntityPersister entityPersister, PropertyCriterion criterion) {
        final String property = criterion.getProperty();
        PersistentProperty prop = getAndValidateProperty(entityPersister, property);

        String sortKey = entityPersister.getPropertySortKey(prop);
        Object max = getMaxValueFromSortedSet(sortKey);

        return executeBetweenInternal(entityPersister, prop, criterion.getValue(), max, false, true);
    }

    private Object getMaxValueFromSortedSet(String sortKey) {
        String maxKey = sortKey + "~max-score";

        Object max = template.get(maxKey);
        if(max == null) {
            Set<String> results = template.zrevrange(sortKey, 0, 0);
            if(!results.isEmpty()) {
                max = template.zscore(sortKey, results.iterator().next());
            }
            else max = -1;

            template.setex(maxKey, max, 500);
        }

        return conversionService.convert(max, Double.class);
    }

    private Object getMinValueFromSortedSet(String sortKey) {
        String minKey = sortKey + "~min-score";

        Object min = template.get(minKey);
        if(min == null) {
            Set<String> results = template.zrange(sortKey, 0, 0);
            if(!results.isEmpty()) {
                min = template.zscore(sortKey, results.iterator().next());
            }
            else min = -1;

            template.setex(minKey, min, 500);
        }
        return conversionService.convert(min, Double.class);
    }

    protected String executeSubBetween(RedisEntityPersister entityPersister, Between between) {
        final String property = between.getProperty();

        PersistentProperty prop = getAndValidateProperty(entityPersister, property);


        Object fromObject = between.getFrom();
        Object toObject = between.getTo();


        return executeBetweenInternal(entityPersister, prop, fromObject, toObject, false, true);
    }

    private String executeBetweenInternal(RedisEntityPersister entityPersister, PersistentProperty prop, Object fromObject, Object toObject, boolean includeFrom, boolean includeTo) {
        String sortKey = entityPersister.getPropertySortKey(prop);
        if(fromObject instanceof Date) {
            fromObject = ((Date)fromObject).getTime();
        }
        if(toObject instanceof Date) {
            toObject = ((Date)toObject).getTime();
        }

        if(!(fromObject instanceof Number)) {
            fromObject = conversionService.convert(fromObject, Double.class);
        }
        if(!(toObject instanceof Number)) {
            toObject = conversionService.convert(toObject, Double.class);
        }

        final double from = ((Number) fromObject).doubleValue();
        final double to = ((Number) toObject).doubleValue();

        final String key = sortKey + "~between-" + from + "-" + from;
        if(!template.exists(key)) {
            final Set<String> results = template.zrangebyscore(sortKey, from, to);
            if(results != null && !results.isEmpty()) {
                template.pipeline(new RedisCallback<RedisTemplate>() {
                    public Object doInRedis(RedisTemplate redis) throws IOException {
                        for (String result : results) {
                            redis.sadd(key, result);
                        }
                        redis.expire(key, 500);

                        return null;
                    }
                });
            }
        }
        return key;
    }

    private PersistentProperty getAndValidateProperty(RedisEntityPersister entityPersister, String property) {
        final PersistentEntity entity = entityPersister.getPersistentEntity();
        PersistentProperty prop = entity.getPropertyByName(property);
        if(prop == null) {
            throw new InvalidDataAccessResourceUsageException("Cannot execute between query on property ["+property+"] of class ["+entity+"]. Property does not exist.");
        }
        return prop;
    }

    private String executeSubLike(RedisEntityPersister entityPersister, Like like) {
        final String property = like.getProperty();
        String pattern = like.getPattern();
        final List<String> keys = resolveMatchingIndices(entityPersister, property, pattern);
        final String disjKey = formulateDisjunctionKey(keys);
        template.sunionstore(disjKey, keys.toArray(new String[keys.size()]));
        template.expire(disjKey, 300);
        return disjKey;
    }

    private List<String> resolveMatchingIndices(RedisEntityPersister entityPersister, String property, String pattern) {
        PersistentProperty prop = getEntity().getPropertyByName(property);
        assertIndexed(property, prop);
        RedisPropertyValueIndexer indexer = (RedisPropertyValueIndexer) entityPersister.getPropertyIndexer(prop);
        return template.keys(indexer.getIndexPattern(pattern));        
    }

    private String getIndexName(RedisEntityPersister entityPersister, String property, Object value) {
        PersistentProperty prop = getEntity().getPropertyByName(property);
        if(prop == null) {
            final PersistentProperty identity = getEntity().getIdentity();
            if(identity.getName().equals(property)) {
                prop = identity;
            }
        }
        assertIndexed(property, prop);

        PropertyValueIndexer indexer = entityPersister.getPropertyIndexer(prop);
        return indexer.getIndexName(value);
    }

    private void assertIndexed(String property, PersistentProperty prop) {
        if(prop == null) {
            throw new InvalidDataAccessResourceUsageException("Cannot execute query. Entity ["+getEntity()+"] does not declare a property named ["+ property +"]");
        }
        else if(!isIndexed(prop)) {
            throw new InvalidDataAccessResourceUsageException("Cannot query class ["+getEntity()+"] on property ["+prop+"]. The property is not indexed!");
        }
    }
}
