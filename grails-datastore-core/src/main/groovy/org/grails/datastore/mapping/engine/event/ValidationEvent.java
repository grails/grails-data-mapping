package org.grails.datastore.mapping.engine.event;

import java.util.List;

import org.grails.datastore.mapping.core.Datastore;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.grails.datastore.mapping.model.PersistentEntity;

/**
 * @author Burt Beckwith
 */
public class ValidationEvent extends AbstractPersistenceEvent {

    private static final long serialVersionUID = 1;

    private List<?> validatedFields;

    public ValidationEvent(final Datastore source, final PersistentEntity entity,
            final EntityAccess entityAccess) {
        super(source, entity, entityAccess);
    }

    public ValidationEvent(final Datastore source, final Object entity) {
        super(source, entity);
    }

    public ValidationEvent(Object source, PersistentEntity entity, EntityAccess entityAccess) {
        super(source, entity, entityAccess);
    }



    public List<?> getValidatedFields() {
        return validatedFields;
    }

    public void setValidatedFields(final List<?> fields) {
        validatedFields = fields;
    }

    @Override
    public EventType getEventType() {
        return EventType.Validation;
    }
}
