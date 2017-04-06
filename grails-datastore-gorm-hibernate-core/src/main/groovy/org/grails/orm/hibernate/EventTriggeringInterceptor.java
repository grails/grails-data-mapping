package org.grails.orm.hibernate;

import org.grails.orm.hibernate.event.listener.HibernateEventListener;

/**
 * <p>Invokes closure events on domain entities such as beforeInsert, beforeUpdate and beforeDelete.
 *
 * @author Graeme Rocher
 * @author Lari Hotari
 * @author Burt Beckwith
 * @since 2.0
 * @deprecated Renamed to avoid confusion with {@link org.grails.orm.hibernate.support.ClosureEventTriggeringInterceptor}. Replaced by {@link HibernateEventListener}
 */
@Deprecated
public class EventTriggeringInterceptor extends HibernateEventListener {
    public EventTriggeringInterceptor(AbstractHibernateDatastore datastore) {
        super(datastore);
    }
}
