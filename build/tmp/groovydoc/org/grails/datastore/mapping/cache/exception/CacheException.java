package org.grails.datastore.mapping.cache.exception;

/**
 * Top-level exception used to report problems with third-party cache operations
 * @author Roman Stepanenko
 */
public class CacheException extends RuntimeException{
    private static final long serialVersionUID = 1;

    public CacheException() {
    }

    public CacheException(String message) {
        super(message);
    }

    public CacheException(String message, Throwable cause) {
        super(message, cause);
    }

    public CacheException(Throwable cause) {
        super(cause);
    }
}
