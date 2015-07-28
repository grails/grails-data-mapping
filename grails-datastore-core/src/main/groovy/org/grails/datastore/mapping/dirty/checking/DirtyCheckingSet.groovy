package org.grails.datastore.mapping.dirty.checking

/**
 * Wrapper list to dirty check a list and mark a parent as dirty
 *
 * @author Graeme Rocher
 * @since 4.1
 */
class DirtyCheckingSet extends DirtyCheckingCollection implements Set {

    @Delegate List target

    DirtyCheckingSet(Set target, DirtyCheckable parent, String property) {
        super(target, parent, property)
        this.target = target
    }
}
