package org.grails.datastore.mapping.config

/**
 * A mapping definition
 *
 * @author Graeme Rocher
 * @since 6.1
 */
interface MappingDefinition<E extends Entity, P extends Property> {

    /**
     * Configures an existing mapping
     *
     * @param existing The existing mapping
     * @return
     */
    E configure(E existing)

    /**
     * Build a new mapping
     *
     * @return The new mapping
     */
    E build()

}