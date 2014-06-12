package org.grails.datastore.mapping.core;

import org.springframework.context.ApplicationEvent;

/**
 * Event fired when a session is created. This can be used to customize the session.
 */
public class SessionCreationEvent extends ApplicationEvent {

    private final Session session;

    public SessionCreationEvent(Session session) {
        super(session.getDatastore());
        this.session = session;
    }

    /**
     * @return The session that has just been created.
     */
    public Session getSession() {
        return session;
    }
}
