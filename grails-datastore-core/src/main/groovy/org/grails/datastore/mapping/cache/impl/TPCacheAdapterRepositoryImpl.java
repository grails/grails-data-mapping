package org.grails.datastore.mapping.cache.impl;

import org.grails.datastore.mapping.cache.TPCacheAdapter;
import org.grails.datastore.mapping.cache.TPCacheAdapterRepository;
import org.grails.datastore.mapping.model.PersistentEntity;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple implementation of {@link TPCacheAdapterRepository}
 *
 * @author Roman Stepanenko
 */
public class TPCacheAdapterRepositoryImpl<T> implements TPCacheAdapterRepository<T> {
    public TPCacheAdapter<T> getTPCacheAdapter(PersistentEntity entity) {
        if (entity == null) {
            return null;
        }

        return adapters.get(entity.getJavaClass().getName());
    }

    public void setTPCacheAdapter(PersistentEntity entity, TPCacheAdapter<T> cacheAdapter) {
        setTPCacheAdapter(entity.getJavaClass(), cacheAdapter);
    }

    public void setTPCacheAdapter(Class entityJavaClass, TPCacheAdapter<T> cacheAdapter) {
        setTPCacheAdapter(entityJavaClass.getName(), cacheAdapter);
    }

    public void setTPCacheAdapter(String entityJavaClassFQN, TPCacheAdapter<T> cacheAdapter) {
        adapters.put(entityJavaClassFQN, cacheAdapter);
    }

    private ConcurrentHashMap<String, TPCacheAdapter<T>> adapters = new ConcurrentHashMap<String, TPCacheAdapter<T>>();
}
