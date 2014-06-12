package org.grails.datastore.mapping.cache;

import org.grails.datastore.mapping.cache.exception.CacheException;

import java.io.Serializable;

/**
 * <p>
 * Third party cache adapter responsible for handling put and get cache operations
 * for concrete third party cache such as infinispan or coherence.
 * Each TPCacheAdapter is registered per type of PersistentEntity via {@link TPCacheAdapterRepository}.
 * </p>
 * <p>
 * Current API does not yet provide bulk operations because the rest of the engine is not bulk-optimized.
 * </p>
 * <p>
 * Transactional semantics: depending on the concrete third-party cache capabilities and the presence of JTA transaction
 * the implementation might perform the write operations at the commit rather than when a {@link #cacheEntry(java.io.Serializable, Object)}
 * is called.
 * </p>
 * <p>
 * Concurrency: each <code>TPCacheAdapter</code> must be thread-safe.
 * </p>
 * @author Roman Stepanenko
 */
public interface TPCacheAdapter<T> {
    /**
     * Stores a cached entry in a <b>synchronous</b> manner.
     * <p>
     * Transactional semantics: depending on the concrete third-party cache capabilities and the presence of JTA transaction,
     * for network optimization reasons the implementation might perform the actual cache cluster update operations at the commit
     * rather than when a {@link #cacheEntry(java.io.Serializable, Object)} is called.
     * </p>
     * <p>
     * In cases when there is no transaction or no transactional support by the implementation, if there are any problems
     * storing the entry the caller is notified about it via exception in the calling thread; also, if this method returns
     * successfully it means that the logistics of putting the specified value in the cache are fully done.
     * </p>
     * @param key the entry key
     * @param entry the entry
     * @throws CacheException runtime exception indicating any cache-related problems
     */
    void cacheEntry(Serializable key, T entry) throws CacheException;

    /**
     * Returns the stored value for the specified key.
     * @param key the entry key
     * @return the entry
     * @throws CacheException runtime exception indicating any cache-related problems
     */
    T getCachedEntry(Serializable key) throws CacheException;
}
