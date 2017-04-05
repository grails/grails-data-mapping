package org.grails.datastore.mapping.engine

import groovy.transform.CompileStatic

/**
 * Tracks modifications to the entity access, this allows synchronization of state for Hibernate for example
 *
 * @author Graeme Rocher
 * @since 6.0.9
 */
@CompileStatic
class ModificationTrackingEntityAccess implements EntityAccess {

    /**
     * The target entity access
     */
    final @Delegate EntityAccess target
    /**
     * The modified properties
     */
    final Map<String,Object> modifiedProperties = [:]

    ModificationTrackingEntityAccess(EntityAccess target) {
        this.target = target
    }

    @Override
    void setPropertyNoConversion(String name, Object value) {
        modifiedProperties.put(name, value)
        target.setPropertyNoConversion(name, value)
    }

    /**
     * Sets a property value
     * @param name The name of the property
     * @param value The value of the property
     */
    @Override
    void setProperty(String name, Object value) {
        modifiedProperties.put(name, value)
        target.setProperty(name, value)
    }
}
