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
package org.grails.datastore.mapping.cassandra;

import java.io.Serializable;
import java.util.Map;

import org.grails.datastore.mapping.cassandra.config.Table;
import org.grails.datastore.mapping.cassandra.engine.CassandraEntityPersister;
import org.grails.datastore.mapping.core.AbstractSession;
import org.grails.datastore.mapping.core.Datastore;
import org.grails.datastore.mapping.engine.NonPersistentTypeException;
import org.grails.datastore.mapping.engine.Persister;
import org.grails.datastore.mapping.model.ClassMapping;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.transactions.SessionOnlyTransaction;
import org.grails.datastore.mapping.transactions.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.util.Assert;

import com.datastax.driver.core.Session;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
public class CassandraSession extends AbstractSession<Session> {

    private Logger log = LoggerFactory.getLogger(CassandraSession.class);
    private Session session;
    private ApplicationEventPublisher applicationEventPublisher;
    private CassandraTemplate cassandraTemplate;

    public CassandraSession(Datastore ds, MappingContext context, Session session, ApplicationEventPublisher applicationEventPublisher, boolean stateless, CassandraTemplate cassandraTemplate) {
        super(ds, context, applicationEventPublisher, stateless);
        Assert.notNull(session, "Native session to Cassandra is null");
        this.applicationEventPublisher = applicationEventPublisher;
        this.session = session;
        this.cassandraTemplate = cassandraTemplate;
    }


    @Override
    public boolean hasTransaction() {
        // the session is the transaction, since Cassandra doesn't support them directly
        return true;
    }

    @Override
    protected Persister createPersister(Class cls, MappingContext mappingContext) {
        PersistentEntity entity = mappingContext.getPersistentEntity(cls.getName());
        if (entity != null) {
            return new CassandraEntityPersister(mappingContext, entity, this, applicationEventPublisher);
        }
        return null;
    }

    @Override
    public void disconnect() {
        super.disconnect();
    }

    @Override
    protected Transaction beginTransactionInternal() {
        return new SessionOnlyTransaction<Session>(getNativeInterface(), this);
    }

    public CassandraDatastore getCassandraDatastore() {
    	return (CassandraDatastore) getDatastore();
    }
    
    public Session getNativeInterface() {
        return session;
    }

    public CassandraTemplate getCassandraTemplate() {
        return cassandraTemplate;
    }
    
    public Serializable update(Object o) {
        Assert.notNull(o, "Cannot persist null object");
        Persister persister = getPersister(o);
        if (persister == null) {
            throw new NonPersistentTypeException("Object [" + o +
                    "] cannot be persisted. It is not a known persistent type.");
        }
        CassandraEntityPersister cassandraEntityPersister = (CassandraEntityPersister) persister;
        final Serializable key = (Serializable) cassandraEntityPersister.update(o);
        cacheObject(key, o);
        return key;
    }
    
    public void deleteAll(Class type) {
        cassandraTemplate.truncate(cassandraTemplate.getTableName(type));
    }

    @Override
    protected Serializable convertIdentityIfNecessasry(PersistentProperty identity, Serializable key) {
        final ClassMapping classMapping = identity.getOwner().getMapping();
        final Table table = (Table) classMapping.getMappedForm();
        if(table.hasCompositePrimaryKeys() && (key instanceof Map)) {
            return key;
        }
        else {
            return super.convertIdentityIfNecessasry(identity, key);
        }
    }
}
