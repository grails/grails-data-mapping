package org.grails.datastore.gorm.neo4j;

import javassist.util.proxy.MethodHandler;
import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.proxy.EntityProxy;
import org.grails.datastore.mapping.proxy.JavassistProxyFactory;
import org.grails.datastore.mapping.proxy.SessionEntityProxyMethodHandler;
import org.springframework.util.ReflectionUtils;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * extends {@link org.grails.datastore.mapping.proxy.JavassistProxyFactory} to capture method calls `hashCode` and `equals`
 * without expanding the proxy
 */
public class HashcodeEqualsAwareProxyFactory extends JavassistProxyFactory {

    @Override
    protected MethodHandler createMethodHandler(Session session, final Class cls, final Class proxyClass, final Serializable id) {
        return new SessionEntityProxyMethodHandler(proxyClass, session, cls, id) {
            final Method getIdMethod = ReflectionUtils.findMethod(cls, "getId");

            protected Object invokeEntityProxyMethods(Object self, String methodName, Object[] args) {
                if (methodName.equals("hashCode")) {
                    return id.hashCode();
                } else if (methodName.equals("equals") && args.length==1) {
                    Object other = args[0];
                    if(other == null) {
                        return false;
                    }
                    if(other == self) {
                        return true;
                    }
                    if(other.getClass() == proxyClass && other instanceof EntityProxy && id.equals(((EntityProxy)other).getProxyKey())) {
                        return true;
                    }
                    if(other.getClass() == cls && id.equals(ReflectionUtils.invokeMethod(getIdMethod, other))) {
                        return true;
                    }
                    return false;
                } else {
                    return super.invokeEntityProxyMethods(self, methodName, args);
                }
            }
        };
    }

}
