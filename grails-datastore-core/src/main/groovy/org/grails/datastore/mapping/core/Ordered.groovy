package org.grails.datastore.mapping.core

import groovy.transform.CompileStatic

/**
 * A trait that adds an order property to any class that implements it.
 *
 * Can be used in combination with {@link org.grails.datastore.mapping.core.order.OrderedComparator} to sort objects
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
trait Ordered {

    /**
     * The order of this object
     */
    int order = 0
}