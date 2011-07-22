package org.grails.datastore.mapping.core;

import org.apache.commons.collections.map.ReferenceMap;

/**
 * Creates a InheritableThreadLocal with an intial value of a Map.
 *
 * @author Graeme Rocher
 */
public class SoftThreadLocalMap extends InheritableThreadLocal<ReferenceMap> {

    /**
     * Creates an initial value of a Map.
     */
    @Override
    protected ReferenceMap initialValue() {
        return new ReferenceMap(ReferenceMap.SOFT, ReferenceMap.SOFT);
    }
}
