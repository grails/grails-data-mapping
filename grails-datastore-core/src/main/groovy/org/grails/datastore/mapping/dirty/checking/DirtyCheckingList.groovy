package org.grails.datastore.mapping.dirty.checking

import groovy.transform.CompileStatic

/**
 * Wrapper list to dirty check a list and mark a parent as dirty
 *
 * @author Graeme Rocher
 * @since 4.1
 */
@CompileStatic
class DirtyCheckingList extends DirtyCheckingCollection implements List {

    @Delegate List target

    DirtyCheckingList(List target, DirtyCheckable parent, String property) {
        super(target, parent, property)
        this.target = target
    }

    @Override
    boolean addAll(int index, Collection c) {
        parent.markDirty(property)
        target.addAll(index, c)
    }

    @Override
    Object set(int index, Object element) {
        parent.markDirty(property)
        target.set(index, element)
    }

    @Override
    void add(int index, Object element) {
        parent.markDirty(property)
        target.add(index, element)
    }


    @Override
    Object remove(int index) {
        parent.markDirty(property)
        target.remove((int)index)
    }
}
