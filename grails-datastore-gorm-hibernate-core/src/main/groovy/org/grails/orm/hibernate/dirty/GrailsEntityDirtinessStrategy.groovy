/*
 * Copyright 2004-2005 the original author or authors.
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
package org.grails.orm.hibernate.dirty

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.hibernate.CustomEntityDirtinessStrategy
import org.hibernate.Session
import org.hibernate.persister.entity.EntityPersister

/**
 * A class to customize Hibernate dirtiness based on Grails {@link DirtyCheckable} interface
 *
 * @author James Kleeh
 * @author Graeme Rocher
 *
 * @since 6.0.3
 */
@CompileStatic
class GrailsEntityDirtinessStrategy implements CustomEntityDirtinessStrategy {

    @Override
    boolean canDirtyCheck(Object entity, EntityPersister persister, Session session) {
        return entity instanceof DirtyCheckable
    }

    @Override
    boolean isDirty(Object entity, EntityPersister persister, Session session) {
        !session.contains(entity) || cast(entity).hasChanged()
    }

    @Override
    void resetDirty(Object entity, EntityPersister persister, Session session) {
        cast(entity).trackChanges()
    }

    @Override
    void findDirty(Object entity, EntityPersister persister, Session session, CustomEntityDirtinessStrategy.DirtyCheckContext dirtyCheckContext) {
        dirtyCheckContext.doDirtyChecking(
            new CustomEntityDirtinessStrategy.AttributeChecker() {
                @Override
                boolean isDirty(CustomEntityDirtinessStrategy.AttributeInformation attributeInformation) {
                    String propertyName = attributeInformation.name
                    !session.contains(entity) || cast(entity).hasChanged(propertyName)
                }
            }
        );
    }

    private DirtyCheckable cast(Object entity) {
        return DirtyCheckable.class.cast(entity);
    }
}
