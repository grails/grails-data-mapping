package org.grails.inconsequential.mapping;

/**
 * A MappedProperty is a property where a mapping exists
 * between the property of a bean and some external form
 * such as a column or keystore
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public interface MappedPersistentProperty extends PersistentProperty{

    /**
     * Specifies the mapping between this property and an external form
     * such as a column, key/value pair etc.
     *
     * @return The PropertyMapping instance
     */
    PropertyMapping getMapping();


}
