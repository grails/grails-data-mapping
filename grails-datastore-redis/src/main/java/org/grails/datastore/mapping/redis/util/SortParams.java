/* Copyright (C) 2010 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.grails.datastore.mapping.redis.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Parameters used for sorting.
 *
 * @author Graeme Rocher
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
