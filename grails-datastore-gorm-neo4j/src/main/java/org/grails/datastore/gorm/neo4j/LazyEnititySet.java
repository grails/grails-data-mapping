package org.grails.datastore.gorm.neo4j;

import org.grails.datastore.gorm.neo4j.simplegraph.Relationship;
import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.proxy.ProxyFactory;
import org.neo4j.helpers.collection.IteratorUtil;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by stefan on 20.03.14.
 */
public class LazyEnititySet<T> implements Set<T> {

    private final Collection<Relationship> relationships;
    private final ProxyFactory proxyFactory;
    private final Session session;
    private final Class<T> clazz;
    private final Long sourceId;
    private final Set<T> delegate = new HashSet<T>();
    private boolean initialized = false;

    public LazyEnititySet(Iterable<Relationship> relationshipIterable, ProxyFactory proxyFactory, Session session, Class<T> clazz, Long sourceId) {
        this.relationships = IteratorUtil.asCollection(relationshipIterable);
        this.proxyFactory = proxyFactory;
        this.session = session;
        this.clazz = clazz;
        this.sourceId = sourceId;
    }

    private void initialize() {
        if (!initialized) {
            initialized = true;
            for (Relationship relationship: relationships) {
                delegate.add(proxyFactory.createProxy(session, clazz, relationship.getOtherId(sourceId)));
            }
        }
    }

    @Override
    public int size() {
        return initialized ? delegate.size() : relationships.size();
    }

    @Override
    public boolean isEmpty() {
        return  initialized ? delegate.isEmpty() : relationships.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        initialize();
        return delegate.contains(o);
    }

    @Override
    public Iterator iterator() {
        initialize();
        return delegate.iterator();
    }

    @Override
    public Object[] toArray() {
        initialize();
        return delegate.toArray();
    }

    @Override
    public boolean add(Object o) {
        initialize();
        return delegate.add((T) o);
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
//        return false;
    }

    @Override
    public boolean addAll(Collection collection) {
        throw new UnsupportedOperationException();
//        return false;
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();

    }

    @Override
    public boolean removeAll(Collection collection) {
        throw new UnsupportedOperationException();
//        return false;
    }

    @Override
    public boolean retainAll(Collection collection) {
        throw new UnsupportedOperationException();
//        return false;
    }

    @Override
    public boolean containsAll(Collection collection) {
        throw new UnsupportedOperationException();
//        return false;
    }

    @Override
    public T[] toArray(Object[] objects) {
        throw new UnsupportedOperationException();
//        return new T[0];
    }
}
