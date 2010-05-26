package org.grails.inconsequential.mapping;

import java.util.List;

/**
 *
 * Represents a persistent entity
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public interface PersistentEntity {

    /**
     * The entity name including any package prefix
     *
     * @return The entity name
     */
    String getName();

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
}
