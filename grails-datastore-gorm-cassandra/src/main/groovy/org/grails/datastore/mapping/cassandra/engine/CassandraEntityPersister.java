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

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.*;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Update;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.grails.datastore.mapping.keyvalue.engine.AbstractKeyValueEntityPersister;
import org.grails.datastore.mapping.keyvalue.mapping.config.Family;
import org.grails.datastore.mapping.model.types.Association;
import org.grails.datastore.mapping.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.grails.datastore.mapping.cassandra.CassandraDatastore;
import org.grails.datastore.mapping.cassandra.CassandraSession;
import org.grails.datastore.mapping.engine.AssociationIndexer;
import org.grails.datastore.mapping.engine.PropertyValueIndexer;
import org.grails.datastore.mapping.keyvalue.engine.KeyValueEntry;
import org.grails.datastore.mapping.model.ClassMapping;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
public class CassandraEntityPersister extends AbstractKeyValueEntityPersister<KeyValueEntry, Object> {

	private static Logger log = LoggerFactory.getLogger(CassandraEntityPersister.class);

	private Session session;
	private CassandraSession cassandraSession;
	private static final byte[] ZERO_LENGTH_BYTE_ARRAY = new byte[0];

	public CassandraEntityPersister(MappingContext context, PersistentEntity entity, CassandraSession conn, Session session, ApplicationEventPublisher applicationEventPublisher) {
		super(context, entity, conn, applicationEventPublisher);
		this.session = session;
		this.cassandraSession = conn;
	}

	@Override
	protected String getNativePropertyKey(PersistentProperty prop) {
		//To C* all column names are lowercase
		return super.getNativePropertyKey(prop).toLowerCase();
	}

	@Override
	public AssociationIndexer getAssociationIndexer(KeyValueEntry nativeEntry, Association association) {
		//        return new CassandraAssociationIndexer(cassandraClient, association, getKeyspaceName());
		return null;
	}

	@Override
	public PropertyValueIndexer getPropertyIndexer(PersistentProperty property) {
		return null;
	}

	@Override
	protected KeyValueEntry createNewEntry(String family) {
		return new KeyValueEntry(family);
	}

	@Override
	protected Object getEntryValue(KeyValueEntry nativeEntry, String property) {
		return nativeEntry.get(property);
	}

	@Override
	protected void setEntryValue(KeyValueEntry nativeEntry, String key, Object value) {
		nativeEntry.put(key, value);
	}

	@Override
	protected KeyValueEntry retrieveEntry(PersistentEntity persistentEntity, String family, Serializable nativeKey) {
		log.debug("retrieveEntry");
		final ClassMapping cm = persistentEntity.getMapping();
		final String keyspaceName = getKeyspace(cm, CassandraDatastore.DEFAULT_KEYSPACE);

		Statement stmt = QueryBuilder.select().all().from(keyspaceName, family).where(QueryBuilder.eq(persistentEntity.getIdentity().getName(), convertToCassandraType(nativeKey)));
		ResultSet rs = session.execute(stmt);

		KeyValueEntry entry = null;
		if (rs.getAvailableWithoutFetching() == 1) {
			entry = new KeyValueEntry(family);
			Row row = rs.one();
			entry = rowToKeyValueEntry(row, family);
		}
/*
Possible other way
		KeyValueEntry entry;
        Row row = rs.one(); //This doesn't make sure we got only one result
        if (row != null) { entry = rowToKeyValueEntry(row, family); }
        else { entry = new KeyValueEntry(family); } //Don't want to return anything if nothing was found
*/

		return entry;
	}

