package org.springframework.datastore.mapping.simpledb.util;

/**
 * Various constants for SimpleDB support.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
public class SimpleDBConst {
    public static final String PROP_SHARDING_ENABLED = "enabled";
    /**
     * What must be specified in mapping as a value of 'mapWith' to map the domain class with SimpleDB gorm plugin:
     * <pre>
     * class DomPerson {
     *      String id
     *      String firstName
     *      static mapWith = "simpledb"
     * }
     * </pre>
     */
    public static final String SIMPLE_DB_MAP_WITH_VALUE = "simpledb";
}
