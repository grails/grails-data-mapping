package org.springframework.datastore.redis.collection;

import java.util.Collection;
import java.util.List;

/**
 * Commons interface for Redis collections
 */
public interface RedisCollection extends Collection {

    /**
     * They key used by the collection
     *
     * @return The redis key
     */    
    String getRedisKey();
    
    String[] members();

    String[] members(int offset, int max);
}
