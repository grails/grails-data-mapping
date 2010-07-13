package org.springframework.datastore.mapping.types;

/**
 *
 * Defines the cascade strategy for associations
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public enum Cascade {
    SAVE, UPDATE, DELETE, ALL
}
