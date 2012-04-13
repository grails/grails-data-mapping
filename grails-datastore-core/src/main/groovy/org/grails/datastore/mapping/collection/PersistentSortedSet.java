package org.grails.datastore.mapping.collection;

import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.engine.AssociationIndexer;

import java.io.Serializable;
import java.util.*;

/**
 * A lazy loaded sorted set.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings("rawtypes")
public class PersistentSortedSet extends AbstractPersistentCollection implements SortedSet {

    public PersistentSortedSet(Class childType, Session session, SortedSet collection) {
        super(childType, session, collection);
    }

    public PersistentSortedSet(Collection keys, Class childType, Session session) {
        super(keys, childType, session, new TreeSet());
    }

    public PersistentSortedSet(Serializable associationKey, Session session, AssociationIndexer indexer) {
        super(associationKey, session, indexer, new TreeSet());
    }

    @Override
    public Comparator comparator() {
        return getSortedSet().comparator();
    }

    private SortedSet getSortedSet() {
        initialize();
        return ((SortedSet)collection);
    }

    @Override
    public SortedSet subSet(Object o, Object o1) {
        return getSortedSet().subSet(o,o1);
    }

    @Override
    public SortedSet headSet(Object o) {
        return getSortedSet().headSet(o);
    }

    @Override
    public SortedSet tailSet(Object o) {
        return getSortedSet().tailSet(o);
    }

    @Override
    public Object first() {
        return getSortedSet().first();
    }

    @Override
    public Object last() {
        return getSortedSet().last();
    }
}

