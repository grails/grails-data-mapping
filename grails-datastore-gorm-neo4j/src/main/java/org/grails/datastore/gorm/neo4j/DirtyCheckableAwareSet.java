package org.grails.datastore.gorm.neo4j;

import org.grails.datastore.mapping.engine.EntityAccess;
import org.grails.datastore.mapping.model.types.Association;

import java.util.Set;

/**
 * A {@link java.util.Set} implementation setting it's owner's dirty status upon
 * modification of the set. Internally {@link org.grails.datastore.mapping.dirty.checking.DirtyCheckable}
 * is used which is applied to all domain classes using an AST transformation
 */
public class DirtyCheckableAwareSet<T> extends DirtyCheckableAwareCollection<T> implements Set<T> {

    private final Set<T> delegate;

    public DirtyCheckableAwareSet(EntityAccess owner, Association association, Set<T> delegate, Neo4jSession session) {
        super(owner, association, delegate, session);
        this.delegate = delegate;
    }

}
