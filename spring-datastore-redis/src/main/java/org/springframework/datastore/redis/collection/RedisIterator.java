package org.springframework.datastore.redis.collection;

import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;

import java.util.Collection;
import java.util.Iterator;

/**
 * An iterator for iterating over Redis results
 *
 * @author Graeme Rocher
 *
 */
public class RedisIterator implements Iterator {
    private String[] values;
    private Collection collection;
    private int index = 0;
    private Object current;
    private TypeConverter converter = new SimpleTypeConverter();

    public RedisIterator(String[] values, Collection col) {
        this.values = values;
        this.collection = col;
    }

    public boolean hasNext() {
        return index < values.length;
    }

    public Object next() {
        current = values[index++];
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
