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
package org.springframework.datastore.core;

import org.springframework.datastore.mapping.MappingContext;

import java.util.Map;

/**
 * Abstract Datastore implementation that deals with binding the
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public abstract class AbstractDatastore implements Datastore {

    MappingContext mappingContext;
    private static ThreadLocal<Session> currentConnectionThreadLocal = new InheritableThreadLocal<Session>();

    public AbstractDatastore(MappingContext mappingContext) {
        this.mappingContext = mappingContext;
    }

    public AbstractDatastore() {
    }

    public final Session connect(Map<String, String> connectionDetails) {
        final Session connection = createConnection(connectionDetails);
        if(connection != null) {
            currentConnectionThreadLocal.set(connection);
        }
        return connection;
    }

    /**
     * Creates the native session
     *
     * @param connectionDetails The session details
     * @return The session object
     */
    protected abstract Session createConnection(Map<String, String> connectionDetails);

    public final Session getCurrentSession() throws ConnectionNotFoundException {
        final Session connection = currentConnectionThreadLocal.get();
        if(connection == null) {
            throw new ConnectionNotFoundException("Not datastore session found. Call Datastore.connect(..) before calling Datastore.getCurrentSession()");
        }
        return connection;
    }

    public MappingContext getMappingContext() {
        return mappingContext;
    }

    /**
     * Clears the thread bound session, should be called by the
     * {@link Session#disconnect()}
     */
    public static void clearCurrentConnection() {
        currentConnectionThreadLocal.set(null);
    }
}
