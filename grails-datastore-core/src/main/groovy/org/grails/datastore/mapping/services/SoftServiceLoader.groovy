/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.datastore.mapping.services

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.helpers.NOPLogger
import org.springframework.util.ClassUtils


/**
 * <p>Variation of {@link java.util.ServiceLoader} that allows soft loading and conditional loading of
 * META-INF/services classes.</p>
 *
 * @param < S >  The service type
 * @author Graeme Rocher
 * @since 1.0
 */
final class SoftServiceLoader<S> implements Iterable<ServiceDefinition<S>> {
    static final String PROPERTY_GRAILS_CLASSLOADER_LOGGING = "grails.classloader.logging"
    static final String META_INF_SERVICES = "META-INF/services"
    static final Logger REFLECTION_LOGGER

    private static final boolean ENABLE_CLASS_LOADER_LOGGING = Boolean.getBoolean(PROPERTY_GRAILS_CLASSLOADER_LOGGING)


    private final Class<S> serviceType
    private final ClassLoader classLoader
    private final Map<String, ServiceDefinition<S>> loadedServices = new LinkedHashMap<>()
    private final Iterator<ServiceDefinition<S>> unloadedServices
    private final Closure<Boolean> condition

    static {
        REFLECTION_LOGGER = getLogger(ClassUtils.class)
    }


    private SoftServiceLoader(Class<S> serviceType, ClassLoader classLoader) {
        this(serviceType, classLoader, { String name -> true })
    }

    private SoftServiceLoader(Class<S> serviceType, ClassLoader classLoader, Closure<Boolean> condition) {
        this.serviceType = serviceType
        this.classLoader = classLoader == null ? ClassLoader.getSystemClassLoader() : classLoader
        this.unloadedServices = new ServiceLoaderIterator()
        this.condition = condition == null ? { String name -> true } : condition
    }

    /**
     * Special case {@code getLogger} method that should be used by classes that are used in the annotation processor.
     *
     * @param type The type
     * @return The logger
     */
    static Logger getLogger(Class type) {
        if (ENABLE_CLASS_LOADER_LOGGING) {
            return LoggerFactory.getLogger(type)
        } else {
            return NOPLogger.NOP_LOGGER
        }
    }

    /**
     * Creates a new {@link SoftServiceLoader} using the thread context loader by default.
     *
     * @param service The service type
     * @param < S >      The service generic type
     * @return A new service loader
     */
    static <S> SoftServiceLoader<S> load(Class<S> service) {
        return SoftServiceLoader.load(service, SoftServiceLoader.class.getClassLoader())
    }

    /**
     * Creates a new {@link SoftServiceLoader} using the given type and class loader.
     *
     * @param service The service type
     * @param loader The class loader
     * @param < S >      The service generic type
     * @return A new service loader
     */
    static <S> SoftServiceLoader<S> load(Class<S> service,
                                         ClassLoader loader) {
        return new SoftServiceLoader<>(service, loader)
    }

    /**
     * Creates a new {@link SoftServiceLoader} using the given type and class loader.
     *
     * @param service The service type
     * @param loader The class loader to use
     * @param condition A {@link Closure} to use to conditionally load the service. The predicate is passed the service class name
     * @param < S >        The service generic type
     * @return A new service loader
     */
    static <S> SoftServiceLoader<S> load(Class<S> service,
                                         ClassLoader loader,
                                         Closure<Boolean> condition) {
        return new SoftServiceLoader<>(service, loader, condition)
    }

    /**
     * @return The iterator
     */
    @Override
    Iterator<ServiceDefinition<S>> iterator() {
        return new Iterator<ServiceDefinition<S>>() {
            Iterator<ServiceDefinition<S>> loaded = loadedServices.values().iterator();

            @Override
            boolean hasNext() {
                if (loaded.hasNext()) {
                    return true
                }
                if (unloadedServices.hasNext()) {
                    return true
                }
                return false
            }

            @Override
            ServiceDefinition<S> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException()
                }

                if (loaded.hasNext()) {
                    return loaded.next()
                }
                if (unloadedServices.hasNext()) {
                    ServiceDefinition<S> nextService = unloadedServices.next()
                    loadedServices.put(nextService.getName(), nextService)
                    return nextService
                }
                // should not happen
                throw new Error("Bug in iterator")
            }

            @Override
            void remove() {
                throw new Error("Not Implemented!")
            }
        }
    }

    /**
     * @param name The name
     * @param loadedClass The loaded class
     * @return The service definition
     */
    @SuppressWarnings(["unchecked", "GrMethodMayBeStatic"])
    protected ServiceDefinition<S> newService(String name, Class loadedClass) {
        return new DefaultServiceDefinition(name, loadedClass)
    }

    /**
     * A service loader iterator implementation.
     */
    private final class ServiceLoaderIterator implements Iterator<ServiceDefinition<S>> {
        private Enumeration<URL> serviceConfigs = null
        private Iterator<String> unprocessed = null

        @Override
        boolean hasNext() {

            if (serviceConfigs == null) {
                String name = serviceType.getName()
                try {
                    serviceConfigs = classLoader.getResources(META_INF_SERVICES + '/' + name)
                } catch (IOException e) {
                    throw new ServiceConfigurationError("Failed to load resources for service: " + name, e)
                }
            }
            while (unprocessed == null || !unprocessed.hasNext()) {
                if (!serviceConfigs.hasMoreElements()) {
                    return false
                }
                URL url = serviceConfigs.nextElement()
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))
                    List<String> lines = reader.readLines()
                            .findAll({ line -> line.length() != 0 && line.charAt(0) != ((char) '#') })
                            .findAll(condition)
                            .collect({ line ->
                                int i = line.indexOf('#')
                                if (i > -1) {
                                    line = line.substring(0, i)
                                }
                                return line
                            })
                    unprocessed = lines.iterator()

                } catch (IOException e) {
                    // ignore, can't do anything here and can't log because class used in compiler
                }
            }
            return unprocessed.hasNext()
        }

        @Override
        ServiceDefinition<S> next() {
            if (!hasNext()) {
                throw new NoSuchElementException()
            }

            String nextName = unprocessed.next()
            try {
                final Class<?> loadedClass = Class.forName(nextName, false, classLoader)
                return newService(nextName, loadedClass)
            } catch (NoClassDefFoundError | ClassNotFoundException e) {
                return newService(nextName, null)
            }
        }

        @Override
        void remove() {
            throw new Error("Not Implemented!")
        }
    }
}
