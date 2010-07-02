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

import org.springframework.datastore.engine.Indexer;
import org.springframework.datastore.mapping.types.Association;
import org.jredis.JRedis;
import org.jredis.RedisException;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An indexer for Redis
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class RedisAssociationIndexer implements Indexer<Long, Long> {
    private JRedis jredis;
    private SimpleTypeConverter typeConverter;
    private Association association;

    public RedisAssociationIndexer(JRedis jredis, SimpleTypeConverter typeConverter, Association association) {
        this.jredis = jredis;
        this.typeConverter = typeConverter;
        this.association = association;
    }

    public void index(Long primaryKey, List<Long> foreignKeys) {
        String redisKey = createRedisKey(primaryKey);
        List storedKeys = queryInternal(redisKey);
        for (Long foreignKey : foreignKeys) {
            if(!storedKeys.contains(foreignKey)) {
                try {
                    jredis.sadd(redisKey, foreignKey);
                } catch (RedisException e) {
                    throw new DataAccessResourceFailureException("Exception occurred executing Redis command SADD: " + e.getMessage(),e);
                }
            }
        }
    }

    private String createRedisKey(Long primaryKey) {
        return association.getOwner().getName()+ ":" + primaryKey + ":" + association.getName();
    }

    public List<Long> query(Long primaryKey) {
        String redisKey = createRedisKey(primaryKey);
        return queryInternal(redisKey);
    }

    private List<Long> queryInternal(String redisKey) {
        try {
            final List<byte[]> results = jredis.smembers(redisKey);
            if(!results.isEmpty()) {
                List<Long> foreignKeys = new ArrayList<Long>(results.size());
                for (byte[] result : results) {
                    foreignKeys.add(getLong(result));
                }
                return foreignKeys;
            }
            else {
                return Collections.emptyList();
            }
        } catch (RedisException e) {
            throw new DataAccessResourceFailureException("Exception occurred executing Redis command SMEMBERS: " + e.getMessage(),e);
        }
    }

    private Long getLong(Object key) {
        return typeConverter.convertIfNecessary(key, Long.class);
    }

}
