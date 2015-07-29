package org.grails.datastore.mapping.dirty.checking

import groovy.transform.CompileStatic

/**
 * Collection capable of marking the parent entity as dirty when it is modified
 *
 * @author Graeme Rocher
 * @since 4.1
 */
@CompileStatic
class DirtyCheckingCollection implements Collection, DirtyCheckableCollection {

    final @Delegate Collection target
    final DirtyCheckable parent
    final String property
    final boolean hasDirtyCheckables = false
    final int originalSize

    DirtyCheckingCollection(Collection target, DirtyCheckable parent, String property) {
        this.target = target
        this.originalSize = target.size()
        this.parent = parent
        this.property = property
        this.hasDirtyCheckables = target.any { it instanceof DirtyCheckable }
    }

    @Override
    boolean hasGrown() {
        return size() > originalSize
    }

    @Override
    boolean hasShrunk() {
        return size() < originalSize
    }

    @Override
    boolean hasChangedSize() {
        return size() != originalSize
    }

    boolean hasChanged() {
        parent.hasChanged(property) || (hasDirtyCheckables && target.any { ((DirtyCheckable)it).hasChanged() } )
    }

    @Override
    boolean add(Object o) {
        parent.markDirty(property)
        target.add o
    }

    @Override
    boolean addAll(Collection c) {
        parent.markDirty(property)
        target.addAll(c)
    }

    @Override
    boolean removeAll(Collection c) {
        parent.markDirty(property)
        target.removeAll(c)
    }

    @Override
    void clear() {
        parent.markDirty(property)
        target.clear()
    }

    @Override
    boolean remove(Object o) {
        parent.markDirty(property)
        target.remove(o)
    }


}

