package org.grails.datastore.gorm.neo4j;

import org.grails.datastore.mapping.engine.EntityAccess;
import org.grails.datastore.mapping.model.types.Association;

import java.util.*;

/**
 * A {@link java.util.Set} implementation setting it's owner's dirty status upon
 * modification of the set. Internally {@link org.grails.datastore.mapping.dirty.checking.DirtyCheckable}
 * is used which is applied to all domain classes using an AST transformation
 */
public class DirtyCheckableAwareList<T> extends DirtyCheckableAwareCollection<T> implements List<T> {

    private final List<T> delegate;

    public DirtyCheckableAwareList(EntityAccess owner, Association association, List<T> delegate, Neo4jSession session) {
        super(owner, association, delegate, session);
        this.delegate = delegate;
    }

    @Override
    public boolean addAll(int i, Collection<? extends T> ts) {
        boolean added = delegate.addAll(i, ts);
        if (added) {
            markDirty();
            for (T t :ts) {
                adoptGraphUponAdd(t);
            }
        }
        return added;
    }

    @Override
    public T get(int i) {
        return delegate.get(i);
    }

    @Override
    public T set(int i, T t) {
        T old = delegate.set(i, t);
        if (!t.equals(old)) {
            markDirty();
            adoptGraphUponRemove(old);
            adoptGraphUponAdd(t);
        }
        return old;
    }

    @Override
    public void add(int i, T t) {
        markDirty();
        delegate.add(i, t);
        adoptGraphUponAdd(t);
    }

    @Override
    public T remove(int i) {
        markDirty();
        T removed = delegate.remove(i);
        adoptGraphUponRemove(removed);
        return removed;
    }

    @Override
    public int indexOf(Object o) {
        return delegate.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return delegate.lastIndexOf(o);
    }

    @Override
    public ListIterator<T> listIterator() {
        return delegate.listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int i) {
        return delegate.listIterator(i);
    }

    @Override
    public List<T> subList(int i, int i2) {
        return delegate.subList(i, i2);
    }
}
