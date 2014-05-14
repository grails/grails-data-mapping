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
package org.grails.datastore.mapping.cassandra.engine;

import static org.springframework.data.cassandra.repository.support.BasicMapId.id;

import java.io.Serializable;
import java.util.List;

import org.grails.datastore.mapping.cassandra.CassandraDatastore;
import org.grails.datastore.mapping.cassandra.CassandraSession;
import org.grails.datastore.mapping.cassandra.query.CassandraQuery;
import org.grails.datastore.mapping.engine.AssociationIndexer;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.grails.datastore.mapping.engine.PropertyValueIndexer;
import org.grails.datastore.mapping.keyvalue.engine.AbstractKeyValueEntityPersister;
import org.grails.datastore.mapping.keyvalue.mapping.config.Family;
import org.grails.datastore.mapping.model.ClassMapping;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.types.Association;
import org.grails.datastore.mapping.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.repository.MapId;

import com.datastax.driver.core.utils.UUIDs;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
public class CassandraEntityPersister extends AbstractKeyValueEntityPersister<EntityAccess, Object> {

    private static Logger log = LoggerFactory.getLogger(CassandraEntityPersister.class);
        
    private static final byte[] ZERO_LENGTH_BYTE_ARRAY = new byte[0];

    public CassandraEntityPersister(MappingContext context, PersistentEntity entity, CassandraSession cassandraSession, ApplicationEventPublisher applicationEventPublisher) {
        super(context, entity, cassandraSession, applicationEventPublisher);                
    }

    protected CassandraTemplate getCassandraTemplate() {
        return getCassandraSession().getCassandraTemplate();
    }

    public CassandraSession getCassandraSession() {
        return (CassandraSession) getSession();
    }

    @Override
    protected String getNativePropertyKey(PersistentProperty prop) {
        // To C* all column names are lowercase
        return super.getNativePropertyKey(prop).toLowerCase();
    }

    @Override
    public AssociationIndexer getAssociationIndexer(EntityAccess nativeEntry, Association association) {
        // return new CassandraAssociationIndexer(cassandraClient, association,
        // getKeyspaceName());
        return null;
    }

    @Override
    public PropertyValueIndexer getPropertyIndexer(PersistentProperty property) {
        return null;
    }

    @Override
    protected EntityAccess createNewEntry(String family, Object instance) {
        return new EntityAccess(null, instance);
    }

    @Override
    protected EntityAccess createNewEntry(String family) {        
        return new EntityAccess(null, null);
    }
    @Override
    protected Object getEntryValue(EntityAccess nativeEntry, String property) {
        return nativeEntry.getProperty(property);
    }

    @Override
    protected void setEntryValue(EntityAccess nativeEntry, String key, Object value) {
        //only going to update the same object so no-op
    }

    @SuppressWarnings("unchecked")
    @Override
    protected EntityAccess retrieveEntry(PersistentEntity persistentEntity, String family, Serializable nativeKey) {
        //TODO: sanity check id
        MapId id = id(persistentEntity.getIdentity().getName(), nativeKey);
        Object entity = getCassandraTemplate().selectOneById(persistentEntity.getJavaClass(), id);
        return entity == null ? null : new EntityAccess(persistentEntity, entity);
    }

    @Override
    protected Object storeEntry(PersistentEntity persistentEntity, EntityAccess entityAccess, Object storeId, EntityAccess entry) {       
        getCassandraTemplate().insert(entityAccess.getEntity());
        return storeId;
    }

    @Override
    protected void updateEntry(PersistentEntity persistentEntity, EntityAccess entityAccess, Object key, EntityAccess entry) {                
        getCassandraTemplate().update(entityAccess.getEntity());
    }

    @Override
    protected void deleteEntries(String family, List<Object> keys) {
        // TODO make this a batch or single call but I'm sleepy so not now.
        for (Object key : keys) {
            deleteEntry(family, key, null);
        }        
    }

    @Override
    protected void deleteEntry(String family, Object key, Object entry) {
        getCassandraTemplate().delete(entry);
    }

    @Override
    protected Object generateIdentifier(PersistentEntity persistentEntity, EntityAccess id) {
        return UUIDs.timeBased(); // TODO review if this is the correct UUID
                                  // type we want.
    }

    private String getKeyspaceName() {
        return getKeyspace(getPersistentEntity().getMapping(), CassandraDatastore.DEFAULT_KEYSPACE);
    }

    @Override
    public Query createQuery() {
        return new CassandraQuery(getCassandraSession(), getPersistentEntity());
    }

    protected String getFamily(PersistentEntity persistentEntity, ClassMapping<Family> cm) {
        String table = null;
        if (cm.getMappedForm() != null) {
            table = cm.getMappedForm().getFamily();
        }
        if (table == null) {
            table = persistentEntity.getJavaClass().getSimpleName();
        }
        log.trace("getFamily: " + table);
        return table;
    }
}
