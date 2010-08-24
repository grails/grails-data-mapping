package org.springframework.datastore.redis.util;

import sma.RedisClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 24, 2010
 * Time: 4:04:59 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class SortParams<T> {

    List<T> paramList = new ArrayList<T>();

    public List<T> getParamList() {
        return paramList;
    }

    public SortParams by(final String pattern) {
        paramList.add(createBy(pattern));
        return this;
    }

    public SortParams limit(final int start, final int count) {

        paramList.add(createLimit(start, count));
        return this;
    }

    public SortParams get(final String pattern) {
        paramList.add(createGet(pattern));
        return this;
    }

    public SortParams asc() {
        paramList.add(createAsc());
        return this;
    }

    public SortParams desc() {
        paramList.add(createDesc());
        return this;
    }

    public SortParams alpha() {
        paramList.add(createAlpha());
        return this;
    }

    protected abstract T createAlpha();

    protected abstract T createDesc();

    protected abstract T createGet(String pattern);

    protected abstract T createLimit(int start, int count);

    protected abstract T createAsc();

    protected abstract T createBy(String pattern);


    public T[] getParamArray() {
        return (T[])paramList.toArray();
    }
}
