package org.grails.datastore.mapping.dirty.checking

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.proxy.EntityProxy

import javax.persistence.Transient

/**
 * Interface to classes that are able to track changes to their internal state.
 *
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@CompileStatic
trait DirtyCheckable {

    @Transient
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
        if(this instanceof EntityProxy && !((EntityProxy)this).isInitialized()) {
            return false
        }
        else {
            return $changedProperties == null || DirtyCheckingSupport.DIRTY_CLASS_MARKER.is($changedProperties) || !$changedProperties.isEmpty()
        }
    }

    /**
     * @param propertyName The name of the property
     * @return True if the given property has any changes
     */
    boolean hasChanged(String propertyName) {
        if(this instanceof EntityProxy && !((EntityProxy)this).isInitialized()) {
            return false
        }
        else {
            return $changedProperties == null || DirtyCheckingSupport.DIRTY_CLASS_MARKER.is($changedProperties) || $changedProperties?.containsKey(propertyName)
        }
    }

    /**
     * Marks the whole class and all its properties as dirty. When called any future call to any of the hasChanged methods will return true.
     */
    void markDirty() {
        if( $changedProperties != null && $changedProperties.isEmpty()) {
            $changedProperties = DirtyCheckingSupport.DIRTY_CLASS_MARKER
        }
    }

    /**
     * Marks the given property name as dirty
     * @param propertyName The property name
     */
    void markDirty(String propertyName) {
        if( $changedProperties != null && !$changedProperties.containsKey(propertyName))  {
            $changedProperties.put propertyName, ((GroovyObject)this).getProperty(propertyName)
        }
    }

    /**
     * Marks the given property name as dirty
     * @param propertyName The property name
     * @param newValue The new value
     */
    void markDirty(String propertyName, newValue) {
        if( $changedProperties != null && !$changedProperties.containsKey(propertyName))  {
            def oldValue = ((GroovyObject) this).getProperty(propertyName)
            if ((newValue == null && oldValue != null) ||
                (newValue != null && oldValue == null) ||
                (newValue != null && !newValue.equals(oldValue))) {
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
        if( $changedProperties != null && !$changedProperties.containsKey(propertyName))  {
            if ((newValue == null && oldValue != null) ||
                (newValue != null && oldValue == null) ||
                (newValue != null && !newValue.equals(oldValue))) {
                $changedProperties.put propertyName, oldValue
            }
        }
    }

    /**
     * @return A list of the dirty property names
     */
    List<String> listDirtyPropertyNames() {
        if(this instanceof EntityProxy && !((EntityProxy)this).isInitialized()) {
            return Collections.emptyList()
        }

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