package org.springframework.datastore.mapping.simpledb.engine;

/**
 * Encapsulates logic of determining SimpleDB domain name based specific a primary key, assuming that
 * this instance of the resolver is used only for one {@link org.springframework.datastore.mapping.model.PersistentEntity},
 * which was provided during construction time of this instance.
 * This class is used to enable sharding and to provide various sharding hashing function 
 * algorithms.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
public interface SimpleDBDomainResolver {
    /**
     * Returns domain name for the specified primary key value.
     * @param id
     * @return
     */
    String resolveDomain(String id);
}
