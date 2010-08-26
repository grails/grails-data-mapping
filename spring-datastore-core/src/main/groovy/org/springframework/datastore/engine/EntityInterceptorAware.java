package org.springframework.datastore.engine;

import java.util.List;

/**
 * An interface for classes aware of {@link EntityInterceptor} instances
 *
 * @since 1.0
 * @author Graeme Rocher
 */
public interface EntityInterceptorAware {

    /**
     * @param interceptors A list of entity interceptors
     */
    void setEntityInterceptors(List<EntityInterceptor> interceptors);

    /**
     * Adds an {@link org.springframework.datastore.engine.EntityInterceptor}
     * @param interceptor The interceptor to add
     */
    void addEntityInterceptor(EntityInterceptor interceptor);
}
