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
package org.springframework.datastore.proxy;

import org.springframework.aop.TargetSource;
import org.springframework.datastore.core.Session;
import org.springframework.datastore.engine.EntityPersister;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * A target source that lazily loads an entity
 *
 * @author Graeme Rocher
 */
public class LazyLoadingTargetSource implements TargetSource {

    private Serializable key;
    private Session session;
    private Class entityType;

    public LazyLoadingTargetSource(Session session, Class entityType, Serializable key) {
        Assert.notNull(session,"Argument [entityPesister] cannot be null");
        this.session = session;
        this.entityType = entityType;
        this.key = key;
    }

    public Class getTargetClass() {
        return entityType;
    }

    public boolean isStatic() {
        return false;
    }

    public Object getTarget() throws Exception {
        return session.retrieve(entityType, key);
    }

    public void releaseTarget(Object target) throws Exception {
        // do nothing
    }
}
