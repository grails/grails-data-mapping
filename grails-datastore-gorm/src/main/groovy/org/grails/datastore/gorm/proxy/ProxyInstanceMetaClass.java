package org.grails.datastore.gorm.proxy;

import groovy.lang.DelegatingMetaClass;
import groovy.lang.MetaClass;
import org.grails.datastore.mapping.core.Session;
import org.springframework.dao.DataIntegrityViolationException;

import java.io.Serializable;

/**
 * Per-instance metaclass to use for proxied GORM domain objects. It auto-retrieves the associated entity when
 * fields, properties or methods are called (other than those supported by the proxy). The methods and properties
 * supported by the proxy are:
 * <ul>
 *     <li>id/getId() - no resolve performed</li>
 *     <li>initialized/isInitialized() - no resolve performed</li>
 *     <li>class/getClass() - no resolve performed</li>
 *     <li>metaClass/getMetaClass() - no resolve performed</li>
 *     <li>domainClass/getDomainClass() - no resolve performed</li>
 *     <li>target/getTarget() - resolve performed</li>
 *     <li>initialize() - resolve performed</li>
 * </ul>
 * @author Tom Widmer
 */
public class ProxyInstanceMetaClass extends DelegatingMetaClass {
    /**
     * Session to fetch from, if we need to.
     */
    private Session session;
    /**
     * The loaded instance we're proxying, or null if it hasn't been loaded.
     */
    private Object target;
    /**
     * The key of the object.
     */
    private Serializable key;

    public ProxyInstanceMetaClass(MetaClass delegate, Session session, Serializable key) {
        super(delegate);
        this.session = session;
        this.key = key;
    }

    /**
     * Load the target from the DB.
     * @return target.
     */
    private Object resolveTarget() {
        if (target == null) {
            target = session.retrieve(getTheClass(), key);
            if (target == null) {
                throw new DataIntegrityViolationException(
                        "Error loading association [" + key + "] of type [" + getTheClass() +
                                "]. Associated instance no longer exists.");
            }
        }

        return target;
    }

    /**
     * Handle method calls on our proxy.
     * @param o The proxy.
     * @param methodName
     * @param arguments
     * @return
     */
    @Override
    public Object invokeMethod(Object o, String methodName, Object[] arguments) {
        if (methodName.equals("isProxy")) {
            return true;
        } else if (methodName.equals("getId")) {
            return key;
        } else if (methodName.equals("isInitialized")) {
            return target != null;
        } else if (methodName.equals("getTarget") || methodName.equals("initialize")) {
            return resolveTarget();
        } else if (methodName.equals("getMetaClass")) {
            return this;
        } else if (methodName.equals("getClass") || methodName.equals("getDomainClass")) {
            // return correct class only if loaded, otherwise hope for the best
            return delegate.invokeMethod(target != null ? target : o, methodName, arguments);
        } else {
            return delegate.invokeMethod(resolveTarget(), methodName, arguments);
        }
    }

    @Override
    public Object getProperty(Object object, String property) {
        if (property.equals("id")) {
            return key;
        } else if (property.equals("proxy")) {
            return true;
        } else if (property.equals("initialized")) {
            return target != null;
        } else if (property.equals("target")) {
            return resolveTarget();
        } else if (property.equals("metaClass")) {
            return this;
        } else if (property.equals("class") || property.equals("domainClass")) {
            // return correct class only if loaded, otherwise hope for the best
            return delegate.getProperty(target != null ? target : object, property);
        } else {
            return delegate.getProperty(resolveTarget(), property);
        }
    }

    @Override
    public void setProperty(Object object, String property, Object newValue) {
        delegate.setProperty(resolveTarget(), property, newValue);
    }

    @Override
    public Object getAttribute(Object object, String attribute) {
        if (attribute.equals("id")) {
            return key;
        } else if (attribute.equals("initialized")) {
            return target != null;
        } else if (attribute.equals("target")) {
            return resolveTarget();
        } else {
            return delegate.getAttribute(resolveTarget(), attribute);
        }
    }

    @Override
    public void setAttribute(Object object, String attribute, Object newValue) {
        delegate.setAttribute(resolveTarget(), attribute, newValue);
    }
}
