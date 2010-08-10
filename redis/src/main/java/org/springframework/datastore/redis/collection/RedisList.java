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

import org.springframework.datastore.redis.util.RedisTemplate;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Creates a list that is backed onto a Redis list
 *
 * @author Graeme Rocher
 * @since 1.0
 * 
 */
public class RedisList extends AbstractRedisCollection implements List {
    public RedisList(RedisTemplate redisTemplate, String redisKey) {
        super(redisTemplate, redisKey);
    }

    public boolean addAll(int index, Collection c) {
        throw new UnsupportedOperationException("Method addAll(index, collection) not implemented");
    }

    public Object get(int index) {
        return redisTemplate.lindex(redisKey, index);
    }

    public Object set(int index, Object element) {
        Object prev = get(index);
        redisTemplate.lset(redisKey, index, element);
        return prev;
    }

    public void add(int index, Object element) {
        throw new UnsupportedOperationException("Method add(index, element) not implemented");
    }

    public Object remove(int index) {
        Object o = get(index);
        remove(o);
        return o;
    }

    public int indexOf(Object o) {
        throw new UnsupportedOperationException("Method indexOf(Object) not implemented");
    }

    public int lastIndexOf(Object o) {
        throw new UnsupportedOperationException("Method lastIndexOf(Object) not implemented");
    }

    public ListIterator listIterator() {
        throw new UnsupportedOperationException("Method listIterator() not implemented");
    }

    public ListIterator listIterator(int index) {
        throw new UnsupportedOperationException("Method listIterator() not implemented");
    }

    public List subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException("Method subList(fromIndex, toIndex) not implemented");
    }

    public int size() {
        return (int) redisTemplate.llen(redisKey);
    }

    public boolean contains(Object o) {
        return elements().contains(o);
    }

    public Iterator iterator() {
        return new RedisIterator(elements(), this);
    }

    private List<byte[]> elements() {
        return redisTemplate.lrange(redisKey, 0, -1);
    }

    public boolean add(Object e) {
        redisTemplate.rpush(redisKey, e);
        return true;
    }

    public boolean remove(Object o) {
        return redisTemplate.lrem(redisKey, o, 0);
    }
}
