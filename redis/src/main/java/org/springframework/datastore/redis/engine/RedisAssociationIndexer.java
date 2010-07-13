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
package org.springframework.datastore.redis.engine;

import org.jredis.JRedis;
import org.jredis.RedisException;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.datastore.engine.AssociationIndexer;
import org.springframework.datastore.mapping.types.Association;
import org.springframework.datastore.redis.query.RedisQueryUtils;
import org.springframework.datastore.redis.util.RedisCallback;
import org.springframework.datastore.redis.util.RedisTemplate;

import java.util.List;

/**
 * An indexer for Redis
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class RedisAssociationIndexer implements AssociationIndexer<Long, Long> {
    private RedisTemplate template;
    private SimpleTypeConverter typeConverter;
    private Association association;

    public RedisAssociationIndexer(JRedis jredis, SimpleTypeConverter typeConverter, Association association) {
        this.template = new RedisTemplate(jredis);
        this.typeConverter = typeConverter;
        this.association = association;
    }

    public void index(Long primaryKey, final List<Long> foreignKeys) {
        final String redisKey = createRedisKey(primaryKey);
        final List storedKeys = queryInternal(redisKey);
        template.execute(new RedisCallback(){
            public Object doInRedis(JRedis jredis) throws RedisException {
                for (Long foreignKey : foreignKeys) {
                    if(!storedKeys.contains(foreignKey)) {
                            jredis.sadd(redisKey, foreignKey);
                    }
                }
                return null;
            }
        });
    }

    private String createRedisKey(Long primaryKey) {
        return association.getOwner().getName()+ ":" + primaryKey + ":" + association.getName();
    }

    public List<Long> query(Long primaryKey) {
        String redisKey = createRedisKey(primaryKey);
        return queryInternal(redisKey);
    }

    private List<Long> queryInternal(final String redisKey) {
        return (List<Long>) template.execute(new RedisCallback() {

            public Object doInRedis(JRedis jredis) throws RedisException {
                final List<byte[]> results = jredis.smembers(redisKey);
                return RedisQueryUtils.transformRedisResults(typeConverter, results);
            }
        });
    }

}
