package org.grails.datastore.mapping.cache.impl;

import org.grails.datastore.mapping.cache.TPCacheAdapter;
import org.grails.datastore.mapping.cache.exception.CacheException;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple implementation of {@link org.grails.datastore.mapping.cache.TPCacheAdapter} backed by a local hash map.
 *
 * @author Roman Stepanenko
 */
public class HashMapTPCacheAdapter<T> implements TPCacheAdapter<T> {
    public void cacheEntry(Serializable key, T entry) throws CacheException {
        cache.put(key, entry);
    }

    public T getCachedEntry(Serializable key) throws CacheException {
        return cache.get(key);
    }

    private ConcurrentHashMap<Object, T> cache = new ConcurrentHashMap<Object, T>();
}
