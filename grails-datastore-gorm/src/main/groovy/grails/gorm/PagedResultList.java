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
package grails.gorm;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.List;

import org.grails.datastore.mapping.query.Query;

/**
 * A result list implementation that provides an additional property called 'totalCount' to obtain the total number of
 * records. Useful for pagination.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class PagedResultList extends AbstractList implements Serializable {


    private static final long serialVersionUID = -5820655628956173929L;

    private Query query;
    protected List resultList;
    protected int totalCount = Integer.MIN_VALUE;

    public PagedResultList(Query query) {
        this.query = query;
    }

    /**
     * @return The total number of records for this query
     */
    public int getTotalCount() {
        initialize();
        if (totalCount == Integer.MIN_VALUE) {
            Query newQuery = (Query)query.clone();
            newQuery.projections().count();
            Number result = (Number) newQuery.singleResult();
            totalCount = result == null ? 0 : result.intValue();
        }

        return totalCount;
    }

    @Override
    public Object get(int i) {
        initialize();
        return resultList.get(i);
    }

    @Override
    public Object set(int i, Object o) {
        initialize();
        return resultList.set(i, o);
    }

    @Override
    public Object remove(int i) {
        initialize();
        return resultList.remove(i);
    }

    @Override
    public void add(int i, Object o) {
        initialize();
        resultList.add(i, o);
    }

    protected void initialize() {
        if (resultList == null) {
            resultList = query.list();
        }
    }

    @Override
    public int size() {
        initialize();
        return resultList.size();
    }

    @Override
    public boolean equals(Object o) {
        initialize();
        return resultList.equals(o);
    }

    @Override
    public int hashCode() {
        initialize();
        return resultList.hashCode();
    }

    private void writeObject(ObjectOutputStream out) throws IOException {

        // find the total count if it hasn't been done yet so when this is deserialized
        // the null GrailsHibernateTemplate won't be an issue
        getTotalCount();

        out.defaultWriteObject();
    }
}
