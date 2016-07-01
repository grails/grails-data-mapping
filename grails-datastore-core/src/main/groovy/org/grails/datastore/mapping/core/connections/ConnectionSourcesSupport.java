package org.grails.datastore.mapping.core.connections;

import org.grails.datastore.mapping.config.Entity;
import org.grails.datastore.mapping.model.PersistentEntity;

import java.util.Collections;
import java.util.List;

/**
 * Utility methods for {@link ConnectionSource} handling
 *
 * @author Graeme Rocher
 * @since 6.0
 */
public class ConnectionSourcesSupport {

    public static final List<String> DEFAULT_CONNECTION_SOURCE_NAMES = Collections.singletonList(ConnectionSource.DEFAULT);

    /**
     * If a domain class uses more than one datasource, we need to know which one to use
     * when calling a method without a namespace qualifier.
     *
     * @param entity the domain class
     * @return the default datasource name
     */
    public static String getDefaultConnectionSourceName(PersistentEntity entity) {
        List<String> names = getConnectionSourceNames(entity);
        if (names.size() == 1 && ConnectionSource.ALL.equals(names.get(0))) {
            return ConnectionSource.ALL;
        }
        return names.get(0);
    }

    /**
     * Obtain all of the {@link ConnectionSource} names for the given entity
     *
     * @param entity The entity
     * @return The {@link ConnectionSource} names
     */
    public static List<String> getConnectionSourceNames(PersistentEntity entity) {
        final Entity mappedForm = entity.getMapping().getMappedForm();
        if(mappedForm != null)  {
            return mappedForm.getDatasources();
        }
        return DEFAULT_CONNECTION_SOURCE_NAMES;
    }

    /**
     * Returns whether the given entity uses the give connection source name or not
     *
     * @param entity The name of the entity
     * @param connectionSourceName The connection source name
     * @return Whether the given connection source is used
     */
    public static boolean usesConnectionSource(PersistentEntity entity, String connectionSourceName) {
        List<String> names = getConnectionSourceNames(entity);
        return names.contains(connectionSourceName) ||
                names.contains(ConnectionSource.ALL);
    }
}
