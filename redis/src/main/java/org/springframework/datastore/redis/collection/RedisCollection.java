package org.springframework.datastore.redis.collection;

import java.util.Collection;
import java.util.List;

/**
 * Commons interface for Redis collections
 */
public interface RedisCollection extends Collection {
    
    List<byte[]> members();

    List<byte[]> members(int offset, int max);
}
