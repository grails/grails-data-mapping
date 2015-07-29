package org.grails.datastore.mapping.dirty.checking

import groovy.transform.CompileStatic

/**
 * A map that can be dirty checked
 *
 * @author Graeme Rocher
 * @since 4.1
 */
@CompileStatic
class DirtyCheckingMap implements Map, DirtyCheckableCollection {

    final @Delegate Map target
    final DirtyCheckable parent
    final String property

    DirtyCheckingMap(Map target, DirtyCheckable parent, String property) {
        this.target = target
        this.parent = parent
        this.property = property
    }

    boolean hasChanged() {
        parent.hasChanged(property)
    }

    @Override
    Object put(Object key, Object value) {
        parent.markDirty(property)
        target.put(key, value)
    }

    @Override
    Object remove(Object key) {
        parent.markDirty(property)
        target.remove key
    }

    @Override
    void putAll(Map m) {
        parent.markDirty(property)
        target.putAll(m)
    }

    @Override
    void clear() {
        parent.markDirty(property)
        target.clear()
    }
}
