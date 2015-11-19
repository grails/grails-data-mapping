package org.grails.datastore.mapping.core;

import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * Creates a InheritableThreadLocal with an intial value of a Map.
 *
 * @author Graeme Rocher
 *
 * @deprecated Do not use
 */
@Deprecated
public class SoftThreadLocalMap extends InheritableThreadLocal<ConcurrentReferenceHashMap> {

    /**
     * Creates an initial value of a Map.
     */
    @Override
    protected ConcurrentReferenceHashMap initialValue() {
        return new ConcurrentReferenceHashMap();
    }
}
