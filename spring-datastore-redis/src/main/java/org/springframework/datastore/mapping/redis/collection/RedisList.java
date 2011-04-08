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

import org.springframework.datastore.mapping.redis.util.RedisTemplate;

import java.util.*;

/**
 * Creates a list that is backed onto a Redis list
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class RedisList extends AbstractList implements List, RedisCollection {
    private RedisTemplate redisTemplate;
    private String redisKey;

    public RedisList(RedisTemplate redisTemplate, String redisKey) {
        this.redisTemplate = redisTemplate;
        this.redisKey = redisKey;
    }

    @Override
    public Object get(int index) {
        return redisTemplate.lindex(redisKey, index);
    }

    @Override
    public Object set(int index, Object element) {
        Object prev = get(index);
        redisTemplate.lset(redisKey, index, element);
        return prev;
    }

    @Override
    public void add(int index, Object element) {
        if (index == 0) {
            redisTemplate.lpush(redisKey, element);
        }
        else if (index == size()) {
            redisTemplate.rpush(redisKey, element);
        }
        else {
            throw new UnsupportedOperationException("Redis lists only support adding elements at the beginning or end of a list");
        }
    }

    @Override
    public Object remove(int index) {
        Object o = get(index);
        remove(o);
        return o;
    }

    @Override
    public int size() {
        return Long.valueOf(redisTemplate.llen(redisKey)).intValue();
    }

    @Override
    public boolean contains(Object o) {
        return Arrays.asList(elements()).contains(o);
    }

    @Override
    public Iterator iterator() {
        return elements().iterator();
    }

    private List<String> elements() {
        return redisTemplate.lrange(redisKey, 0, -1);
    }

    @Override
    public boolean add(Object e) {
        redisTemplate.rpush(redisKey, e);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        return redisTemplate.lrem(redisKey, o, 0) != 0;
    }

    public String getRedisKey() {
        return redisKey;
    }

    public Set<String> members() {
        return new HashSet(redisTemplate.lrange(redisKey, 0, -1));
    }

    public List<String> members(int offset, int max) {
        return redisTemplate.lrange(redisKey, offset, max);
    }
}
