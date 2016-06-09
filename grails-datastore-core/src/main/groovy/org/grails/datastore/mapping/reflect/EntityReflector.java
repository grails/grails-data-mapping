package org.grails.datastore.mapping.reflect;


import org.grails.datastore.mapping.model.PersistentEntity;
import org.springframework.cglib.reflect.FastClass;

import java.io.Serializable;
import java.util.List;

/**
 * Used for reflective data
 *
 * @author Graeme Rocher
 * @since 5.0
 */
public interface EntityReflector {

    /**
     * The entity
     */
    PersistentEntity getPersitentEntity();
    /**
     * @return The fast class
     */
    FastClass fastClass();
    /**
     * @return The identity type
     */
    Class identifierType();
    /**
     * @return The name of the identifier
     */
    String getIdentifierName();

    /**
     * @return The property names
     */
    Iterable<String> getPropertyNames();

    /**
     * @return Obtain the identifier
     */
    Serializable getIdentifier(Object object);

    /**
     * Set the identifier
     *
     * @param value The value
     */
    void setIdentifier(Object object, Object value);

    /**
     * Get a property for the specified index
     *
     * @param object The object
     * @param index The index
     * @return The value
     */
    Object getProperty(Object object, int index);

    /**
     * Set a property for the specified index
     *
     * @param object The object
     * @param index The index
     * @param value  The value
     */
    void setProperty(Object object, int index, Object value);

    /**
     * Get a property for the specified index
     *
     * @param object The object
     * @param name The index
     * @return The value
     */
    Object getProperty(Object object, String name);

    /**
     * Set a property for the specified index
     *
     * @param object The object
     * @param name The index
     * @param value  The value
     */
    void setProperty(Object object, String name, Object value);

    /**
     * @param name Obtains the property reader for the given property
     *
     * @return The name of the property
     */
    PropertyReader getPropertyReader(String name);

    /**
     * @param name Obtains the property writer for the given property
     * @return The property writer
     */
    PropertyWriter getPropertyWriter(String name);

    interface PropertyReader {
        /**
         * @return The property type
         */
        Class propertyType();

        /**
         * reads the property
         *
         * @param object The object
         * @return The read value
         */
        Object read(Object object);
    }

    interface PropertyWriter {
        /**
         * @return The property type
         */
        Class propertyType();
        /**
         * Writes the property
         *
         * @param object the object
         * @param value The value
         */
        void write(Object object, Object value);
    }
}