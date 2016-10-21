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
        cast(entity).listDirtyPropertyNames().size() > 0
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