	@Override
	protected Object storeEntry(PersistentEntity persistentEntity, EntityAccess entityAccess, Object storeId, KeyValueEntry entry) {
		log.debug("StoreEntry");
		//		UUID uuid = (UUID)storeId;
		final ClassMapping cm = persistentEntity.getMapping();
		final String keyspaceName = getKeyspace(cm, CassandraDatastore.DEFAULT_KEYSPACE);
		String family = getFamily(persistentEntity, persistentEntity.getMapping());
		log.debug("storeEntry: family={}", family);
		Insert insert = QueryBuilder.insertInto(keyspaceName, family);

		//Include identity
		insert.value(persistentEntity.getIdentity().getName(), convertToCassandraType(storeId));

		//        log.debug("storeEntry: id->UUID:{}", storeId.toString());

		for (String prop : entry.keySet()) {
			if (!prop.equalsIgnoreCase("id")) { //Filter out ID in the case of mapping to a different id column
				log.debug("storeEntry: {}->{}", prop, entry.get(prop));
				insert.value(prop, convertToCassandraType(entry.get(prop)));
			}
		}

		log.debug("After session execute insert " + insert.toString());

		ResultSet rs = session.execute(insert);

		log.debug(rs.getExecutionInfo().toString());
		return storeId;
	}

	@Override
	protected void updateEntry(PersistentEntity persistentEntity, EntityAccess entityAccess, Object key, KeyValueEntry entry) {
		final ClassMapping cm = getPersistentEntity().getMapping();
		final String keyspaceName = getKeyspace(cm, CassandraDatastore.DEFAULT_KEYSPACE);
		final String family = getFamily(persistentEntity, persistentEntity.getMapping());

		Update.Assignments updateAssignments = QueryBuilder.update(keyspaceName, family).with();
		for (PersistentProperty prop : persistentEntity.getPersistentProperties()) {
			if (!prop.getName().equalsIgnoreCase("id")) { //Filter out ID in the case of mapping to a different id column
				log.info("Update set: " + prop.getName() + " to " + entry.get(prop.getName()));
				updateAssignments = updateAssignments.and(QueryBuilder.set(prop.getName(), convertToCassandraType(entry.get(prop.getName()))));
			}
		}

		Statement update = updateAssignments.where(QueryBuilder.eq(persistentEntity.getIdentity().getName(), convertToCassandraType(key)));
		log.info("Running Update: " + update.toString());
		session.execute(update);
	}

	@Override
	protected void deleteEntries(String family, List<Object> keys) {
		//TODO make this a batch or single call
		for (Object key : keys) {
			deleteEntry(family, key, null);
		}
	}

	@Override
	protected void deleteEntry(String family, Object key, Object entry) {
		final ClassMapping cm = getPersistentEntity().getMapping();
		final String keyspaceName = getKeyspace(cm, CassandraDatastore.DEFAULT_KEYSPACE);

		Statement stmt = QueryBuilder.delete().all().from(keyspaceName, family).where(QueryBuilder.eq(getPersistentEntity().getIdentity().getName(), convertToCassandraType(key)));
		session.execute(stmt);
	}

	@Override
	protected Object generateIdentifier(PersistentEntity persistentEntity, KeyValueEntry id) {
		return UUID.randomUUID();
	}

	private String getKeyspaceName() {
		return getKeyspace(getPersistentEntity().getMapping(), CassandraDatastore.DEFAULT_KEYSPACE);
	}

	@Override
	public Query createQuery() {
		return new CassandraQuery(cassandraSession, getPersistentEntity());
	}

	protected String getFamily(PersistentEntity persistentEntity, ClassMapping<Family> cm) {
		String table = null;
		if (cm.getMappedForm() != null) {
			table = cm.getMappedForm().getFamily();
		}
		if (table == null) { table = persistentEntity.getJavaClass().getSimpleName(); }
		log.trace("getFamily: " + table);
		return table;
	}

