package org.grails.datastore.mapping.reflect;

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple class that takes method invocations and property setters and populates
 * the arguments of these into the supplied map ignoring null values.
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class ClosureToMapPopulator extends GroovyObjectSupport {

    private Map map;

    public ClosureToMapPopulator(Map theMap) {
        map = theMap;
    }

    public ClosureToMapPopulator() {
        this(new HashMap());
    }

    public Map populate(Closure callable) {
        callable.setDelegate(this);
        callable.setResolveStrategy(Closure.DELEGATE_FIRST);
        callable.call();
        return map;
    }

    @Override
    public void setProperty(String name, Object o) {
        if (o != null) {
            map.put(name, o);
        }
    }

    @Override
    public Object invokeMethod(String name, Object o) {
        if (o != null) {
            if (o.getClass().isArray()) {
                Object[] args = (Object[])o;
                if (args.length == 1) {
                    map.put(name, args[0]);
                }
                else {
                    map.put(name, Arrays.asList(args));
                }
            }
            else {
                map.put(name,o);
            }
        }
        return null;
    }
}
