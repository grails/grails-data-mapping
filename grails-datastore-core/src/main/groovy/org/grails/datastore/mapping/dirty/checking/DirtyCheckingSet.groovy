package org.grails.datastore.mapping.dirty.checking

import groovy.transform.CompileStatic

/**
 * Wrapper list to dirty check a list and mark a parent as dirty
 *
 * @author Graeme Rocher
 * @since 4.1
 */
@CompileStatic
class DirtyCheckingSet extends DirtyCheckingCollection implements Set {

    @Delegate Set target

    DirtyCheckingSet(Set target, DirtyCheckable parent, String property) {
        super(target, parent, property)
        this.target = target
    }
}
