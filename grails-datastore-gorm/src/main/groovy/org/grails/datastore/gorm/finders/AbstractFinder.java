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
package org.grails.datastore.gorm.finders;

import org.grails.datastore.gorm.SessionCallback;
import org.grails.datastore.gorm.VoidSessionCallback;
import org.springframework.datastore.mapping.core.Datastore;
import org.springframework.datastore.mapping.core.DatastoreUtils;
import org.springframework.datastore.mapping.core.Session;

/**
 * Abstract base class for finders.
 * TODO move this and code in AbstractDatastoreApi to shared location.
 *
 * @author Burt Beckwith
 */
public abstract class AbstractFinder implements FinderMethod {

    protected Datastore datastore;

    public AbstractFinder(final Datastore datastore) {
        this.datastore = datastore;
    }

    protected <T> T execute(SessionCallback<T> callback) {
        Session session = DatastoreUtils.getSession(datastore, true);
        boolean existing = DatastoreUtils.isSessionTransactional(session, datastore);
        try {
            return callback.doInSession(session);
        }
        finally {
            if (!existing) {
                DatastoreUtils.closeSessionOrRegisterDeferredClose(session, datastore);
            }
        }
    }

    protected void execute(VoidSessionCallback callback) {
        Session session = DatastoreUtils.getSession(datastore, true);
        boolean existing = DatastoreUtils.isSessionTransactional(session, datastore);
        try {
            callback.doInSession(session);
        }
        finally {
            if (!existing) {
                DatastoreUtils.closeSessionOrRegisterDeferredClose(session, datastore);
            }
        }
    }
}
