package org.grails.datastore.mapping.dirty.checking

import groovy.transform.CompileStatic

/**
 * Created by graemerocher on 27/07/15.
 */
@CompileStatic
class DirtyCheckingMap implements Map {

    final @Delegate Map target
    final DirtyCheckable parent
    final String property

    DirtyCheckingMap(Map target, DirtyCheckable parent, String property) {
        this.target = target
        this.parent = parent
        this.property = property
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
