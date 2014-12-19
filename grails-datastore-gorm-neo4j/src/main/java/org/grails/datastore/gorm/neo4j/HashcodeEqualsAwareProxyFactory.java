package org.grails.datastore.gorm.neo4j;

import javassist.util.proxy.MethodHandler;
import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.proxy.JavassistProxyFactory;
import org.grails.datastore.mapping.proxy.SessionEntityProxyMethodHandler;

import java.io.Serializable;

/**
 * extends {@link org.grails.datastore.mapping.proxy.JavassistProxyFactory} to capture method calls `hashCode` and `equals`
 * without expanding the proxy
 */
public class HashcodeEqualsAwareProxyFactory extends JavassistProxyFactory {

    @Override
    protected MethodHandler createMethodHandler(Session session, Class cls, Class proxyClass, final Serializable id) {
        return new SessionEntityProxyMethodHandler(proxyClass, session, cls, id) {
            protected Object invokeEntityProxyMethods(Object self, String methodName, Object[] args) {
                if (methodName.equals("hashCode")) {
                    return id.hashCode();
                } else {
                    return super.invokeEntityProxyMethods(self, methodName, args);
                }
            }
        };
    }

}
