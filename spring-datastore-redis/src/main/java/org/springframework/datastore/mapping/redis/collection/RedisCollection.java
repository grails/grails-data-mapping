package org.springframework.datastore.mapping.redis.collection;

import java.util.Collection;
import java.util.List;
import java.util.Set;

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
    
    Set<String> members();

    List<String> members(int offset, int max);
}
