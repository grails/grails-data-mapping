package org.springframework.datastore.mapping.redis.collection;

import org.springframework.beans.TypeConverter;

public class RedisValue {
    private byte[] value;
    private TypeConverter converter;

    public RedisValue(byte[] bytes, TypeConverter converter) {
        this.value = bytes;
        this.converter = converter;
    }

    public byte[] getBytes() {
        return value;
    }

    public String plus(String str) {
        return toString() + str;
    }

    public Long plus(Long n) {
        return toLong() + n;
    }

    public Long plus(Integer n) {
        return toLong() + n;
    }

    public Long toLong() {
        return converter.convertIfNecessary(value, Long.class);
    }

    @Override
    public String toString() {
        return converter.convertIfNecessary(value, String.class);
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CharSequence) {
            return toString().equals(obj.toString());
        }
        if (obj instanceof Number) {
            return toLong().equals(obj);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}