package org.grails.datastore.gorm.neo4j;

import org.grails.datastore.gorm.neo4j.engine.CypherResult;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.grails.datastore.mapping.model.types.Association;
import org.grails.datastore.mapping.proxy.ProxyFactory;

import java.util.*;

/**
 * Created by stefan on 20.03.14.
 */
public class LazyEnititySet<T> implements Set<T> {

    private final ProxyFactory proxyFactory;
    private final Neo4jSession session;
    private final EntityAccess owner;
    private final Set<T> delegate = new HashSet<T>();
    private final Association association;
    private boolean initialized = false;
    private boolean reversed;
    private String relType;

    public LazyEnititySet(EntityAccess owner, Association association, ProxyFactory proxyFactory, Neo4jSession session) {
        this.owner = owner;
        this.association = association;
        this.proxyFactory = proxyFactory;
        this.session = session;
        reversed = RelationshipUtils.useReversedMappingFor(association);
        relType = RelationshipUtils.relationshipTypeUsedFor(association);
    }

    private void initialize() {
        if (!initialized) {
            initialized = true;
            String cypher = CypherBuilder.findRelationshipEndpointIdsFor(association);
            CypherResult result = session.getNativeInterface().execute(cypher, Collections.singletonList(owner.getIdentifier()));

            Class<T> clazz = association.getAssociatedEntity().getJavaClass();
            for (Map<String, Object> row : result) {
                Long endpoint = (Long) row.get("id");
                delegate.add( proxyFactory.createProxy(session, clazz, endpoint));
            }
        }
    }

    @Override
    public int size() {
        initialize();
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        initialize();
        return  delegate.isEmpty();
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
        boolean isNew = delegate.add((T) o);
        if (isNew) {
            EntityAccess target = new EntityAccess(association.getAssociatedEntity(), o);

            if (association.isBidirectional()) {
                target.setProperty(association.getReferencedPropertyName(), owner.getEntity()); // TODO: might fail on many2many
            }

            if (target.getIdentifier()==null) { // non-persistent instance
                session.persist(o);
            }

            if (!reversed) { // prevent duplicated rels
                session.addPendingInsert(new RelationshipPendingInsert(owner, relType, target, session.getNativeInterface()));
            }

        }
        return isNew;
    }

    @Override
    public boolean remove(Object o) {
        boolean isDeleted = delegate.remove(o);
        if (isDeleted && (!reversed)) {
            session.addPendingInsert(new RelationshipPendingDelete(owner, relType,
                    new EntityAccess(association.getAssociatedEntity(), o),
                    session.getNativeInterface()));
        }
        return isDeleted;
    }

    @Override
    public boolean addAll(Collection collection) {
        return addAll((Iterable)collection);
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

    public boolean addAll(Iterable objects) {
        boolean hasChanged = false;
        for (Object object: objects) {
            hasChanged |= add(object);
        }
        return hasChanged;
    }
}
