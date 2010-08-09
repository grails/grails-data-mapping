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

import org.springframework.datastore.engine.EntityInterceptor;
import org.springframework.datastore.engine.EntityInterceptorAware;
import org.springframework.datastore.engine.NonPersistentTypeException;
import org.springframework.datastore.engine.Persister;
import org.springframework.datastore.mapping.MappingContext;
import org.springframework.datastore.mapping.PersistentEntity;
import org.springframework.datastore.query.Query;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract implementation of the ObjectDatastore interface that uses
 * a list of {@link org.springframework.datastore.engine.Persister} instances
 * to save, update and delete instances
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public abstract class AbstractSession<N> implements Session {
    protected Map<Class,Persister> persisters = new ConcurrentHashMap<Class,Persister>();
    private MappingContext mappingContext;
    private Map<String, String> connectionDetails;
    protected List<EntityInterceptor> interceptors = new ArrayList<EntityInterceptor>();

    public AbstractSession(Map<String, String> connectionDetails, MappingContext mappingContext) {
        super();
        this.mappingContext = mappingContext;
        this.connectionDetails = connectionDetails;
    }

    public void addEntityInterceptor(EntityInterceptor interceptor) {
        if(interceptor != null) {
            this.interceptors.add(interceptor);
        }
    }

    public void setEntityInterceptors(List<EntityInterceptor> interceptors) {
        if(interceptors!=null) this.interceptors = interceptors;
    }

    public Map<String, String> getDetails() {
        return connectionDetails;
    }

    public MappingContext getMappingContext() {
        return this.mappingContext;
    }

    public final Persister getPersister(Object o) {
        if(o == null) return null;
        Class cls;
        if(o instanceof Class) {
           cls = (Class) o;
        }
        else if(o instanceof PersistentEntity) {
            cls = ((PersistentEntity)o).getJavaClass();
        }
        else {
           cls = o.getClass();
        }
        Persister p = this.persisters.get(cls);
        if(p == null) {
            p = createPersister(cls, getMappingContext());
            if(p instanceof EntityInterceptorAware) {
                ((EntityInterceptorAware)p).setEntityInterceptors(interceptors);
            }
            if(p != null)
                this.persisters.put(cls, p);
        }
        return p;
    }

    protected abstract Persister createPersister(Class cls, MappingContext mappingContext);


    public Serializable persist(Object o) {
        if(o == null) throw new IllegalArgumentException("Cannot persist null object");
        Persister persister = getPersister(o);
        if(persister != null) {
            return persister.persist(o);
        }
        throw new NonPersistentTypeException("Object ["+o+"] cannot be persisted. It is not a known persistent type.");
    }

    public Object retrieve(Class type, Serializable key) {
        if(key == null || type == null) return null;
        Persister persister = getPersister(type);
        if(persister != null) {
            return persister.retrieve(key);
        }
        throw new NonPersistentTypeException("Cannot retrieve object with key ["+key+"]. The class ["+type+"] is not a known persistent type.");
    }

    public void delete(Object obj) {
        if(obj != null) {
            Persister p = getPersister(obj);
            p.delete(obj);
        }
    }

    public void delete(Iterable objects) {
        if(objects != null) {
            for (Object object : objects) {
                if(object != null) {
                    Persister p = getPersister(object);
                    if(p != null) {
                        p.delete(objects);
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

    public List<Serializable> persist(Iterable objects) {
        if(objects != null) {

            final Iterator i = objects.iterator();
            if(i.hasNext()) {
                // peek at the first object to get the persister
                final Object obj = i.next();
                final Persister p = getPersister(obj);
                if(p != null) {
                    return p.persist(objects);
                }
                else {
                    throw new NonPersistentTypeException("Cannot persist objects. The class ["+obj.getClass()+"] is not a known persistent type.");
                }
            }

        }
        return Collections.emptyList();
    }

    public List retrieveAll(Class type, Iterable keys) {
        Persister p = getPersister(type);

        if(p != null) {
            return p.retrieveAll(keys);
        }
        throw new NonPersistentTypeException("Cannot retrieve objects with keys ["+keys+"]. The class ["+type+"] is not a known persistent type.");
    }

    public Query createQuery(Class type) {
        Persister p = getPersister(type);
        if(p!= null) {
            return p.createQuery();
        }
        throw new NonPersistentTypeException("Cannot create query. The class ["+type+"] is not a known persistent type.");
    }
}
