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

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.datastore.engine.PropertyValueIndexer;
import org.springframework.datastore.keyvalue.mapping.KeyValue;
import org.springframework.datastore.mapping.PersistentEntity;
import org.springframework.datastore.mapping.PersistentProperty;
import org.springframework.datastore.query.Projections;
import org.springframework.datastore.query.Query;
import org.springframework.datastore.query.Restrictions;
import org.springframework.datastore.redis.RedisSession;
import org.springframework.datastore.redis.collection.RedisCollection;
import org.springframework.datastore.redis.engine.RedisEntityPersister;
import org.springframework.datastore.redis.engine.RedisPropertyValueIndexer;
import org.springframework.datastore.redis.util.RedisCallback;
import org.springframework.datastore.redis.util.RedisTemplate;
import sma.RedisClient;

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

    public RedisQuery(RedisSession session, PersistentEntity persistentEntity, RedisEntityPersister entityPersister) {
        super(session, persistentEntity);
        this.entityPersister = entityPersister;
        template = new RedisTemplate(session.getNativeInterface());
    }

    @Override
    protected List executeQuery(PersistentEntity entity, Junction criteria) {
        List<Long> identifiers = null;
        final boolean hasCountProjection = projections().getProjectionList().contains(Projections.count());
        final boolean shouldPaginate = offset > 0 || max > -1;
        if(criteria.isEmpty()) {

            if(hasCountProjection) {
                final String redisKey = entityPersister.getAllEntityIndex().getRedisKey();
                return getSetCountResult(redisKey);
            }
            else {
                String[] results;
                RedisCollection col = entityPersister.getAllEntityIndex();
                if(shouldPaginate) {
                    results = col.members(offset, max);
                }
                else {
                    results = col.members();
                }

                identifiers = RedisQueryUtils.transformRedisResults(entityPersister.getTypeConverter(), results);
            }
        }
        else {
            List<Criterion> criteriaList = criteria.getCriteria();
            String finalKey = executeSubQuery(criteria, criteriaList);
            String[] results;
            if(hasCountProjection) {
                return getSetCountResult(finalKey);
            }
            else if(shouldPaginate) {
                results = paginateResults(finalKey);
            }
            else {
                results = template.smembers(finalKey);
            }

            identifiers = RedisQueryUtils.transformRedisResults(entityPersister.getTypeConverter(), results);
        }
        if(identifiers != null) {
            if(projections().getProjectionList().contains(Projections.ID_PROJECTION)) {
                return identifiers;
            }
            else {
                return new RedisEntityResultList(getSession(), getEntity(), identifiers);
            }
        }
        else {
            return Collections.emptyList();
        }
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

    private String[] paginateResults(final String disjKey) {
        return (String[]) template.execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                return redis.sort(disjKey, RedisClient.SortParam.limit(offset, max));
            }
        });
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

    private List getSetCountResult(String redisKey) {
        final long count = template.scard(redisKey);
        List result = new ArrayList();
        result.add(count);
        return result;
    }

    private boolean isIndexed(PersistentProperty property) {
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
//       put(GreaterThan.class,  new CriterionHandler<GreaterThan>() {
//           public void handle(RedisEntityPersister entityPersister, List<String> indices, GreaterThan criterion) {
////               String key = executeGreaterThan(entityPersister, criterion);
////               indices.add(key);
//           }
//       });
       put(GreaterThanEquals.class,  new CriterionHandler<GreaterThanEquals>() {
           public void handle(RedisEntityPersister entityPersister, List<String> indices, GreaterThanEquals criterion) {
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
       put(Junction.class,  new CriterionHandler<Junction>() {
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


    protected String executeLessThanEquals(RedisEntityPersister entityPersister, LessThanEquals criterion) {

        final String property = criterion.getProperty();
        PersistentProperty prop = getAndValidateProperty(entityPersister, property);

        return executeBetweenInternal(entityPersister, prop, 0, criterion.getValue(), false, true);
    }

    protected String executeGreaterThanEquals(RedisEntityPersister entityPersister, GreaterThanEquals criterion) {
        final String property = criterion.getProperty();
        PersistentProperty prop = getAndValidateProperty(entityPersister, property);

        String sortKey = entityPersister.getPropertySortKey(prop);
        String maxKey = sortKey + "~max-score";

        Object max = template.get(maxKey);
        if(max == null) {
            String[] results = template.zrevrange(sortKey, 0, 0);
            if(results.length > 0) {
                max = template.zscore(sortKey, results[0]);
            }
            else max = -1;

            template.setex(maxKey, max, 500);
        }

        return executeBetweenInternal(entityPersister, prop, criterion.getValue(), max, false, true);
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
        if(!(fromObject instanceof Number)) {
            fromObject = entityPersister.getTypeConverter().convertIfNecessary(fromObject, Double.class);
        }
        if(!(toObject instanceof Number)) {
            toObject = entityPersister.getTypeConverter().convertIfNecessary(toObject, Double.class);
        }

        final double from = ((Number) fromObject).doubleValue();
        final double to = ((Number) toObject).doubleValue();

        final String key = sortKey + "~between-" + from + "-" + from;
        if(!template.exists(key)) {
            String[] results;
            results = template.zrangebyscore(sortKey, from, to);
            template.multi();
            for (String result : results) {
                template.sadd(key, result);
            }
            template.expire(key, 500);
            template.exec();
        }
        return key;
    }

    private PersistentProperty getAndValidateProperty(RedisEntityPersister entityPersister, String property) {
        final PersistentEntity entity = entityPersister.getPersistentEntity();
        PersistentProperty prop = entity.getPropertyByName(property);
        if(prop == null) {
            throw new DataIntegrityViolationException("Cannot execute between query on property ["+property+"] of class ["+entity+"]. Property does not exist.");
        }
        return prop;
    }

    private String executeSubLike(RedisEntityPersister entityPersister, Like like) {
        final String property = like.getProperty();
        String pattern = like.getPattern();
        final String[] keys = resolveMatchingIndices(entityPersister, property, pattern);
        final String disjKey = formulateDisjunctionKey(keys);
        template.sunionstore(disjKey, keys);
        template.expire(disjKey, 300);
        return disjKey;
    }

    private String[] resolveMatchingIndices(RedisEntityPersister entityPersister, String property, String pattern) {
        PersistentProperty prop = getEntity().getPropertyByName(property);
        assertIndexed(property, prop);
        RedisPropertyValueIndexer indexer = (RedisPropertyValueIndexer) entityPersister.getPropertyIndexer(prop);
        return template.keys(indexer.getIndexPattern(pattern));        
    }

    private String getIndexName(RedisEntityPersister entityPersister, String property, Object value) {
        PersistentProperty prop = getEntity().getPropertyByName(property);
        assertIndexed(property, prop);

        PropertyValueIndexer indexer = entityPersister.getPropertyIndexer(prop);
        return indexer.getIndexName(value);
    }

    private void assertIndexed(String property, PersistentProperty prop) {
        if(prop == null) {
            throw new DataIntegrityViolationException("Cannot execute query. Entity ["+getEntity()+"] does not declare a property named ["+ property +"]");
        }
        else if(!isIndexed(prop)) {
            throw new DataIntegrityViolationException("Cannot query class ["+getEntity()+"] on property ["+prop+"]. The property is not indexed!");
        }
    }
}
