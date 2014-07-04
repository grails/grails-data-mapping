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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.grails.datastore.mapping.cassandra.CassandraSession;
import org.grails.datastore.mapping.cassandra.config.CassandraMappingContext;
import org.grails.datastore.mapping.cassandra.config.Column;
import org.grails.datastore.mapping.cassandra.config.Table;
import org.grails.datastore.mapping.cassandra.query.CassandraQuery;
import org.grails.datastore.mapping.cassandra.uuid.UUIDUtil;
import org.grails.datastore.mapping.engine.AssociationIndexer;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.grails.datastore.mapping.engine.NativeEntryEntityPersister;
import org.grails.datastore.mapping.engine.PropertyValueIndexer;
import org.grails.datastore.mapping.model.ClassMapping;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.types.Association;
import org.grails.datastore.mapping.proxy.ProxyFactory;
import org.grails.datastore.mapping.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.cassandra.core.CassandraTemplate;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings("rawtypes")
public class CassandraEntityPersister extends NativeEntryEntityPersister<EntityAccess, Object> {

    private static Logger log = LoggerFactory.getLogger(CassandraEntityPersister.class);
    private org.springframework.data.cassandra.mapping.CassandraPersistentEntity<?> springCassandraPersistentEntity;
    private CassandraTemplate cassandraTemplate;
    private ConversionService conversionService;
    public CassandraEntityPersister(MappingContext context, PersistentEntity entity, CassandraSession cassandraSession, ApplicationEventPublisher applicationEventPublisher) {
        super(context, entity, cassandraSession, applicationEventPublisher);
        springCassandraPersistentEntity = ((CassandraMappingContext) context).getSpringCassandraMappingContext().getExistingPersistentEntity(entity.getJavaClass());
        cassandraTemplate = getCassandraTemplate();
        conversionService = cassandraTemplate.getConverter().getConversionService();
    }

    protected CassandraTemplate getCassandraTemplate() {
        return getCassandraSession().getCassandraTemplate();
    }

    protected CassandraSession getCassandraSession() {
        return (CassandraSession) getSession();
    }

    @Override
    public String getEntityFamily() {
        String table = getPersistentEntity().getDecapitalizedName();
        if (table == null) {
            table = getPersistentEntity().getJavaClass().getSimpleName();
        }
        
        return table;
    }

    @Override
    protected boolean doesRequirePropertyIndexing() {
        return false;
    }

    @Override
    protected EntityAccess createEntityAccess(PersistentEntity persistentEntity, Object obj) {
        return new CassandraEntityAccess(persistentEntity, obj);
    }

    @Override
    protected EntityAccess createEntityAccess(PersistentEntity persistentEntity, Object obj, final EntityAccess nativeEntry) {
    	final CassandraEntityAccess ea = new CassandraEntityAccess(persistentEntity, obj);
        ea.setNativeEntry(nativeEntry);
        return ea;
    }

    @Override
    protected EntityAccess createNewEntry(String family) {
        return new CassandraEntityAccess(getPersistentEntity(), getPersistentEntity().newInstance());
    }

    @Override
    protected Object readObjectIdentifier(EntityAccess entityAccess, ClassMapping cm) {
        Table table = (Table) cm.getMappedForm();
        if (table.hasCompositePrimaryKeys()) {
            Map<String, Object> identifier = new HashMap<String, Object>();
            for (Column column : table.getPrimaryKeys()) {
                String name = column.getName();
                identifier.put(name, entityAccess.getProperty(name));
            }
            return identifier;
        } else {
            return entityAccess.getIdentifier();
        }
    }

    @Override
    protected Object readIdentifierFromObject(Object object) {
        EntityAccess entityAccess = createEntityAccess(getPersistentEntity(), object);
        return readObjectIdentifier(entityAccess, getPersistentEntity().getMapping());
    }
    
    @Override
    public Serializable getObjectIdentifier(Object object) {
        if (object == null) return null;
        final ProxyFactory pf = getProxyFactory();
        if (pf.isProxy(object)) {
            return pf.getIdentifier(object);
        }
        return (Serializable) readIdentifierFromObject(object);
    }

    @Override
    protected Object getEntryValue(EntityAccess nativeEntry, String property) {
        return nativeEntry.getProperty(property);
    }

