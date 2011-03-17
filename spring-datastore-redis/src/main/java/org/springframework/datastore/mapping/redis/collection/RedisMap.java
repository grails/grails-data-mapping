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
package org.springframework.datastore.mapping.redis.collection;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.datastore.mapping.redis.util.RedisTemplate;

/**
 * A map that is backed onto a Redis hash
 *
 * @author Graeme Rocher
 */
public class RedisMap extends AbstractMap {

    private String redisKey;
    private RedisTemplate redisTemplate;

    public RedisMap(RedisTemplate template, String redisKey) {
        this.redisKey = redisKey;
        this.redisTemplate = template;
    }

    @Override
    public Set entrySet() {

        final Map<String,String> redisMap = redisTemplate.hgetall(redisKey);
        Set entrySet = new HashSet();
        for (final String key : redisMap.keySet()) {
            entrySet.add(new Map.Entry() {
                public Object getKey() {
                    return redisMap.get(key);
                }

                public Object getValue() {
                    return redisMap.get(key);
                }

                public Object setValue(Object value) {
                    Object current = getValue();
                    redisTemplate.hset(redisKey, key, value);
                    return current;
                }
            });
        }
        return entrySet;
    }

    @Override
    public Object put(Object key, Object value) {
        Object current = get(key);

        if (key != null) {
            redisTemplate.hset(redisKey, key.toString(), value);
        }
        return current;
    }

    @Override
    public Object remove(Object key) {
        Object current = get(key);

        if (key != null) {
            redisTemplate.hdel(redisKey, key.toString());
        }
        return current;
    }

    @Override
    public Object get(Object key) {
        if (key != null) {
            return redisTemplate.hget(redisKey, key.toString());
        }
        return super.get(key);
    }

    @Override
    public int size() {
        return redisTemplate.hlen(redisKey);
    }
}
