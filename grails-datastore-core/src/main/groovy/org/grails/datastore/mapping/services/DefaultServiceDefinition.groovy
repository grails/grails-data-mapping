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
/**
 * Default implementation of {@link ServiceDefinition}.
 *
 * @param <S> The type
 * @author Graeme Rocher
 * @since 1.0
 */
class DefaultServiceDefinition<S> implements ServiceDefinition<S> {
    private final String name
    private final Class<S> loadedClass

    /**
     * @param name        The name
     * @param loadedClass The loaded class
     */
    DefaultServiceDefinition(String name, Class<S> loadedClass) {
        this.name = name
        this.loadedClass = loadedClass
    }

    @Override
    String getName() {
        return name
    }

    @Override
    Class<S> getType() {
        if (loadedClass == null) {
            new ServiceConfigurationError("Call to load() when class '" + name + "' is not present")
        }
        return loadedClass
    }

    @Override
    boolean isPresent() {
        return loadedClass != null
    }

    @Override
    public <X extends Throwable> S orElseThrow(Closure exceptionSupplier) throws X {
        if (loadedClass == null) {
            exceptionSupplier()
            return
        }
        final Class<S> type = loadedClass
        try {
            return type.newInstance()
        } catch (Throwable e) {
            throw exceptionSupplier()
        }
    }

    @Override
    S load() {
        if (loadedClass == null) {
            new ServiceConfigurationError("Call to load() when class '" + name + "' is not present")
        }
        try {
            return loadedClass.newInstance()
        } catch (Throwable e) {
            throw new ServiceConfigurationError("Error loading service [" + loadedClass.getName() + "]: " + e.getMessage(), e)
        }
    }
}
