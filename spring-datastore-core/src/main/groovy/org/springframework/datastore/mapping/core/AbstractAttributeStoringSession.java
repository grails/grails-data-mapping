/* Copyright (C) 2011 SpringSource
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

package org.springframework.datastore.mapping.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.datastore.mapping.transactions.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public abstract class AbstractAttributeStoringSession implements Session {

    protected Map<Object, Map<String, Object>> attributes = new ConcurrentHashMap<Object, Map<String, Object>>();

    public void setAttribute(Object entity, String attributeName, Object value) {
        if (entity == null) {
            return;
        }

        Map<String, Object> attrs = attributes.get(entity);
        if (attrs == null) {
            attrs = new ConcurrentHashMap<String, Object>();
            attributes.put(entity, attrs);
        }

        if (attributeName != null && value != null) {
            attrs.put(attributeName, value);
        }
    }

    public Object getAttribute(Object entity, String attributeName) {
        if (entity == null) {
            return null;
        }

        final Map<String, Object> attrs = attributes.get(entity);
        if (attrs == null || attributeName == null) {
            return null;
        }

        return attrs.get(attributeName);
    }

    /**
     * Performs clear up. Subclasses should always call into this super
     * implementation.
     */
    public void disconnect() {
        clear();
        attributes.clear();
        SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.getResource(getDatastore());
        if (sessionHolder == null) {
            return;
        }

        sessionHolder.removeSession(this);
        if (sessionHolder.isEmpty()) {
            try {
                TransactionSynchronizationManager.unbindResource(getDatastore());
            } catch (IllegalStateException e) {
                // ignore session disconnected by a another thread
            }
        }
    }
}
