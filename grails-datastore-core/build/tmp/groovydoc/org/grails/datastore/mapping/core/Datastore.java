/* Copyright (C) 2010 SpringSource
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
package org.grails.datastore.mapping.core;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.grails.datastore.mapping.model.MappingContext;
import org.springframework.validation.Errors;

/**
 * The <code>Datastore</code> interface is the basic commom denominator all NoSQL databases should support:
 * <ul>
 *     <li>Storing data</li>
 *     <li>Retrieving one or more elements at a time, identified by their keys</li>
 *     <li>Deleting one or more elements</li>
 * </ul>
 *
 * @author Guillaume Laforge
 * @author Graeme Rocher
 */
public interface Datastore {

    /**
     * Connects to the datastore with the default connection details, normally provided via the datastore implementations constructor
     *
     * @return The session created using the default connection details.
     */
    Session connect();

    /**
     * Obtains the current session (if any)
     * @return The current thread bound session
     *
     * @throws ConnectionNotFoundException Thrown if the {@link #connect()} method has not yet been called
     */
    Session getCurrentSession() throws ConnectionNotFoundException;

    /**
     * Checks if there is a current session.
     * @return true if there's a bound active session
     */
    boolean hasCurrentSession();

    /**
     * Obtains the MappingContext object
     *
     * @return The MappingContext object
     */
    MappingContext getMappingContext();

    /**
     * Get the application event publisher.
     * @return the publisher
     */
    ApplicationEventPublisher getApplicationEventPublisher();

    /**
     * Get the application context.
     * @return the context
     */
    ConfigurableApplicationContext getApplicationContext();

    /**
     * Get the validation errors if available.
     * @param o the entity
     * @return the errors or null
     */
    Errors getObjectErrors(Object o);

    /**
     * Register validation errors for an instance.
     * @param object the instance
     * @param errors the errors
     */
    void setObjectErrors(Object object, Errors errors);

    /**
     * Check if validation should be skipped.
     * @param o the instance
     * @return true to skip
     */
    boolean skipValidation(Object o);

    /**
     * Register that validation should be skipped or not.
     * @param o the instance
     * @param skip whether to skip or not
     */
    void setSkipValidation(Object o, boolean skip);

    /**
     * Whether the datastore is schema-less. That is it allows changes to the schema runtime, dynamic attributes etc.
     *
     * @return True if it does
     */
    boolean isSchemaless();
}
