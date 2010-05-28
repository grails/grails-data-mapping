package org.grails.inconsequential.mapping;

import java.util.List;

/**
 * This interface defines a strategy for reading how
 * persistent properties are defined in a persistent entity
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public interface MappingSyntaxStrategy {

    /**
     * Tests whether the given class is a persistent entity
     *
     * @param javaClass The java class
     * @return true if it is a persistent entity
     */
    boolean isPersistentEntity(Class javaClass);

    /**
     * @see #getPersistentProperties(Class, MappingContext, ClassMapping)
     */
    List<PersistentProperty> getPersistentProperties(Class javaClass, MappingContext context);

    /**
     * Obtains a List of PersistentProperty instances for the given Mapped class
     *
     * @param javaClass The Java class
     * @param context The MappingContext instance
     * @param mapping The mapping for this class
     * @return The PersistentProperty instances
     */
    List<PersistentProperty> getPersistentProperties(Class javaClass, MappingContext context, ClassMapping mapping);

    /**
     * Obtains the identity of a persistent entity
     *
     * @param javaClass The Java class
     * @param context The MappingContext
     * @return A PersistentProperty instance
     */
    PersistentProperty getIdentity(Class javaClass, MappingContext context);

    /**
     * Obtains the default manner in which identifiers are mapped. In GORM
     * this is just using a property called 'id', but in other frameworks this
     * may differ. For example JPA expects an annotated @Id property
     *
     * @return The default identifier mapping
     * @param classMapping The ClassMapping instance
     */
    IdentityMapping getDefaultIdentityMapping(ClassMapping classMapping);
}
