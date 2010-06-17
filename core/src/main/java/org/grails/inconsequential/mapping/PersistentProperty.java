package org.grails.inconsequential.mapping;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
public interface PersistentProperty<T> {

    /**
     * The name of the property
     * @return The property name
     */
    String getName();

    /**
     * The type of the property
     * @return The property type
     */
    Class getType();

    /**
    * Specifies the mapping between this property and an external form
    * such as a column, key/value pair etc.
    *
    * @return The PropertyMapping instance
    */
    PropertyMapping<T> getMapping();

    /**
     * Obtains the owner of this persistent property
     *
     * @return The owner
     */
    PersistentEntity getOwner();

}
