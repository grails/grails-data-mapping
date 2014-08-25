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
import java.net.URL;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.UUID;

import org.grails.datastore.mapping.cassandra.CassandraSession;
import org.grails.datastore.mapping.cassandra.config.CassandraMappingContext;
import org.grails.datastore.mapping.cassandra.config.Column;
import org.grails.datastore.mapping.cassandra.config.Table;
import org.grails.datastore.mapping.cassandra.query.CassandraQuery;
import org.grails.datastore.mapping.cassandra.uuid.UUIDUtil;
import org.grails.datastore.mapping.core.OptimisticLockingException;
import org.grails.datastore.mapping.engine.AssociationIndexer;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.grails.datastore.mapping.engine.NativeEntryEntityPersister;
import org.grails.datastore.mapping.engine.PropertyValueIndexer;
import org.grails.datastore.mapping.model.ClassMapping;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.config.GormProperties;
import org.grails.datastore.mapping.model.types.Association;
import org.grails.datastore.mapping.proxy.ProxyFactory;
import org.grails.datastore.mapping.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cassandra.core.SessionCallback;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.mapping.CassandraPersistentProperty;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Update;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings("rawtypes")
public class CassandraEntityPersister extends NativeEntryEntityPersister<EntityAccess, Object> {

    private static Logger LOG = LoggerFactory.getLogger(CassandraEntityPersister.class);
    private org.springframework.data.cassandra.mapping.CassandraPersistentEntity<?> springCassandraPersistentEntity;
    private CassandraTemplate cassandraTemplate;
    private ConversionService conversionService;
    public CassandraEntityPersister(MappingContext context, PersistentEntity entity, CassandraSession cassandraSession, ApplicationEventPublisher applicationEventPublisher) {
        super(context, entity, cassandraSession, applicationEventPublisher);
        springCassandraPersistentEntity = ((CassandraMappingContext) context).getSpringCassandraMappingContext().getExistingPersistentEntity(entity.getJavaClass());
        cassandraTemplate = getCassandraTemplate();
        conversionService = context.getConversionService();
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
    	    String name = persistentEntity.getIdentity().getName();
    	    CassandraPersistentProperty cassandraPersistentProperty = springCassandraPersistentEntity.getPersistentProperty(name);  
    	    if (cassandraPersistentProperty == null) {
    	        throwUnknownPrimaryKeyException(name);
    	    }
            nativeKey = id(name, (Serializable) convertPrimitiveToNative(nativeKey, cassandraPersistentProperty, conversionService));
        } else {
            Table table = (Table) persistentEntity.getMapping().getMappedForm();            
            for (Entry<String, Object> entry : ((Map<String, Object>) nativeKey).entrySet()) {
                String name = entry.getKey();
                CassandraPersistentProperty cassandraPersistentProperty = springCassandraPersistentEntity.getPersistentProperty(name);
                if (cassandraPersistentProperty == null || !table.isPrimaryKey(name)) {
                    throwUnknownPrimaryKeyException(name);
                }                                                                
                entry.setValue(convertPrimitiveToNative(entry.getValue(), cassandraPersistentProperty, conversionService));                
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
    protected void updateEntry(final PersistentEntity persistentEntity, final EntityAccess entityAccess, final Object key, final EntityAccess entry) {       
        Object entity = entityAccess.getEntity();          
        if (isVersioned(entityAccess)) {
            Object currentVersion = getCurrentVersion(entityAccess);
            //TODO: bug in Spring Cassandra throwing NPE in exception translation. Throw OptimisticLockingException directly in SessionCallback below once fixed.
            final Boolean[] optimisticLockException = {false};        
            incrementVersion(entityAccess);
            if (currentVersion != null) {
                final Update update = CassandraTemplate.toUpdateQuery(cassandraTemplate.getTableName(entity.getClass()).toCql(), entity, null, cassandraTemplate.getConverter());
                update.onlyIf(QueryBuilder.eq(GormProperties.VERSION, currentVersion));
                cassandraTemplate.execute(new SessionCallback<Object>() {
                    @Override
                    public Object doInSession(Session s) throws DataAccessException {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Cassandra Update: " + update.toString());
                        }
                        ResultSet resultSet = s.execute(update);
                        Row row = resultSet.one();

                        if (row.getBool("[applied]")) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Successfully modified entry " + key + " to version " + getCurrentVersion(entityAccess));
                            }
                        } else {
                            optimisticLockException[0] = true;
                            
                        }
                        return null;
                    }
                });
            }
            if (optimisticLockException[0]) {
                throw new OptimisticLockingException(persistentEntity, key);
            }
            return;
        }
        cassandraTemplate.update(entity);
        
    }
    
    protected Object getCurrentVersion(final EntityAccess ea) {
        Object currentVersion = ea.getProperty(GormProperties.VERSION);
        if (Number.class.isAssignableFrom(ea.getPropertyType(GormProperties.VERSION))) {
            currentVersion = currentVersion != null ? ((Number)currentVersion).longValue() : currentVersion;
        }
        return currentVersion;
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
    public void setObjectIdentifier(Object obj, Serializable id) {
        new CassandraEntityAccess(getPersistentEntity(), obj).setIdentifier(id);
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

    public static Object convertPrimitiveToNative(Object item, CassandraPersistentProperty cassandraPersistentProperty, ConversionService conversionService) {
        Object nativeValue = item;
        if (item != null) {             
            TypeDescriptor itemTypeDescriptor = TypeDescriptor.forObject(item);
            Class<?> itemTypeClass = itemTypeDescriptor.getObjectType();
            if (Enum.class.isAssignableFrom(itemTypeClass) && conversionService.canConvert(itemTypeDescriptor, TypeDescriptor.valueOf(String.class))) {
                nativeValue = conversionService.convert(item, String.class);
            }
            else if (Currency.class.isAssignableFrom(itemTypeClass) || Locale.class.isAssignableFrom(itemTypeClass) || 
                    TimeZone.class.isAssignableFrom(itemTypeClass) || URL.class.isAssignableFrom(itemTypeClass)) {
                nativeValue = conversionService.convert(item, String.class);    
            } else {                  
                if (cassandraPersistentProperty != null && !cassandraPersistentProperty.getType().isAssignableFrom(itemTypeClass)) {
                    TypeDescriptor targetTypeDescriptor = TypeDescriptor.valueOf(cassandraPersistentProperty.getType());      
                    if (conversionService.canConvert(itemTypeDescriptor, targetTypeDescriptor)) {     
                        try {
                            nativeValue = conversionService.convert(item, itemTypeDescriptor, targetTypeDescriptor);
                        } catch (Exception e) {
                            throw new IllegalArgumentException(String.format("Failed to convert property [%s] on entity class [%s]: [%s]", 
                                    cassandraPersistentProperty.getName(), cassandraPersistentProperty.getOwner().getName(), e.getMessage()), e);
                        }
                    }
                }
                
            }
        } 
        return nativeValue;
    }
    
    private void throwUnknownPrimaryKeyException(String name) {
        throw new IllegalArgumentException(String.format("Unknown primary key property [%s] on entity class [%s]", name, getPersistentEntity().getName()));
    }
    
    protected class CassandraEntityAccess extends NativeEntryModifyingEntityAccess {

        public CassandraEntityAccess(PersistentEntity persistentEntity, Object entity) {
            super(persistentEntity, entity);
        }
        
        @Override
        public void setIdentifier(Object id) {
            final Table table = (Table) classMapping.getMappedForm();
            if (table.hasCompositePrimaryKeys() && id instanceof Map) {
                Map idMap = (Map) id;
                for (String primaryKeyName : table.getPrimaryKeyNames()) {
                    Object value = idMap.get(primaryKeyName);
                    if (value != null) {
                        setProperty(primaryKeyName, value);
                    }
                }
            } else {
                super.setIdentifier(id);
            }
        }
        
        @Override
        public void setProperty(String name, Object value) {
            final Table table = (Table) classMapping.getMappedForm();
            if (table.isPrimaryKey(name) && value instanceof Map) {
                value = ((Map)value).get(name);
            }
            Class type = getPropertyType(name);
            if(type.isEnum() || GormProperties.VERSION.equals(name)) {
                super.setProperty(name, value);    
            } else {
                super.setPropertyNoConversion(name, value);
            }            
        }
    }
}
