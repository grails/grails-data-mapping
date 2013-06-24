package org.codehaus.groovy.grails.orm.hibernate;

import java.io.Serializable;
import java.util.Collection;

import org.hibernate.LockMode;

public interface IHibernateTemplate {

    Serializable save(Object o);

    void refresh(Object o);

    void lock(Object o, LockMode lockMode);

    void flush();

    void clear();

    void evict(Object o);

    boolean contains(Object o);

    void setFlushMode(int mode);

    int getFlushMode();

    void deleteAll(Collection<?> list);

    <T> T get(Class<T> type, Serializable key);

    <T> T get(Class<T> type, Serializable key, LockMode mode);

    <T> T load(Class<T> type, Serializable key);

    void delete(Object o);
}
