
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
import org.grails.datastore.mapping.collection.PersistentList
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.grails.datastore.mapping.engine.AssociationIndexer
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.ManyToMany


/**
 * Neo4j version of the {@link PersistentList} class
 *
 * @author Graeme Rocher
 * @since 5.0
 */
@CompileStatic
class Neo4jPersistentList extends PersistentList {

    protected final boolean reversed
    protected final String relType
    protected final EntityAccess parentAccess
    protected final Association association

    Neo4jPersistentList(Session session, List collection, EntityAccess parentAccess, Association association) {
        super(association.associatedEntity.javaClass, session, collection)
        this.parentAccess = parentAccess
        this.association = association
        reversed = RelationshipUtils.useReversedMappingFor(association)
        relType = RelationshipUtils.relationshipTypeUsedFor(association)
    }

    Neo4jPersistentList(Collection keys, Session session, EntityAccess parentAccess, Association association) {
        super(keys, association.associatedEntity.javaClass, session)
        this.parentAccess = parentAccess
        this.association = association
        reversed = RelationshipUtils.useReversedMappingFor(association)
        relType = RelationshipUtils.relationshipTypeUsedFor(association)

    }

    Neo4jPersistentList(Serializable associationKey, Session session, AssociationIndexer indexer, EntityAccess parentAccess, Association association) {
        super(associationKey, session, indexer)
        this.parentAccess = parentAccess
        this.association = association
        reversed = RelationshipUtils.useReversedMappingFor(association)
        relType = RelationshipUtils.relationshipTypeUsedFor(association)
    }


    @Override
    boolean addAll(Collection c) {
        def added = super.addAll(c)
        if(added) {
            for( o in c ) {
                adaptGraphUponAdd(o)
            }
        }

        return added
    }

    @Override
    boolean removeAll(Collection c) {
        for(o in c) {
            adaptGraphUponRemove(o)
        }
        return super.removeAll(c)
    }

    @Override
    boolean remove(Object o) {
        adaptGraphUponRemove(o)
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

    protected void adaptGraphUponRemove(Object o) {
        Neo4jSession session = (Neo4jSession)this.session
        if (session.getMappingContext().getProxyFactory().isProxy(o)) {
            return;
        }
        if (!reversed) {
            session.addPendingInsert(new RelationshipPendingDelete(parentAccess, relType,
                    session.createEntityAccess(association.getAssociatedEntity(), o),
                    session.getNativeInterface()));
        }
    }

    protected void adaptGraphUponAdd(Object t) {
        Neo4jSession session = (Neo4jSession)this.session
        def proxyFactory = session.getMappingContext().getProxyFactory()
        if (proxyFactory.isProxy(t)) {
            if ( !proxyFactory.isInitialized(t) ) return
            if ( !childType.isInstance(t) ) return
        }
        EntityAccess target = session.createEntityAccess(association.getAssociatedEntity(), t)
        if (association.isBidirectional()) {
            if (association instanceof ManyToMany) {
                Collection coll = (Collection) target.getProperty(association.getReferencedPropertyName());
                coll.add(parentAccess.entity);
            } else {
                target.setProperty(association.getReferencedPropertyName(), parentAccess.entity);
            }
        }

        if (target.getIdentifier() == null) { // non-persistent instance
            session.persist(t);
        }

        if (!reversed) { // prevent duplicated rels
            session.addPendingInsert(new RelationshipPendingInsert(parentAccess, relType, target, session.getNativeInterface()));
        }
    }

    @Override
    void markDirty() {
        ((DirtyCheckable)parentAccess.entity).markDirty(association.getName())
        super.markDirty()
    }
}
