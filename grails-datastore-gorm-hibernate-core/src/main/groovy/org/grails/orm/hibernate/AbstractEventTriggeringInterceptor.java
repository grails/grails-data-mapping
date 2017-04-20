package org.grails.orm.hibernate;

import org.grails.orm.hibernate.event.listener.AbstractHibernateEventListener;

/**
 * <p>Invokes closure events on domain entities such as beforeInsert, beforeUpdate and beforeDelete.
 *
 * @deprecated Renamed to avoid confusiong with {@link org.grails.orm.hibernate.support.ClosureEventTriggeringInterceptor}. Replaced by {@link AbstractHibernateEventListener}
 */
@Deprecated
public abstract class AbstractEventTriggeringInterceptor extends AbstractHibernateEventListener {
    protected AbstractEventTriggeringInterceptor(AbstractHibernateDatastore datastore) {
        super(datastore);
    }
}
