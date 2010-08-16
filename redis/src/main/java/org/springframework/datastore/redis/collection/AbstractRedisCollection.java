package org.springframework.datastore.redis.collection;

import org.springframework.datastore.redis.util.RedisTemplate;

import java.util.Collection;
import java.util.Iterator;

/**
 * Abstract base class for Redis collections
 *
 * @author Graeme Rocher
 */
abstract public class AbstractRedisCollection implements Collection, RedisCollection {
    protected RedisTemplate redisTemplate;
    protected String redisKey;

    public AbstractRedisCollection(RedisTemplate redisTemplate, String redisKey) {
        this.redisTemplate = redisTemplate;
        this.redisKey = redisKey;
    }


    /**
     * They key used by the collection
     *
     * @return The redis key
     */
    public String getRedisKey() {
        return redisKey;
    }

    public void clear() {
        redisTemplate.del(redisKey);
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public Object[] toArray() {
        return new Object[0];
    }

    public boolean containsAll(Collection c) {
        for (Object o : c) {
            if(!contains(o)) return false;
        }
        return true;
    }

    public boolean addAll(Collection c) {
        boolean changed  = false;
        for (Object e : c) {
            boolean elChange = add(e);
            if(elChange && !changed) changed = true;
        }
        return changed;
    }

    public boolean retainAll(Collection c) {
        Iterator i = iterator();
        boolean changed = false;
        while (i.hasNext()) {
            Object o = i.next();
            if(!c.contains(o)) {
                i.remove();
                changed = true;
            }
        }
        return changed;
    }

    public boolean removeAll(Collection c) {
        boolean changed  = false;
        for (Object e : c) {
            boolean elChange = remove(e);
            if(elChange && !changed) changed = true;
        }
        return changed;

    }

    public Object[] toArray(Object[] array) {
        return new Object[0];
    }
}
