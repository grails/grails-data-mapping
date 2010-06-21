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
package org.grails.inconsequential.core;

import org.grails.inconsequential.engine.CannotPersistException;
import org.grails.inconsequential.engine.Persister;
import org.grails.inconsequential.mapping.MappingContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract implementation of the ObjectDatastore interface that uses
 * a list of {@link org.grails.inconsequential.engine.Persister} instances
 * to save, update and delete instances
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public abstract class AbstractObjectDatastoreConnection<T> implements ObjectDatastoreConnection<T> {
    protected Map<Class,Persister> persisters = new ConcurrentHashMap<Class,Persister>();
    private MappingContext mappingContext;
    private Map<String, String> connectionDetails;


    public AbstractObjectDatastoreConnection(Map<String, String> connectionDetails, MappingContext mappingContext) {
        super();
        this.mappingContext = mappingContext;
        this.connectionDetails = connectionDetails;
    }

    public Map<String, String> getDetails() {
        return connectionDetails;
    }

    public MappingContext getMappingContext() {
        return this.mappingContext;
    }

    public final Persister getPersister(Object o) {
        if(o == null) return null;
        Class cls = o instanceof Class ? (Class) o : o.getClass();
        Persister p = this.persisters.get(cls);
        if(p == null) {
            p = createPersister(cls, getMappingContext());
            if(p != null)
                this.persisters.put(cls, p);
        }
        return p;
    }

    protected abstract Persister createPersister(Class cls, MappingContext mappingContext);


    public Key<T> persist(Object o) {
        if(o == null) throw new IllegalArgumentException("Cannot persist null object");
        Persister persister = getPersister(o);
        if(persister != null) {
            return persister.persist(getMappingContext(), o);
        }
        throw new CannotPersistException("Object ["+o+"] cannot be persisted. It is not a known persistent type.");
    }

    public Object retrieve(Class type, Key<T> key) {
        if(key == null || type == null) return null;
        Persister persister = getPersister(type);
        if(persister != null) {
            return persister.retrieve(getMappingContext(), key);
        }
        throw new CannotPersistException("Cannot retrieveEntity object with key ["+key+"]. The class ["+type+"] is not a known persistent type.");
    }

    public void delete(Object... objects) {
        if(objects != null) {
            for (Object object : objects) {
                if(object != null) {
                    Persister p = getPersister(object);
                    if(p != null) {
                        p.delete(getMappingContext(), objects);
                    }                   
                    break;
                }
            }
        }
    }

    /**
     * Performs clear up. Subclasses should always call into this super
     * implementation.
     */
    public void disconnect() {
        AbstractDatastore.clearCurrentConnection();
    }
}
