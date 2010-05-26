package org.grails.inconsequential.mapping;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
public interface MappedPersistentEntity extends PersistentEntity{
    /**
     * Defines the mapping between this persistent entity
     * and an external form
     *
     * @return The ClassMapping instance
     */
    ClassMapping getMapping();
}
