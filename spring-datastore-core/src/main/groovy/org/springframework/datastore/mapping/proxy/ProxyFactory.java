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
package org.springframework.datastore.mapping.proxy;

import org.springframework.datastore.mapping.core.Session;

import java.io.Serializable;

/**
 * The factory used to create proxies
 * 
 * @author Graeme Rocher
 * @since 1.0
 */
public interface ProxyFactory {

	/**
	 * Creates a proxy
	 * 
	 * @param <T> The type of the proxy to create
	 * @param session The session instance
	 * @param type The type of the proxy to create
	 * @param key The key to proxy
	 * @return A proxy instance
	 */
    <T> T createProxy(Session session, Class<T> type, Serializable key);

    /**
     * Checks whether the specified instance is a proxy
     * 
     * @param object The object to check
     * @return True if it is a proxy
     */
    boolean isProxy(Object object);

    /**
     * Obtains the identifier of an object without initializing the proxy if it is one
     * @param obj The object
     * @return The identifier
     */
    Serializable getIdentifier(Object obj);
}
