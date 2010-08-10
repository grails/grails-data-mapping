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
import org.jredis.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.datastore.engine.PropertyValueIndexer;
import org.springframework.datastore.keyvalue.mapping.KeyValue;
import org.springframework.datastore.mapping.PersistentEntity;
import org.springframework.datastore.mapping.PersistentProperty;
import org.springframework.datastore.query.Query;
import org.springframework.datastore.redis.engine.RedisEntityPersister;
import org.springframework.datastore.redis.util.RedisCallback;
import org.springframework.datastore.redis.util.RedisTemplate;

import java.util.Collections;
import java.util.List;

/**
 * A Query implementation for Redis
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class RedisQuery extends Query {
    private RedisTemplate redisTemplate;
    private RedisEntityPersister entityPersister;

    public RedisQuery(PersistentEntity persistentEntity, RedisEntityPersister entityPersister) {
        super(persistentEntity);
        this.redisTemplate = entityPersister.getRedisTemplate();
        this.entityPersister = entityPersister;
    }

    @Override
    protected List executeQuery(PersistentEntity entity, List<Criterion> criteria) {
        if(criteria == null || criteria.isEmpty()) {
            return (List) redisTemplate.execute(new RedisCallback() {
                public Object doInRedis(JRedis jredis) throws RedisException {
                    List<byte[]> results;
                    if(offset > 0 || max > -1) {
                        Sort sort = jredis.sort(entityPersister.getAllEntityIndex()).LIMIT(offset, max);
                        results = sort.exec();
                    }
                    else {
                        results = jredis.smembers(entityPersister.getAllEntityIndex());
                    }

                    final List<Long> identifiers = RedisQueryUtils.transformRedisResults(entityPersister.getTypeConverter(), results);

                    return entityPersister.retrieveAll((Iterable)identifiers);
                }
            });
        }
        else {
            if(criteria.size() == 1) {
                Criterion c = criteria.get(0);
                if(c instanceof Equals) {
                    Equals eq = (Equals) c;
                    PersistentProperty property = getEntity().getPropertyByName(eq.getName());
                    KeyValue kv = (KeyValue) property.getMapping().getMappedForm();
                    if(kv.isIndexed()) {
                        PropertyValueIndexer indexer = entityPersister.getPropertyIndexer(property);
                        List identifiers = indexer.query(eq.getValue(), offset, max);
                    }
                    else {
                        throw new DataIntegrityViolationException("Cannot query on class ["+getEntity()+"] on property ["+property+"]. The property is not indexed!");
                    }
                }
            }
        }
        return Collections.emptyList();
    }
}