    @Override
    protected void setEntryValue(EntityAccess nativeEntry, String key, Object value) {
        nativeEntry.setProperty(key, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected EntityAccess retrieveEntry(PersistentEntity persistentEntity, String family, Serializable nativeKey) {
    	if (!(nativeKey instanceof Map)) {
            nativeKey = id(persistentEntity.getIdentity().getName(), (Serializable) convertPrimitiveToNative(nativeKey, conversionService));
        } else {
            Table table = (Table) persistentEntity.getMapping().getMappedForm();
            for (Entry<String, Object> entry : ((Map<String, Object>) nativeKey).entrySet()) {
                String property = entry.getKey();
                entry.setValue(convertPrimitiveToNative(entry.getValue(), conversionService));
                if (!table.isPrimaryKey(property)) {
                    throw new IllegalArgumentException(String.format("unknown property [%s] on entity class [%s]", property, persistentEntity.getName()));
                }
            }
        }
        Object entity = cassandraTemplate.selectOneById(persistentEntity.getJavaClass(), nativeKey);
        return entity == null ? null : new CassandraEntityAccess(persistentEntity, entity);
    }

    @Override
    protected Object storeEntry(PersistentEntity persistentEntity, EntityAccess entityAccess, Object storeId, EntityAccess entry) {
        cassandraTemplate.insert(entityAccess.getEntity());
        return storeId;
    }

    @Override
    protected void updateEntry(PersistentEntity persistentEntity, EntityAccess entityAccess, Object key, EntityAccess entry) {
        cassandraTemplate.update(entityAccess.getEntity());
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void deleteEntries(String family, List<Object> keys) {
        for (Object key : keys) {
            if (!(key instanceof Map)) {
                key = id(getPersistentEntity().getIdentity().getName(), (Serializable) key);
            }
            cassandraTemplate.deleteById(getPersistentEntity().getJavaClass(), key);
        }
    }

    @Override
    protected void deleteEntry(String family, Object key, Object entry) {
        cassandraTemplate.delete(entry);
    }

    @Override
    protected Object generateIdentifier(PersistentEntity persistentEntity, EntityAccess entityAccess) {
        UUID id = null;
        Column idColumn = (Column) persistentEntity.getIdentity().getMapping().getMappedForm();
        if (idColumn != null && "timeuuid".equals(idColumn.getType())) {
            id = UUIDUtil.getRandomTimeUUID();
        } else {
            id = UUIDUtil.getRandomUUID();
        }
        entityAccess.setIdentifier(id);
        return id;
    }

    @Override
    public Query createQuery() {
        return new CassandraQuery(getCassandraSession(), getPersistentEntity());
    }

    @Override
    public PropertyValueIndexer getPropertyIndexer(PersistentProperty property) {
        return null;
    }

    @Override
    public AssociationIndexer getAssociationIndexer(EntityAccess nativeEntry, Association association) {
        return null;
    }

    public static Object convertPrimitiveToNative(Object item, ConversionService conversionService) {
        Object nativeValue;
        if (item != null) {             
            TypeDescriptor itemTypeDescriptor = TypeDescriptor.forObject(item);
            Class<?> itemTypeClass = itemTypeDescriptor.getObjectType();
            if (Enum.class.isAssignableFrom(itemTypeClass) && conversionService.canConvert(itemTypeDescriptor, TypeDescriptor.valueOf(String.class))) {
                nativeValue = conversionService.convert(item, String.class);
            } else {               
                nativeValue = item;
            }
        } else {
            nativeValue = null;
        }
        return nativeValue;
    }

    protected class CassandraEntityAccess extends NativeEntryModifyingEntityAccess {

        public CassandraEntityAccess(PersistentEntity persistentEntity, Object entity) {
            super(persistentEntity, entity);
        }
        
        @Override
        public void setProperty(String name, Object value) {
        	final Table table = (Table) classMapping.getMappedForm();
        	if (table.isPrimaryKey(name) && value instanceof Map) {
        		value = ((Map)value).get(name);
        	}
        	//TODO: change back to setPropertyNoConversion and handle version property separately?
        	super.setProperty(name, value);
        }

    }
}
