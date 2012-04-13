/* Copyright (C) 2011 SpringSource
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
package org.grails.datastore.mapping.collection;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.engine.AssociationIndexer;

/**
 * Abstract base class for persistent collections.
 *
 * @author Burt Beckwith
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class AbstractPersistentCollection implements PersistentCollection {

    private boolean initialized;
    private Serializable associationKey;
    private Session session;
    private AssociationIndexer indexer;
    private Collection keys;
    private Class childType;
    private boolean dirty = false;

    protected final Collection collection;

    protected AbstractPersistentCollection(Class childType, Session session, Collection collection) {
        this.childType = childType;
        this.collection = collection;
        this.session = session;
    }

    protected AbstractPersistentCollection(Collection keys, Class childType,
            Session session, Collection collection) {
        this.session = session;
        this.keys = keys;
        this.childType = childType;
        this.collection = collection;
    }

    protected AbstractPersistentCollection(Serializable associationKey, Session session,
            AssociationIndexer indexer, Collection collection) {
        this.session = session;
        this.associationKey = associationKey;
        this.indexer = indexer;
        this.collection = collection;
    }

    /* Collection methods */

    public Iterator iterator() {
        initialize();

        final Iterator iterator = collection.iterator();
        return new Iterator() {
            public boolean hasNext() {
                return iterator.hasNext();
            }

            public Object next() {
                return iterator.next();
            }

            public void remove() {
                iterator.remove();
                markDirty();
            }
        };
    }

    public int size() {
        initialize();
        return collection.size();
    }

    public boolean isEmpty() {
        initialize();
        return collection.isEmpty();
    }

    public boolean contains(Object o) {
        initialize();
        return collection.contains(o);
    }

    public boolean add(Object o) {
        initialize();
        boolean added = collection.add(o);
        if (added) {
            markDirty();
        }
        return added;
    }

    public boolean remove(Object o) {
        initialize();
        boolean remove = collection.remove(o);
        if (remove) {
            markDirty();
        }
        return remove;
    }

    public void clear() {
        initialize();
        collection.clear();
        markDirty();
    }

    @Override
    public boolean equals(Object o) {
        initialize();
        return collection.equals(o);
    }

    @Override
    public int hashCode() {
        initialize();
        return collection.hashCode();
    }

    @Override
    public String toString() {
        initialize();
        return collection.toString();
    }

    public boolean removeAll(Collection c) {
        initialize();
        boolean changed = collection.removeAll(c);
        if (changed) {
            markDirty();
        }
        return changed;
    }

    public Object[] toArray() {
        initialize();
        return collection.toArray();
    }

    public Object[] toArray(Object[] a) {
        initialize();
        return collection.toArray(a);
    }

    public boolean containsAll(Collection c) {
        initialize();
        return collection.containsAll(c);
    }

    public boolean addAll(Collection c) {
        initialize();
        boolean changed = collection.addAll(c);
        if (changed) {
            markDirty();
        }
        return changed;
    }

    public boolean retainAll(Collection c) {
        initialize();
        boolean changed = collection.retainAll(c);
        if (changed) {
            markDirty();
        }
        return changed;
    }

    /* PersistentCollection methods */

    public boolean isInitialized() {
        return initialized;
    }

    public void initialize() {
        if (initialized) {
            return;
        }

        initialized = true;

        if (associationKey == null) {
            if (keys != null) {
                addAll(session.retrieveAll(childType, keys));
            }
        }
        else {
            List results = indexer.query(associationKey);
            addAll(session.retrieveAll(indexer.getIndexedEntity().getJavaClass(), results));
        }
    }

    public boolean isDirty() {
        return dirty;
    }

    public void resetDirty() {
        dirty = false;
    }

    /* misc methods */

    public void markDirty() {
        dirty = true;
    }
}
