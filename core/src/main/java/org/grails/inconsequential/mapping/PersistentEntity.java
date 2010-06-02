package org.grails.inconsequential.mapping;

import org.grails.inconsequential.mapping.lifecycle.Initializable;

import java.util.List;

/**
 *
 * Represents a persistent entity
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public interface PersistentEntity extends Initializable {

    /**
     * The entity name including any package prefix
     *
     * @return The entity name
     */
    String getName();

    /**
     * Returns the identity of the instance
     *
     * @return The identity
     */
    PersistentProperty getIdentity();

    /**
     * A list of properties to be persisted
     * @return A list of PersistentProperty instances
     */
    List<PersistentProperty> getPersistentProperties();

    /**
     * Obtains a PersistentProperty instance by name
     *
     * @param name The name of the property
     * @return The PersistentProperty or null if it doesn't exist
     */
    PersistentProperty getPropertyByName(String name);

    /**
     * @return The underlying Java class for this entity
     */
    Class getJavaClass();

    /**
     * Tests whether the given instance is an instance of this persistent entity
     *
     * @param obj The object
     * @return True if it is
     */
    boolean isInstance(Object obj);

    /**
     * Defines the mapping between this persistent entity
     * and an external form
     *
     * @return The ClassMapping instance
     */
    ClassMapping getMapping();
    
}
