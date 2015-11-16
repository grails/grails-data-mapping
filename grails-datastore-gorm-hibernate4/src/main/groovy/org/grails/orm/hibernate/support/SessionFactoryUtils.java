package org.grails.orm.hibernate.support;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

/**
 * Helper methods for Hibernate 4
 */
public class SessionFactoryUtils extends org.springframework.orm.hibernate4.SessionFactoryUtils {

    public static Session openSession(SessionFactory sessionFactory) {
        return sessionFactory.openSession();
    }
}
