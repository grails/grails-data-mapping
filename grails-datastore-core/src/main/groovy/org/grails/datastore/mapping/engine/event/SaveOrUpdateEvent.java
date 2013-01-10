package org.grails.datastore.mapping.engine.event;

import org.grails.datastore.mapping.core.Datastore;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.grails.datastore.mapping.model.PersistentEntity;

/**
 * @author Burt Beckwith
 */
public class SaveOrUpdateEvent extends AbstractPersistenceEvent {

    private static final long serialVersionUID = 1;

    public SaveOrUpdateEvent(final Datastore source, final PersistentEntity entity,
            final EntityAccess entityAccess) {
        super(source, entity, entityAccess);
    }

    public SaveOrUpdateEvent(final Datastore source, final Object entity) {
        super(source, entity);
    }

    @Override
    public EventType getEventType() {
        return EventType.SaveOrUpdate;
    }
}
