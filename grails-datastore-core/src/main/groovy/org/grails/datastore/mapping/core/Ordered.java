package org.grails.datastore.mapping.core;

/**
 * Adds a getOrder() method to any class that implements it.
 *
 * Can be used in combination with {@link org.grails.datastore.mapping.core.order.OrderedComparator} to sort objects
 *
 * @author Graeme Rocher
 * @since 6.1
 */
public interface Ordered {

    /**
     * The order of this object
     */
    default int getOrder() {
        return 0;
    }
}