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

import org.jredis.JRedis;
import org.jredis.RedisException;
import org.springframework.dao.DataIntegrityViolationException;
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
import org.springframework.datastore.redis.util.RedisCallback;
import org.springframework.datastore.redis.util.RedisTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
                List<byte[]> results;
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
            List<byte[]> results;
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

    private List<byte[]> paginateResults(final String disjKey) {
        return (List<byte[]>) template.execute(new RedisCallback() {
            public Object doInRedis(JRedis jredis) throws RedisException {
                return jredis.sort(disjKey).LIMIT(offset, max).exec();
            }
        });
    }

    private String formulateDisjunctionKey(List<String> indices) {
        return "~!" + indices.toString().replaceAll("\\s", "-");
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

    private List<String> getIndexNames(Junction criteria, RedisEntityPersister entityPersister) {

        List<Criterion> criteriaList = criteria.getCriteria();
        List<String> indices = new ArrayList<String>();
        for (Criterion criterion : criteriaList) {
            if(criterion instanceof Junction) {
                final Junction junc = (Junction) criterion;
                indices.add( executeSubQuery(junc, junc.getCriteria()) );
            }
            else if(criterion instanceof Equals) {
                Equals eq = (Equals) criterion;
                final String property = eq.getName();
                final Object value = eq.getValue();
                final String indexName = getIndexName(entityPersister, property, value);
                indices.add(indexName);
            }
            else if(criterion instanceof In) {
                final In in = (In)criterion;
                final String property = in.getName();
                Disjunction dis = new Disjunction();
                for (Object value : in.getValues()) {
                    dis.add(Restrictions.eq(property, value));                    
                }
                indices.add( executeSubQuery(dis, dis.getCriteria()) );

            }
        }
        return indices;
    }

    private String getIndexName(RedisEntityPersister entityPersister, String property, Object value) {
        PersistentProperty prop = getEntity().getPropertyByName(property);
        if(prop == null) {
            throw new DataIntegrityViolationException("Cannot execute query. Entity ["+getEntity()+"] does not declare a property named ["+ property +"]");
        }
        else if(!isIndexed(prop)) {
            throw new DataIntegrityViolationException("Cannot query class ["+getEntity()+"] on property ["+prop+"]. The property is not indexed!");
        }

        PropertyValueIndexer indexer = entityPersister.getPropertyIndexer(prop);
        return indexer.getIndexName(value);
    }
}
