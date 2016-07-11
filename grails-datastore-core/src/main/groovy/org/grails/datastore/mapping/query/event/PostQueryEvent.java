package org.grails.datastore.mapping.query.event;

import org.grails.datastore.mapping.query.Query;

import java.util.List;

/**
 * Query fired after a query has run.
 */
public class PostQueryEvent extends AbstractQueryEvent {
    /**
     * The results of the query.
     */
    private List results;

    public PostQueryEvent(Query query, List results) {
        super(query);
        this.results = results;
    }

    public PostQueryEvent(Object source, Query query, List results) {
        super(source, query);
        this.results = results;
    }

    /**
     * @return The results of the query. Note that this list is usually non-modifiable.
     */
    public List getResults() {
        return results;
    }

    /**
     * Reset the list of results to a new list. This allows an event handler to modify the results of a query.
     * @param results The replacement results.
     */
    public void setResults(List results) {
        if (results == null) {
            throw new IllegalArgumentException("results must be non-null");
        }
        this.results = results;
    }

    /**
     * @return The type of event.
     */
    @Override
    public QueryEventType getEventType() {
        return QueryEventType.PostExecution;
    }
}
