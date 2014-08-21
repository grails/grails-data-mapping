package org.grails.datastore.gorm.neo4j;

import org.grails.datastore.mapping.dirty.checking.DirtyCheckable;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.types.Association;
import org.grails.datastore.mapping.model.types.ManyToMany;

import java.util.Collection;
import java.util.Iterator;

/**
 * A {@link java.util.Collection} implementation setting it's owner's dirty status upon
 * modification of the set. Internally {@link org.grails.datastore.mapping.dirty.checking.DirtyCheckable}
 * is used which is applied to all domain classes using an AST transformation.
 */
public abstract class DirtyCheckableAwareCollection<T> implements Collection<T> {

    private final EntityAccess owner;
    private final Association association;
    private final Collection<T> delegate;
    private final Neo4jSession session;
    private final boolean reversed;
    private final String relType;

    protected DirtyCheckableAwareCollection(EntityAccess owner, Association association, Collection<T> delegate, Neo4jSession session) {
        this.owner = owner;
        this.association = association;
        this.delegate = delegate;
        this.session = session;
        reversed = RelationshipUtils.useReversedMappingFor(association);
        relType = RelationshipUtils.relationshipTypeUsedFor(association);

        for (T obj: delegate) {
            session.addPendingInsert(new RelationshipPendingInsert(owner, relType, new EntityAccess(association.getAssociatedEntity(), obj), session.getNativeInterface()));
        }
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return delegate.contains(0);
    }

    @Override
    public Iterator<T> iterator() {
        return delegate.iterator();
    }

    @Override
    public Object[] toArray() {
        return delegate.toArray();
    }

    @Override
    public <T> T[] toArray(T[] ts) {
        return delegate.toArray(ts);
    }

    @Override
    public boolean add(T t) {
        boolean isAdded = delegate.add(t);
        if (isAdded) {
            markDirty();
            adoptGraphUponAdd(t);
        }
        return isAdded;
    }

    protected void adoptGraphUponAdd(T t) {
        if (session.getMappingContext().getProxyFactory().isProxy(t)) {
            if (!isDomainInstance(t)) return;
        }
        EntityAccess target = new EntityAccess(association.getAssociatedEntity(), t);
        if (association.isBidirectional()) {
            if (association instanceof ManyToMany) {
                Collection coll = (Collection) target.getProperty(association.getReferencedPropertyName());
                coll.add(owner.getEntity());
            } else {
                target.setProperty(association.getReferencedPropertyName(), owner.getEntity());
            }
        }

        if (target.getIdentifier() == null) { // non-persistent instance
            session.persist(t);
        }

        if (!reversed) { // prevent duplicated rels
            session.addPendingInsert(new RelationshipPendingInsert(owner, relType, target, session.getNativeInterface()));
        }
    }

    /**
     * UpdateWithProxyPresentSpec from TCK forces usage of {@link org.grails.datastore.gorm.proxy.GroovyProxyFactory},
     * it's isProxy method is not 100% solid so we need to workaround here
     * @param t
     * @return
     */
    private boolean isDomainInstance(T t) {
        boolean isDomainInstance = false;
        for (PersistentEntity pe: session.getMappingContext().getPersistentEntities()) {
            if (pe.getJavaClass().equals(t.getClass())) {
                isDomainInstance = true;
            }
        }
        return isDomainInstance;
    }

    @Override
    public boolean remove(Object o) {
        boolean isDeleted = delegate.remove(o);
        if (isDeleted) {
            markDirty();
            adoptGraphUponRemove(o);
        }
        return isDeleted;

    }

    protected void adoptGraphUponRemove(Object o) {
        if (session.getMappingContext().getProxyFactory().isProxy(o)) {
            return;
        }
        if (!reversed) {
            session.addPendingInsert(new RelationshipPendingDelete(owner, relType,
                    new EntityAccess(association.getAssociatedEntity(), o),
                    session.getNativeInterface()));
        }
    }

    @Override
    public boolean containsAll(Collection<?> objects) {
        return delegate.containsAll(objects);
    }

    @Override
    public boolean addAll(Collection<? extends T> ts) {
        boolean changed = delegate.addAll(ts);
        if (changed) {
            markDirty();
        }
        return changed;
    }

    public boolean addAll(Iterable<T> objects) {
        boolean result = false;
        for (T obj : objects) {
            result |= add(obj);
        }
        return result;
    }

    @Override
    public boolean removeAll(Collection<?> objects) {
        markDirty();
        return delegate.removeAll(objects);
    }

    @Override
    public boolean retainAll(Collection<?> objects) {
        markDirty();
        return delegate.retainAll(objects);
    }

    @Override
    public void clear() {
        markDirty();
        delegate.clear();
    }

    protected void markDirty() {
        Object ownerEntity = owner.getEntity();
        if (ownerEntity instanceof DirtyCheckable) {
            ((DirtyCheckable) ownerEntity).markDirty(association.getName());
        }
    }

    /*@Override
    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }*/
}
