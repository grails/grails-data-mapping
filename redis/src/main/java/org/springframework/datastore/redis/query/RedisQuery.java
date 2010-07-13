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
import org.springframework.datastore.mapping.PersistentEntity;
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
                    final List<byte[]> results = jredis.lrange(entityPersister.getAllEntityIndex(), offset, max);
                    return RedisQueryUtils.transformRedisResults(entityPersister.getTypeConverter(), results);
                }
            });
        }
        return Collections.emptyList();
    }
}
