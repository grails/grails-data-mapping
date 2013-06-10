package org.grails.datastore.mapping.query.event;

import org.grails.datastore.mapping.query.Query;
import org.springframework.context.ApplicationEvent;

/**
 * Base class for query events.
 */
public abstract class AbstractQueryEvent extends ApplicationEvent {
    /**
     * The query.
     */
    protected Query query;

    public AbstractQueryEvent(Query query) {
        super(query.getSession().getDatastore());
        this.query = query;
    }

    /**
     * @return The type of event.
     */
    public abstract QueryEventType getEventType();

    /**
     * Get the query from the event.
     * @return The query.
     */
    public Query getQuery() {
        return query;
    }
}
