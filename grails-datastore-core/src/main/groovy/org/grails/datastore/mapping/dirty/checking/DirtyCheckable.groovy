package org.grails.datastore.mapping.dirty.checking

/**
 * Interface to classes that are able to track changes to their internal state.
 *
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public interface DirtyCheckable {

    /**
     * Indicates that the instance should start tacking changes
     */
    void trackChanges()

    /**
     * @return True if the instance has any changes
     */
    boolean hasChanged()

    /**
     * @param propertyName The name of the property
     * @return True if the given property has any changes
     */
    boolean hasChanged(String propertyName)

    /**
     * Marks this instance as dirty
     */
    void markDirty()

    /**
     * Marks the given property name as dirty
     * @param propertyName The property name
     */
    void markDirty(String propertyName)

}