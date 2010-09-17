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

import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.engine.AssociationIndexer;

import java.io.Serializable;
import java.util.*;

/**
 * A lazy loaded list
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class PersistentList extends ArrayList implements PersistentCollection{

    private boolean initialized;
    private Serializable associationKey;
    private Session session;
    private AssociationIndexer indexer;

    public PersistentList(Serializable associationKey, Session session, AssociationIndexer indexer) {
        this.associationKey = associationKey;
        this.session = session;
        this.indexer = indexer;
    }

    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public int size() {
        if(!isInitialized()) initialize();
        return super.size();
    }

    @Override
    public boolean isEmpty() {
        if(!isInitialized()) initialize();
        return super.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        if(!isInitialized()) initialize();
        return super.contains(o);
    }

    @Override
    public int indexOf(Object o) {
        if(!isInitialized()) initialize();
        return super.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        if(!isInitialized()) initialize();
        return super.lastIndexOf(o);
    }

    @Override
    public Object clone() {
        if(!isInitialized()) initialize();
        return super.clone();
    }

    @Override
    public Object[] toArray() {
        if(!isInitialized()) initialize();
        return super.toArray();
    }

    @Override
    public Object[] toArray(Object[] a) {
        if(!isInitialized()) initialize();
        return super.toArray(a);
    }

    @Override
    public Object get(int index) {
        if(!isInitialized()) initialize();
        return super.get(index);
    }

    @Override
    public Object set(int index, Object element) {
        if(!isInitialized()) initialize();
        return super.set(index, element);
    }

    @Override
    public boolean add(Object o) {
        if(!isInitialized()) initialize();
        return super.add(o);
    }

    @Override
    public void add(int index, Object element) {
        if(!isInitialized()) initialize();
        super.add(index, element);
    }

    @Override
    public Object remove(int index) {
        if(!isInitialized()) initialize();
        return super.remove(index);
    }

    @Override
    public boolean remove(Object o) {
        if(!isInitialized()) initialize();
        return super.remove(o);
    }

    @Override
    public void clear() {
        if(!isInitialized()) initialize();
        super.clear();
    }

    @Override
    public boolean addAll(Collection c) {
        if(!isInitialized()) initialize();
        return super.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection c) {
        if(!isInitialized()) initialize();
        return super.addAll(index, c);
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        if(!isInitialized()) initialize();
        super.removeRange(fromIndex, toIndex);
    }

    @Override
    public Iterator iterator() {
        if(!isInitialized()) initialize();
        return super.iterator();
    }

    @Override
    public ListIterator listIterator() {
        if(!isInitialized()) initialize();
        return super.listIterator();
    }

    @Override
    public ListIterator listIterator(int index) {
        if(!isInitialized()) initialize();
        return super.listIterator(index);
    }

    @Override
    public List subList(int fromIndex, int toIndex) {
        if(!isInitialized()) initialize();
        return super.subList(fromIndex, toIndex);
    }

    @Override
    public boolean equals(Object o) {
        if(!isInitialized()) initialize();
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        if(!isInitialized()) initialize();
        return super.hashCode();
    }

    
    @Override
    public String toString() {
        if(!isInitialized()) initialize();
        return super.toString();
    }

    public void initialize() {
        if(!initialized) {
            initialized = true;
            List results = indexer.query(associationKey);
            addAll( session.retrieveAll(indexer.getIndexedEntity().getJavaClass(), results) );
        }
    }
}
