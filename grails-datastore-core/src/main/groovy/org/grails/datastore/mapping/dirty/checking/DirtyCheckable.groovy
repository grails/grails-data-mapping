package org.grails.datastore.mapping.dirty.checking

import groovy.transform.CompileStatic

/**
 * Interface to classes that are able to track changes to their internal state.
 *
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@CompileStatic
trait DirtyCheckable {

    /**
     * For internal use, do not use
     */
    @Deprecated
    public static final  String DIRTY_CLASS_MARKER = '$DIRTY_MARKER'

    private transient Map<String, Object> $changedProperties

    /**
     * Indicates that the instance should start tacking changes. Note that if the instance is dirty this will clear any previously tracked
     * changes
     */
    void trackChanges() {
        $changedProperties = new LinkedHashMap<String, Object>()
    }

    /**
     * @return True if the instance has any changes
     */
    boolean hasChanged() {
        $changedProperties == null || DirtyCheckingSupport.DIRTY_CLASS_MARKER.is($changedProperties) || !$changedProperties.isEmpty()
    }

    /**
     * @param propertyName The name of the property
     * @return True if the given property has any changes
     */
    boolean hasChanged(String propertyName) {
        hasChanged() && $changedProperties.containsKey(propertyName)
    }

    /**
     * Marks this instance as dirty
     */
    void markDirty() {
        if( $changedProperties != null) {
            $changedProperties = DirtyCheckingSupport.DIRTY_CLASS_MARKER
        }
    }

    /**
     * Marks the given property name as dirty
     * @param propertyName The property name
     */
    void markDirty(String propertyName) {
        if( $changedProperties != null && !$changedProperties.is(DirtyCheckingSupport.DIRTY_CLASS_MARKER) && !$changedProperties.containsKey(propertyName))  {
            $changedProperties.put propertyName, ((GroovyObject)this).getProperty(propertyName)
        }
    }

    /**
     * Marks the given property name as dirty
     * @param propertyName The property name
     * @param newValue The new value
     */
    void markDirty(String propertyName, newValue) {
        if( $changedProperties != null && !$changedProperties.is(DirtyCheckingSupport.DIRTY_CLASS_MARKER) && !$changedProperties.containsKey(propertyName))  {
            def oldValue = ((GroovyObject) this).getProperty(propertyName)
            if(newValue != oldValue) {
                $changedProperties.put propertyName, oldValue
            }
        }
    }

    /**
     * Marks the given property name as dirty
     * @param propertyName The property name
     * @param newValue The new value
     */
    void markDirty(String propertyName, newValue, oldValue) {
        if( $changedProperties != null && !$changedProperties.is(DirtyCheckingSupport.DIRTY_CLASS_MARKER) && !$changedProperties.containsKey(propertyName))  {
            if(newValue != oldValue) {
                $changedProperties.put propertyName, oldValue
            }
        }
    }

    /**
     * @return A list of the dirty property names
     */
    List<String> listDirtyPropertyNames() {
        if($changedProperties != null) {
            return Collections.unmodifiableList(
                $changedProperties.keySet().toList()
            )
        }
        return Collections.emptyList()
    }

    /**
     * Returns the original value of the property prior to when {@link #trackChanges()} was called
     *
     * @param propertyName The property name
     * @return The original value
     */
    Object getOriginalValue(String propertyName) {
        if($changedProperties != null && $changedProperties.containsKey(propertyName)) {
            return $changedProperties.get(propertyName)
        } else {
            return null
        }
    }
}