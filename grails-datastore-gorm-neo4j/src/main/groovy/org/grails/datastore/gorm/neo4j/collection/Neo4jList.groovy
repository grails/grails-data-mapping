/*
 * Copyright 2015 original authors
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
package org.grails.datastore.gorm.neo4j.collection

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.neo4j.Neo4jSession
import org.grails.datastore.gorm.neo4j.RelationshipUtils
import org.grails.datastore.gorm.neo4j.engine.RelationshipPendingDelete
import org.grails.datastore.gorm.neo4j.engine.RelationshipPendingInsert
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.grails.datastore.mapping.dirty.checking.DirtyCheckingList
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.ManyToMany


/**
 *
 * A {@link org.grails.datastore.mapping.dirty.checking.DirtyCheckingList} for Neo4j
 *
 * @author Graeme Rocher
 * @since 5.0
 */

@CompileStatic
class Neo4jList extends DirtyCheckingList {
    private final boolean reversed
    final transient Association association
    final transient Neo4jSession session
    private final String relType
    private final EntityAccess owner

    Neo4jList(EntityAccess parentAccess, Association association, List delegate, Neo4jSession session) {
        super(delegate, (DirtyCheckable)parentAccess.getEntity(), association.getName())
        this.association = association
        this.session = session
        this.owner = parentAccess
        reversed = RelationshipUtils.useReversedMappingFor(association)
        relType = RelationshipUtils.relationshipTypeUsedFor(association)
    }


    @Override
    boolean add(Object o) {

        def added = super.add(o)
        if(added) {
            adoptGraphUponAdd(o)
        }
        return added
    }

    @Override
    boolean addAll(Collection c) {
        def added = super.addAll(c)
        if(added) {
            for( o in c ) {
                adoptGraphUponAdd(o)
            }
        }

        return added
    }

    @Override
    boolean removeAll(Collection c) {
        for(o in c) {
            adoptGraphUponRemove(o)
        }
        return super.removeAll(c)
    }

    @Override
    boolean remove(Object o) {
        adoptGraphUponRemove(o)
        return super.remove(o)
    }

    @Override
    boolean retainAll(Collection c) {
        return super.retainAll(c)
    }

    @Override
    Object[] toArray(Object[] a) {
        return super.toArray(a)
    }

    @Override
    boolean containsAll(Collection c) {
        return super.containsAll(c)
    }

    protected void adoptGraphUponRemove(Object o) {
        if (session.getMappingContext().getProxyFactory().isProxy(o)) {
            return;
        }
        if (!reversed) {
            session.addPendingInsert(new RelationshipPendingDelete(owner, relType,
                    session.createEntityAccess(association.getAssociatedEntity(), o),
                    session.getNativeInterface()));
        }
    }

    protected void adoptGraphUponAdd(Object t) {
        def proxyFactory = session.getMappingContext().getProxyFactory()
        if (proxyFactory.isProxy(t)) {
            if ( !proxyFactory.isInitialized(t) ) return
            if ( !association.associatedEntity.isInstance(t) ) return
        }
        EntityAccess target = session.createEntityAccess(association.getAssociatedEntity(), t)
        if (association.isBidirectional()) {
            if (association instanceof ManyToMany) {
                Collection coll = (Collection) target.getProperty(association.getReferencedPropertyName());
                coll.add(parent);
            } else {
                target.setProperty(association.getReferencedPropertyName(), parent);
            }
        }

        if (target.getIdentifier() == null) { // non-persistent instance
            session.persist(t);
        }

        if (!reversed) { // prevent duplicated rels
            session.addPendingInsert(new RelationshipPendingInsert(owner, relType, target, session.getNativeInterface()));
        }
    }
}
