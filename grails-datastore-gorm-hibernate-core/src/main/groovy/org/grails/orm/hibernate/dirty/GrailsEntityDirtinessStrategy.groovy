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

import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.hibernate.CustomEntityDirtinessStrategy
import org.hibernate.Session
import org.hibernate.persister.entity.EntityPersister

/**
 * A class to customize Hibernate dirtiness based on Grails {@link DirtyCheckable} interface
 *
 * @author James Kleeh
 * @since 6.0.3
 */
class GrailsEntityDirtinessStrategy implements CustomEntityDirtinessStrategy {

    @Override
    public boolean canDirtyCheck(Object entity, EntityPersister persister, Session session) {
        return entity instanceof DirtyCheckable
    }

    @Override
    public boolean isDirty(Object entity, EntityPersister persister, Session session) {
        !cast(entity).listDirtyPropertyNames().empty
    }

    @Override
    public void resetDirty(Object entity, EntityPersister persister, Session session) {
        cast(entity).trackChanges()
    }

    @Override
    public void findDirty(Object entity, EntityPersister persister, Session session, CustomEntityDirtinessStrategy.DirtyCheckContext dirtyCheckContext) {
        final DirtyCheckable dirtyAware = cast(entity)
        dirtyCheckContext.doDirtyChecking(
            new CustomEntityDirtinessStrategy.AttributeChecker() {
                @Override
                public boolean isDirty(CustomEntityDirtinessStrategy.AttributeInformation attributeInformation) {
                    String propertyName = attributeInformation.name
                    cast(entity).listDirtyPropertyNames().contains(propertyName)
                }
            }
        );
    }

    private DirtyCheckable cast(Object entity) {
        return DirtyCheckable.class.cast(entity);
    }
}
