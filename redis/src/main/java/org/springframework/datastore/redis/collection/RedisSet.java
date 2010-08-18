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
package org.springframework.datastore.redis.collection;

import org.springframework.datastore.redis.util.RedisCallback;
import org.springframework.datastore.redis.util.RedisTemplate;
import sma.RedisClient;

import java.util.Iterator;
import java.util.Set;

/**
 * A Java set implementation that backs onto a Redis set
 */
public class RedisSet extends AbstractRedisCollection implements Set {

    public RedisSet(RedisTemplate redisTemplate, String redisKey) {
        super(redisTemplate, redisKey);
    }

    public int size() {
        return (int) redisTemplate.scard(redisKey);
    }

    public boolean contains(Object o) {
        return redisTemplate.sismember(redisKey, o);
    }

    public Iterator iterator() {
        return new RedisIterator(redisTemplate.smembers(redisKey), this);
    }

    public boolean add(Object o) {
        return redisTemplate.sadd(redisKey, o);
    }

    public boolean remove(Object o) {
        return redisTemplate.srem(redisKey, o);
    }


    public String[] members() {
        return redisTemplate.smembers(redisKey);  
    }

    public String[] members(final int offset, final int max) {
        return (String[]) redisTemplate.execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                return redis.sort(redisKey, RedisClient.SortParam.limit(offset, max));
            }
        });
    }
}
