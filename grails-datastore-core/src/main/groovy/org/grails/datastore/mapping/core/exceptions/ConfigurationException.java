package org.grails.datastore.mapping.core.exceptions;

/**
 * An exception thrown if a configuration error occurs
 *
 * @author Graeme Rocher
 * @since 6.0
 */
public class ConfigurationException extends RuntimeException {

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
