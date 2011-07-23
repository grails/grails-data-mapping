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
package org.grails.datastore.mapping.simpledb;

import org.grails.datastore.mapping.model.MappingContext;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Simple extension used in testing to fight eventual consistency of SimpleDB.
 */
public class DelayAfterWriteSimpleDBSession extends SimpleDBSession {
    public DelayAfterWriteSimpleDBSession(SimpleDBDatastore datastore, MappingContext mappingContext, ApplicationEventPublisher publisher, long delayMillis) {
        super(datastore, mappingContext, publisher);
        this.delayMillis = delayMillis;
    }

    @Override
    protected void postFlush(@SuppressWarnings("unused") boolean hasUpdates) {
        if (hasUpdates) {
            pause();
        }
    }

    private void pause(){
        try { Thread.sleep(delayMillis); } catch (InterruptedException e) { }
    }

    private long delayMillis;
}
