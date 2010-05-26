package org.grails.inconsequential.mapping;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
public interface PersistentProperty {

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

}
