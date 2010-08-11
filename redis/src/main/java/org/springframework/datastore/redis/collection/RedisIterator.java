package org.springframework.datastore.redis.collection;

import org.springframework.beans.TypeConverter;
import org.springframework.datastore.keyvalue.convert.ByteArrayAwareTypeConverter;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * An iterator for iterating over Redis results
 *
 * @author Graeme Rocher
 *
 */
public class RedisIterator implements Iterator {
    private List<byte[]> values;
    private Collection collection;
    private int index = 0;
    private RedisValue current;
    private TypeConverter converter = new ByteArrayAwareTypeConverter();

    public RedisIterator(List<byte[]> values, Collection col) {
        this.values = values;
        this.collection = col;
    }

    public boolean hasNext() {
        return index < values.size();
    }

    public Object next() {
        current = new RedisValue(values.get(index++), converter);
        return current;
    }

    public void remove() {
        if(current != null)
            collection.remove(current);
    }

    public Long toLong() {
        if(current != null) {
           return converter.convertIfNecessary(current, Long.class);
        }
        return 0L;
    }

    public String toString() {
        if(current != null)
            return converter.convertIfNecessary(current, String.class);
        else
            return super.toString();
    }

}
