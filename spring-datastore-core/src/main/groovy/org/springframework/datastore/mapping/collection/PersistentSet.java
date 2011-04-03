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

package org.springframework.datastore.mapping.collection;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.engine.AssociationIndexer;

/**
 * A lazy loaded set.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class PersistentSet extends HashSet implements PersistentCollection {

    private boolean initialized;
    private Serializable associationKey;
    private Session session;
    private AssociationIndexer indexer;

    public PersistentSet(Serializable associationKey, Session session, AssociationIndexer indexer) {
        this.associationKey = associationKey;
        this.session = session;
        this.indexer = indexer;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void initialize() {
        if (initialized) {
            return;
        }

        initialized = true;
        List results = indexer.query(associationKey);
        addAll(session.retrieveAll(indexer.getIndexedEntity().getJavaClass(), results));
    }

    @Override
    public String toString() {
        initialize();
        return super.toString();
    }

    @Override
    public Iterator iterator() {
        initialize();
        return super.iterator();
    }

    @Override
    public int size() {
        initialize();
        return super.size();
    }

    @Override
    public boolean isEmpty() {
        initialize();
        return super.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        initialize();
        return super.contains(o);
    }

    @Override
    public boolean add(Object o) {
        initialize();
        return super.add(o);
    }

    @Override
    public boolean remove(Object o) {
        initialize();
        return super.remove(o);
    }

    @Override
    public void clear() {
        initialize();
        super.clear();
    }

    @Override
    public Object clone() {
        initialize();
        return super.clone();
    }

    @Override
    public boolean equals(Object o) {
        initialize();
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        initialize();
        return super.hashCode();
    }

    @Override
    public boolean removeAll(Collection c) {
        initialize();
        return super.removeAll(c);
    }

    @Override
    public Object[] toArray() {
        initialize();
        return super.toArray();
    }

    @Override
    public Object[] toArray(Object[] a) {
        initialize();
        return super.toArray(a);
    }

    @Override
    public boolean containsAll(Collection c) {
        initialize();
        return super.containsAll(c);
    }

    @Override
    public boolean addAll(Collection c) {
        initialize();
        return super.addAll(c);
    }

    @Override
    public boolean retainAll(Collection c) {
        initialize();
        return super.retainAll(c);
    }
}
