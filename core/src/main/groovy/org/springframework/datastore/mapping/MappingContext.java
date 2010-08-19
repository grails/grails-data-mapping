/* Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.datastore.mapping;

import java.util.List;

/**
 * <p>This interface defines the overall context including all known
 * PersistentEntity instances and methods to obtain instances on demand</p>
 *
 * <p>This interface is used internally to establish associations
 * between entities and also at runtime to obtain entities by name</p>
 *
 * <p>The generic type parameters T & R are used to specify the
 * mapped form of a class (example Table) and property (example Column) respectively.</p>
 *
 * <p>Used instances of the {@link org.springframework.datastore.core.Datastore} interface to
 * discover how to persist objects</p>
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public interface MappingContext<T,R> {

    /**
     * Obtains a list of PersistentEntity instances
     *
     * @return A list of PersistentEntity instances
     */
    List<PersistentEntity> getPersistentEntities();

    /**
     * Obtains a PersistentEntity by name
     *
     * @param name The name of the entity
     * @return The entity or null
     */
    PersistentEntity getPersistentEntity(String name);

    /**
     * Adds a PersistentEntity instance
     *
     * @param javaClass The Java class representing the entity
     * @return The PersistentEntity instance
     */
    PersistentEntity addPersistentEntity(Class javaClass);

    /**
     * Returns the syntax reader used to interpret the entity
     * mapping syntax
     *
     * @return The SyntaxReader
     */
    MappingConfigurationStrategy getMappingSyntaxStrategy();

    /**
     * Obtains the MappingFactory instance
     * @return The mapping factory instance
     */
    MappingFactory<T,R> getMappingFactory();

    /**
     * Returns whether the specified class is a persistent entity
     * @param type The type to check
     * @return True if it is
     */
    boolean isPersistentEntity(Class type);

    /**
     * Returns whether the specified value is a persistent entity
     * @param value The value to check
     * @return True if it is
     */
    boolean isPersistentEntity(Object value);
}
