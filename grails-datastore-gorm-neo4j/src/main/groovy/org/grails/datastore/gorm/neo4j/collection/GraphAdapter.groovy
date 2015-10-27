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
import groovy.transform.PackageScope
import org.grails.datastore.gorm.neo4j.Neo4jSession
import org.grails.datastore.gorm.neo4j.RelationshipUtils
import org.grails.datastore.gorm.neo4j.engine.RelationshipPendingDelete
import org.grails.datastore.gorm.neo4j.engine.RelationshipPendingInsert
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.ManyToMany



/**
 * Helps to Adapt a collection to the Neo4j graph
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
@PackageScope
class GraphAdapter {

    final EntityAccess parentAccess
    final Neo4jSession session
    final Association association
    final boolean reversed
    final String relType
    final Class childType

    GraphAdapter(Neo4jSession session, EntityAccess parentAccess, Association association) {
        this.association = association
        this.session = session
        this.parentAccess = parentAccess
        reversed = RelationshipUtils.useReversedMappingFor(association)
        relType = RelationshipUtils.relationshipTypeUsedFor(association)
        childType = association.associatedEntity.javaClass
    }

    void adaptGraphUponRemove(Object o, boolean currentlyInitializing = false) {
        Neo4jSession session = (Neo4jSession)this.session
        if (session.getMappingContext().getProxyFactory().isProxy(o)) {
            return;
        }
        if (!reversed && !currentlyInitializing) {
            session.addPostFlushOperation(new RelationshipPendingDelete(parentAccess, relType,
                    session.createEntityAccess(association.getAssociatedEntity(), o),
                    session.getNativeInterface()));
        }
    }

    void adaptGraphUponAdd(Object t, boolean currentlyInitializing = false) {
        Neo4jSession session = (Neo4jSession)this.session
        def proxyFactory = session.getMappingContext().getProxyFactory()
        if (proxyFactory.isProxy(t)) {
            if ( !proxyFactory.isInitialized(t) ) return
            if ( !childType.isInstance(t) ) return
        }
        EntityAccess target = session.createEntityAccess(association.getAssociatedEntity(), t)
        if (association.isBidirectional() && !currentlyInitializing) {
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

        if (!reversed && !currentlyInitializing) { // prevent duplicated rels
            session.addPostFlushOperation(new RelationshipPendingInsert(parentAccess, relType, target, session.getNativeInterface()));
        }
    }
}
