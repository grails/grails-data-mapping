/* Copyright (C) 2013 original authors
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
package org.grails.datastore.mapping.dirty.checking

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.collection.PersistentCollection
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.ToOne
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher

/**
 * Support methods for dirty checking
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@CompileStatic
class DirtyCheckingSupport {

    /**
     * Checks whether associations are dirty
     *
     * @param session The session
     * @param entity The entity
     * @param instance The instance
     * @return True if they are
     */
    static boolean areAssociationsDirty(Session session, PersistentEntity entity, Object instance) {
        if(!instance) return false

        final proxyFactory = session.mappingContext.proxyFactory
        final cpf = ClassPropertyFetcher.forClass(instance.getClass())

        final associations = entity.associations
        for(Association a in associations) {
            final isOwner = a.isOwningSide() || (a.bidirectional && !a.inverseSide?.owningSide)
            if(isOwner) {
                if(a instanceof ToOne) {
                    final value = cpf.getPropertyValue(instance, a.name)
                    if(proxyFactory.isInitialized(value)) {
                        if(value instanceof DirtyCheckable) {
                            DirtyCheckable dirtyCheckable = (DirtyCheckable) value
                            if(dirtyCheckable.hasChanged()) {
                                return true
                            }
                        }
                    }
                }
                else {
                    final value = cpf.getPropertyValue(instance, a.name)
                    if(value instanceof PersistentCollection) {
                        PersistentCollection coll = (PersistentCollection)value
                        if(coll.isInitialized()) {
                            if(coll.isDirty()) return true
                        }
                    }
                }

            }
        }
        return false
    }
}
