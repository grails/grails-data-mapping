package org.grails.datastore.mapping.query.event;

import org.grails.datastore.mapping.query.Query;

/**
 * Event fired immediately before a query is executed.
 */
public class PreQueryEvent extends AbstractQueryEvent {
    public PreQueryEvent(Query query) {
        super(query);
    }

    public PreQueryEvent(Object source, Query query) {
        super(source, query);
    }

    /**
     * @return The type of event.
     */
    @Override
    public QueryEventType getEventType() {
        return QueryEventType.PreExecution;
    }
}