	private Object convertToCassandraType(Object object) {
		//TODO refactor into extensible approach with custom types ala hibernate
		if (object == null) {
			return null;
		}
		Class clazz = object.getClass();

		Object o = object;
		if (clazz == byte[].class) {
			o = ByteBuffer.wrap((byte[])object);
		} else if (clazz.isEnum()) {
			o = object.toString();
		} else if (clazz.equals(Long.class)) {
			o = Long.class.cast(object).longValue();
		} else if (clazz.equals(Float.class)) {
			o = Float.class.cast(object).floatValue();
		} else if (clazz.equals(Double.class)) {
			o = Double.class.cast(object).doubleValue();
		} else if (clazz.equals(Byte.class)) {
			o = Byte.class.cast(object).intValue();
		} else if (clazz.equals(Short.class)) {
			o = Short.class.cast(object).intValue();
		} else if (clazz.equals(Boolean.class)) {
			o = Boolean.class.cast(object).booleanValue();
		} else if (clazz.equals(URI.class) ||
			clazz.equals(URL.class) ||
			TimeZone.class.isAssignableFrom(clazz) ||
			clazz.equals(Currency.class) ||
			clazz.equals(Locale.class)
			) {
			o = object.toString();
		} else if (Calendar.class.isAssignableFrom(clazz)) {
			o = ((Calendar)object).getTime();
		} else if (!cassandraNativeSupport(clazz)) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				ObjectOutputStream oos = new ObjectOutputStream(baos);
				oos.writeObject(object);
			} catch (IOException e) {
				e.printStackTrace();
			}
			o = ByteBuffer.wrap(baos.toByteArray());
		}

		return o;
	}

	private boolean cassandraNativeSupport(Class c) {
		String cassandraType = "blob";
		if (c.equals(String.class)) {
			cassandraType = "text";
		} else if (c.equals(long.class) || c.equals(Long.class)) {
			cassandraType = "bigint";
		} else if (c.equals(ByteBuffer.class)) {
			cassandraType = "blob";
		} else if (c.equals(boolean.class) || c.equals(Boolean.class)) {
			cassandraType = "boolean";
		} else if (c.equals(BigDecimal.class)) {
			cassandraType = "decimal";
		} else if (c.equals(double.class)) {
			cassandraType = "double";
		} else if (c.equals(float.class)) {
			cassandraType = "float";
		} else if (c.equals(int.class) || c.equals(Integer.class) || c.equals(short.class) || c.equals(Short.class) || c.equals(byte.class) || c.equals(Byte.class)) {
			cassandraType = "int";
		} else if (List.class.isAssignableFrom(c)) {
			cassandraType = "list<text>";
		} else if (c.equals(Map.class)) {
			cassandraType = "map<text,text>";
		} else if (c.equals(Set.class)) {
			cassandraType = "set<text>";
		} else if (c.equals(String.class)) {
			cassandraType = "text";
		} else if (c.equals(Date.class)) {
			cassandraType = "timestamp";
		} else if (c.equals(UUID.class)) {
			cassandraType = "uuid";
		} else if (c.equals(BigInteger.class)) {
			cassandraType = "varint";
		}

		return !cassandraType.equalsIgnoreCase("blob");
	}

	public KeyValueEntry rowToKeyValueEntry(Row row, String family) {
		log.debug("rowToKeyValueEntry: " + row);
		KeyValueEntry entry = new KeyValueEntry(family);
		ColumnDefinitions columns = row.getColumnDefinitions();
		for (ColumnDefinitions.Definition definition : columns) {
			String columnName = definition.getName();
			DataType dt = definition.getType();
			ByteBuffer columnUnsafeBytes = row.getBytesUnsafe(columnName);
			Object o = null;
			if (columnUnsafeBytes != null) {
				o = dt.deserialize(columnUnsafeBytes);
			}

			log.debug(columnName + ">" + dt.getName() + ": " + dt.getName().equals(DataType.Name.BLOB));

			if (dt.getName().equals(DataType.Name.BLOB)) {

				try {
					ByteBuffer bb = row.getBytes(columnName);
					byte[] result = new byte[bb.remaining()];
					bb.get(result);
					ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(result);
					ObjectInputStream ois = new ObjectInputStream(byteArrayInputStream);
					o = ois.readObject();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
			entry.put(columnName, o);
		}

		if (entry.isEmpty()) {
			entry = null;
		}
		return entry;
	}

}
