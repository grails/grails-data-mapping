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

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;
import static org.springframework.data.cassandra.repository.support.BasicMapId.id;
import groovy.lang.MissingPropertyException;

import java.io.Serializable;
import java.net.URL;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import org.grails.datastore.mapping.cassandra.CassandraSession;
import org.grails.datastore.mapping.cassandra.config.Column;
import org.grails.datastore.mapping.cassandra.config.Table;
import org.grails.datastore.mapping.cassandra.query.CassandraQuery;
import org.grails.datastore.mapping.cassandra.utils.UUIDUtil;
import org.grails.datastore.mapping.core.OptimisticLockingException;
import org.grails.datastore.mapping.core.SessionImplementor;
import org.grails.datastore.mapping.core.impl.PendingUpdate;
import org.grails.datastore.mapping.core.impl.PendingUpdateAdapter;
import org.grails.datastore.mapping.engine.*;
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
import org.springframework.cassandra.core.CqlTemplate;
import org.springframework.cassandra.core.SessionCallback;
import org.springframework.cassandra.core.WriteOptions;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.mapping.CassandraPersistentEntity;
import org.springframework.data.cassandra.mapping.CassandraPersistentProperty;
import org.springframework.data.mapping.PropertyHandler;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Update;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class CassandraEntityPersister extends NativeEntryEntityPersister<EntityAccess, Object> {

	private static Logger LOG = LoggerFactory.getLogger(CassandraEntityPersister.class);
	private org.springframework.data.cassandra.mapping.CassandraPersistentEntity<?> springCassandraPersistentEntity;
	private CassandraTemplate cassandraTemplate;
	private ConversionService conversionService;

	public CassandraEntityPersister(MappingContext context, PersistentEntity entity, CassandraSession cassandraSession, ApplicationEventPublisher applicationEventPublisher) {
		super(context, entity, cassandraSession, applicationEventPublisher);
		cassandraTemplate = getCassandraTemplate();
		springCassandraPersistentEntity = cassandraTemplate.getCassandraMappingContext().getExistingPersistentEntity(entity.getJavaClass());
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
	protected EntityAccess createNewEntry(String family, Object instance) {
		return createEntityAccess(getPersistentEntity(), instance);
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
		if (object == null)
			return null;
		final ProxyFactory pf = getProxyFactory();
		if (pf.isProxy(object)) {
			return pf.getIdentifier(object);
		}
		return (Serializable) readIdentifierFromObject(object);
	}

	public Object convertObject(PersistentEntity persistentEntity, Serializable nativeKey, Object object) {
		return createObjectFromNativeEntry(persistentEntity, nativeKey, createEntityAccess(persistentEntity, object));
	}
	
	@Override
	public Object createObjectFromNativeEntry(PersistentEntity persistentEntity, Serializable nativeKey, EntityAccess nativeEntry) {
        persistentEntity = discriminatePersistentEntity(persistentEntity, nativeEntry);

        cacheNativeEntry(persistentEntity, nativeKey, nativeEntry);

        Object obj = nativeEntry.getEntity();
        refreshObjectStateFromNativeEntry(persistentEntity, obj, nativeKey, nativeEntry, false);
        return obj;
    }

	@Override
	protected Object getEntryValue(EntityAccess nativeEntry, String property) {
		return nativeEntry.getProperty(property);
	}

	@Override
	protected void setEntryValue(EntityAccess nativeEntry, String key, Object value) {
		nativeEntry.setProperty(key, value);
	}

	@Override
	protected EntityAccess retrieveEntry(PersistentEntity persistentEntity, String family, Serializable nativeKey) {
		Map idMap = createIdMap(nativeKey, false);
		Object entity = cassandraTemplate.selectOneById(persistentEntity.getJavaClass(), idMap);
		return entity == null ? null : new CassandraEntityAccess(persistentEntity, entity);
	}

	@Override
	protected Object storeEntry(PersistentEntity persistentEntity, EntityAccess entityAccess, Object storeId, EntityAccess entry) {
		Object entity = entityAccess.getEntity();
		WriteOptions writeOptions = getWriteOptions(entity);
		cassandraTemplate.insert(entity, writeOptions);
		return storeId;
	}

	@Override
	protected void updateEntry(final PersistentEntity persistentEntity, final EntityAccess entityAccess, final Object key, final EntityAccess entry) {
		updateEntry(persistentEntity, entityAccess, key, entry, false);
	}

	protected void updateEntry(final PersistentEntity persistentEntity, final EntityAccess entityAccess, final Object key, final EntityAccess entry, final boolean simpleTypesOnly) {
		Object entity = entityAccess.getEntity();
		WriteOptions writeOptions = getWriteOptions(entity);
		final Update update = createUpdate();
		final boolean versioned = isVersioned(entityAccess);
		if (versioned) {
			Object currentVersion = getCurrentVersion(entityAccess);
			incrementVersion(entityAccess);
			if (currentVersion != null) {
				update.onlyIf(QueryBuilder.eq(GormProperties.VERSION, currentVersion));
			}
		}
		springCassandraPersistentEntity.doWithProperties(new PropertyHandler<CassandraPersistentProperty>() {
			@Override
			public void doWithPersistentProperty(CassandraPersistentProperty prop) {
				Object value = entityAccess.getProperty(prop.getName());
				value = convertPrimitiveToNative(value, prop, conversionService);
				if (value != null) {
					if (prop.isIdProperty() || prop.isPrimaryKeyColumn()) {
						update.where(QueryBuilder.eq(prop.getColumnName().toCql(), value));
					} else if (simpleTypesOnly == false || !(prop.isCollectionLike() || prop.isMap())) {
						update.with(QueryBuilder.set(prop.getColumnName().toCql(), value));
					}
				}
			}
		});
		CqlTemplate.addWriteOptions(update, writeOptions);
		cassandraTemplate.execute(new SessionCallback<Object>() {
			@Override
			public Object doInSession(Session s) throws DataAccessException {
				if (LOG.isDebugEnabled()) {
					LOG.debug("executing [{}]", update.toString());
				}
				ResultSet resultSet = s.execute(update);

				if (versioned) {
					Row row = resultSet.one();
					if (row != null && row.getBool("[applied]")) {
						if (LOG.isDebugEnabled()) {
							LOG.debug("Successfully modified entry [{}] to version [{}] ", key, getCurrentVersion(entityAccess));
						}
					} else {
						throw new OptimisticLockingException(persistentEntity, key);
					}
				}
				return null;
			}
		});
	}

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
		final Object identifier = entityAccess.getIdentifier();
		if (identifier != null) {
			return identifier;
		}
		UUID id = null;
		Column idColumn = (Column) persistentEntity.getIdentity().getMapping().getMappedForm();
		if (idColumn != null && "timeuuid".equals(idColumn.getType())) {
			id = UUIDUtil.getRandomTimeUUID();
		} else {
			id = UUIDUtil.getRandomUUID();
		}
		if (UUID.class.isAssignableFrom(persistentEntity.getIdentity().getType())) {
			entityAccess.setIdentifier(id);
			return id;
		}
		
		String stringId = id.toString();
		entityAccess.setIdentifier(stringId);
		return stringId;
	}

	protected Map<String, Serializable> createIdMap(Serializable nativeKey, boolean withColumnName) {
		Map<String, Serializable> idMap = new HashMap<String, Serializable>();
		if (!(nativeKey instanceof Map)) {
			String name = getPersistentEntity().getIdentity().getName();
			CassandraPersistentProperty cassandraPersistentProperty = springCassandraPersistentEntity.getPersistentProperty(name);
			if (cassandraPersistentProperty == null) {
				throwUnknownPrimaryKeyException(name, springCassandraPersistentEntity.getName());
			}
			idMap = id((withColumnName ? cassandraPersistentProperty.getColumnName().toCql() : name), (Serializable) convertPrimitiveToNative(nativeKey, cassandraPersistentProperty, conversionService));
		} else {
			for (Entry<String, Object> entry : ((Map<String, Object>) nativeKey).entrySet()) {
				String name = entry.getKey();
				CassandraPersistentProperty cassandraPersistentProperty = springCassandraPersistentEntity.getPersistentProperty(name);
				if (cassandraPersistentProperty == null || !cassandraPersistentProperty.isPrimaryKeyColumn()) {
					throwUnknownPrimaryKeyException(name, springCassandraPersistentEntity.getName());
				}
				idMap.put((withColumnName ? cassandraPersistentProperty.getColumnName().toCql() : name), (Serializable) convertPrimitiveToNative(entry.getValue(), cassandraPersistentProperty, conversionService));
			}
		}
		return idMap;
	}

	protected WriteOptions getWriteOptions(Object entity) {		
		return getCassandraSession().getCassandraDatastore().getWriteOptions(entity);
	}

	protected String getTableName() {
		return cassandraTemplate.getTableName(getPersistentEntity().getJavaClass()).toCql();
	}
	
	protected Update createUpdate() {
		return QueryBuilder.update(getTableName());
	}

	protected Update prepareUpdate(Serializable id, final Update update, WriteOptions writeOptions) {
		Map<String, Serializable> idMap = createIdMap(id, true);
		for (Entry<String, Serializable> entry : ((Map<String, Serializable>) idMap).entrySet()) {
			update.where(eq(entry.getKey(), entry.getValue()));
		}
		CqlTemplate.addWriteOptions(update, writeOptions);
		return update;
	}
	
	protected void addPendingUpdate(Serializable id, Statement statement) {
		if (id != null && statement != null) {
			PendingUpdate<EntityAccess, Object> pendingUpdate = new CassandraPendingUpdateAdapter<EntityAccess, Object>(getPersistentEntity(), id, statement, cassandraTemplate);
			((SessionImplementor<Object>) session).addPendingUpdate(pendingUpdate);
		}
	}

	protected Delete prepareDelete(Serializable id, final Delete delete) {
		Map<String, Serializable> idMap = createIdMap(id, true);
		for (Entry<String, Serializable> entry : ((Map<String, Serializable>) idMap).entrySet()) {
			delete.where(eq(entry.getKey(), entry.getValue()));
		}		
		return delete;
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

	public Object update(Object obj) {
		return update(obj, false);
	}
	
	public Object updateSingleTypes(Object obj) {
		return update(obj, true);
	}
	
	public Object update(Object obj, final boolean simpleTypesOnly) {
		final PersistentEntity persistentEntity = getPersistentEntity();
		final EntityAccess entityAccess = createEntityAccess(persistentEntity, obj);
		final Object key = readIdentifierFromObject(obj);
		PendingUpdate<EntityAccess, Object> pendingUpdate = new PendingUpdateAdapter<EntityAccess, Object>(persistentEntity, key, entityAccess, entityAccess) {
			public void run() {
				updateEntry(entity, getEntityAccess(), getNativeKey(), getNativeEntry(), simpleTypesOnly);
				firePostUpdateEvent(persistentEntity, entityAccess);
			}
		};
		((SessionImplementor<Object>) session).addPendingUpdate(pendingUpdate);
		return key;
	}	
	
	public void updateProperty(Serializable id, String propertyName, Object item, WriteOptions writeOptions) {
		Statement statement = prepareUpdateProperty(id, propertyName, item, writeOptions);
		addPendingUpdate(id, statement);
	}
	
	public Statement prepareUpdateProperty(Serializable id, String propertyName, Object item, WriteOptions writeOptions) {
		final Update update = createUpdate();
		CassandraPersistentProperty persistentProperty = getPersistentProperty(springCassandraPersistentEntity, propertyName);
		String columnName = getPropertyName(persistentProperty);
		item = convertPrimitiveToNative(item, persistentProperty, conversionService);
		update.with(set(columnName, item));
		return prepareUpdate(id, update, writeOptions);		
	}
	
	public void updateProperties(Serializable id, Map<String, Object> properties, WriteOptions writeOptions) {
		Statement statement = prepareUpdateProperties(id, properties, writeOptions);
		addPendingUpdate(id, statement);
	}
	
	public Statement prepareUpdateProperties(Serializable id, Map<String, Object> properties, WriteOptions writeOptions) {
		final Update update = createUpdate();
		for (Entry<String, Object> property : properties.entrySet()) {
    		CassandraPersistentProperty persistentProperty = getPersistentProperty(springCassandraPersistentEntity, property.getKey());
    		String columnName = getPropertyName(persistentProperty);
    		Object item = convertPrimitiveToNative(property.getValue(), persistentProperty, conversionService);
    		update.with(set(columnName, item));
		}
		return prepareUpdate(id, update, writeOptions);		
	}
	
	public void append(Object obj, String propertyName, Object element, WriteOptions writeOptions) {
		Serializable id = (Serializable) readIdentifierFromObject(obj);
		append(id, propertyName, element, writeOptions);
	}
	
	public void append(Serializable id, String propertyName, Object element, WriteOptions writeOptions) {
		Statement statement = prepareAppend(id, propertyName, element, writeOptions);
		addPendingUpdate(id, statement);
	}
	
	public Statement prepareAppend(Serializable id, String propertyName, Object element, WriteOptions writeOptions) {
		final Update update = createUpdate();
		CassandraPersistentProperty persistentProperty = getPersistentProperty(springCassandraPersistentEntity, propertyName);
		String columnName = getPropertyName(persistentProperty);
		final Class<?> type = persistentProperty.getType();
		
		if (Set.class.isAssignableFrom(type)) {
			if (element instanceof Set<?>) {
				Set<?> set = (Set<?>) element;
				if (set.size() > 0) {
					update.with(QueryBuilder.addAll(columnName, set));
				}
			} else {
				update.with(QueryBuilder.add(columnName, element));
			}
		} else if (List.class.isAssignableFrom(type)) {
			if (element instanceof List<?>) {
				List<?> list = (List<?>) element;
				if (list.size() > 0) {
					update.with(QueryBuilder.appendAll(columnName, list));
				}
			} else {
				update.with(QueryBuilder.append(columnName, element));
			}
		} else if (Map.class.isAssignableFrom(type)) {
			if (element instanceof Map<?, ?>) {
				Map<?, ?> map = (Map<?, ?>) element;
				if (map.size() > 0) {
					update.with(QueryBuilder.putAll(columnName, map));
				}
			}
		}

		return prepareUpdate(id, update, writeOptions);
	}

	public void prepend(Object obj, String propertyName, Object element, WriteOptions writeOptions) {
		Serializable id = (Serializable) readIdentifierFromObject(obj);
		prepend(id, propertyName, element, writeOptions);
	}
	
	public void prepend(Serializable id, String propertyName, Object element, WriteOptions writeOptions) {
		Statement statement = preparePrepend(id, propertyName, element, writeOptions);
		addPendingUpdate(id, statement);
	}
	
	public Statement preparePrepend(Serializable id, String propertyName, Object element, WriteOptions writeOptions) {
		final Update update = createUpdate();
		CassandraPersistentProperty persistentProperty = getPersistentProperty(springCassandraPersistentEntity, propertyName);
		String columnName = getPropertyName(persistentProperty);

		if (List.class.isAssignableFrom(persistentProperty.getType())) {
			if (element instanceof List<?>) {
				List<?> list = (List<?>) element;
				if (list.size() > 0) {
					update.with(QueryBuilder.prependAll(columnName, list));
				}
			} else {
				update.with(QueryBuilder.prepend(columnName, element));
			}
		}

		return prepareUpdate(id, update, writeOptions);
	}

	public void replaceAt(Object obj, String propertyName, int index, Object element, WriteOptions writeOptions) {
		Serializable id = (Serializable) readIdentifierFromObject(obj);
		replaceAt(id, propertyName, index, element, writeOptions);
	}
	
	public void replaceAt(Serializable id, String propertyName, int index, Object element, WriteOptions writeOptions) {
		Statement statement = prepareReplaceAt(id, propertyName, index, element, writeOptions);
		addPendingUpdate(id, statement);
	}

	public Statement prepareReplaceAt(Serializable id, String propertyName, int index, Object element, WriteOptions writeOptions) {
		final Update update = createUpdate();
		CassandraPersistentProperty persistentProperty = getPersistentProperty(springCassandraPersistentEntity, propertyName);
		String columnName = getPropertyName(persistentProperty);

		if (List.class.isAssignableFrom(persistentProperty.getType())) {			
			update.with(QueryBuilder.setIdx(columnName, index, element));			
		}

		return prepareUpdate(id, update, writeOptions);
	}
	
	public void deleteFrom(Object obj, String propertyName, Object item, boolean isIndex, WriteOptions writeOptions) {
		Serializable id = (Serializable) readIdentifierFromObject(obj);
		deleteFrom(id, propertyName, item, isIndex, writeOptions);
	}
	
	public void deleteFrom(Serializable id, String propertyName, Object item, boolean isIndex, WriteOptions writeOptions) {
		Statement statement = prepareDeleteFrom(id, propertyName, item, isIndex, writeOptions);
		addPendingUpdate(id, statement);
	}

	public Statement prepareDeleteFrom(Serializable id, String propertyName, Object item, boolean isIndex, WriteOptions writeOptions) {
		Update update = null;
		Delete delete = null;
		CassandraPersistentProperty persistentProperty = getPersistentProperty(springCassandraPersistentEntity, propertyName);
		String columnName = getPropertyName(persistentProperty);		
		final Class<?> type = persistentProperty.getType();
		
		if (isIndex) {
			if (List.class.isAssignableFrom(type)) {
				delete = QueryBuilder.delete().listElt(columnName, (Integer) item).from(getTableName());
			}
		} else {
    		if (Set.class.isAssignableFrom(type)) {
    			update = createUpdate();
    			if (item instanceof Set<?>) {
    				Set<?> set = (Set<?>) item;
    				if (set.size() > 0) {
    					update.with(QueryBuilder.removeAll(columnName, set));
    				}
    			} else {
    				update.with(QueryBuilder.remove(columnName, item));
    			}
    		} else if (List.class.isAssignableFrom(type)) {
    			update = createUpdate();
    			if (item instanceof List<?>) {
    				List<?> list = (List<?>) item;
    				if (list.size() > 0) {
    					update.with(QueryBuilder.discardAll(columnName, list));
    				}
    			} else {
    				update.with(QueryBuilder.discard(columnName, item));
    			}
    		} else if (Map.class.isAssignableFrom(type)) {
    			delete = QueryBuilder.delete().mapElt(columnName, item).from(getTableName());
    		}
		}
		
		if (update != null) {
			return prepareUpdate(id, update, writeOptions);
		}
		if (delete != null) {
			return prepareDelete(id, delete);
		} 
		return null;
	}	

	public static Object convertPrimitiveToNative(Object item, CassandraPersistentProperty cassandraPersistentProperty, ConversionService conversionService) {
		Object nativeValue = item;
		if (item != null) {
			TypeDescriptor itemTypeDescriptor = TypeDescriptor.forObject(item);
			Class<?> itemTypeClass = itemTypeDescriptor.getObjectType();
			if (Enum.class.isAssignableFrom(itemTypeClass) && conversionService.canConvert(itemTypeDescriptor, TypeDescriptor.valueOf(String.class))) {
				nativeValue = conversionService.convert(item, String.class);
			} else if (Currency.class.isAssignableFrom(itemTypeClass) || Locale.class.isAssignableFrom(itemTypeClass) || TimeZone.class.isAssignableFrom(itemTypeClass) || URL.class.isAssignableFrom(itemTypeClass)) {
				nativeValue = conversionService.convert(item, String.class);
			} else {
				if (cassandraPersistentProperty != null && !cassandraPersistentProperty.getType().isAssignableFrom(itemTypeClass)) {
					TypeDescriptor targetTypeDescriptor = TypeDescriptor.valueOf(cassandraPersistentProperty.getType());
					if (conversionService.canConvert(itemTypeDescriptor, targetTypeDescriptor)) {
						try {
							nativeValue = conversionService.convert(item, itemTypeDescriptor, targetTypeDescriptor);
						} catch (Exception e) {
							throw new IllegalArgumentException(String.format("Failed to convert property [%s] on entity class [%s]: [%s]", cassandraPersistentProperty.getName(), cassandraPersistentProperty.getOwner().getName(), e.getMessage()), e);
						}
					}
				}

			}
		}
		return nativeValue;
	}

	public static String getPropertyName(CassandraPersistentEntity<?> cassandraPersistentEntity, String propertyName) {
		CassandraPersistentProperty property = getPersistentProperty(cassandraPersistentEntity, propertyName);
		return getPropertyName(property);
	}

	public static String getPropertyName(CassandraPersistentProperty property) {
		return property.getColumnName().toCql();
	}

	public static CassandraPersistentProperty getPersistentProperty(CassandraPersistentEntity<?> cassandraPersistentEntity, String propertyName) {
		CassandraPersistentProperty property = cassandraPersistentEntity.getPersistentProperty(propertyName);
		if (property == null) {
			throw new MissingPropertyException(propertyName, cassandraPersistentEntity.getType());
		}
		return property;
	}

	private static void throwUnknownPrimaryKeyException(String name, String entity) {
		throw new MissingPropertyException(String.format("No such primary key property: %s for entity class: %s", name, entity));
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
		public void setIdentifierNoConversion(Object id) {
			setIdentifier(id);
		}

		@Override
		public void setProperty(String name, Object value) {
			final Table table = (Table) classMapping.getMappedForm();
			if (table.isPrimaryKey(name) && value instanceof Map) {
				value = ((Map) value).get(name);
			}
			Class type = getPropertyType(name);
			if (type.isEnum() || GormProperties.VERSION.equals(name)) {
				super.setProperty(name, value);
			} else {
				super.setPropertyNoConversion(name, value);
			}
		}
	}
}
