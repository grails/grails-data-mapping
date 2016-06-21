package org.grails.datastore.mapping.config;

import org.grails.datastore.mapping.reflect.ReflectionUtils;
import org.springframework.core.env.PropertyResolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Utility methods for configuration
 * 
 * @author Graeme Rocher
 * @since 6.0
 */
public class ConfigurationUtils {

    /**
     * Finds services of the given type from configuration or via {@link ServiceLoader}
     *
     * @param configuration The configuration
     * @param configurationKey The configuration key
     * @param serviceType The type of service
     * @param <T> The service type
     * @return A list of services
     */
    public static <T> Iterable<T> findServices(PropertyResolver configuration, String configurationKey, Class<T> serviceType) {

        List customTypes = configuration.getProperty(configurationKey, List.class, Collections.emptyList());
        List<T> services = new ArrayList<>();

        for (Object customType : customTypes) {
            Class serviceTypeClass = null;
            if(customType instanceof Class) {
                serviceTypeClass = (Class) customType;
            }
            else if(customType instanceof CharSequence) {
                serviceTypeClass = ReflectionUtils.forName(customType.toString(),ConfigurationUtils.class.getClassLoader());
            }
            if(serviceTypeClass != null && serviceType.isAssignableFrom(serviceTypeClass)) {
                T serviceInstance = (T) ReflectionUtils.instantiate(serviceTypeClass);
                services.add(serviceInstance);
            }
        }

        ServiceLoader<T> serviceLoader = ServiceLoader.load(serviceType, ConfigurationUtils.class.getClassLoader());
        for (T service : serviceLoader) {
            services.add(service);
        }
        return services;
    }
}
