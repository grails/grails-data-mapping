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
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
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
import org.springframework.context.ApplicationEventPublisher;
import org.grails.datastore.mapping.cassandra.CassandraDatastore;
import org.grails.datastore.mapping.cassandra.CassandraSession;
import org.grails.datastore.mapping.cassandra.uuid.UUIDUtil;
import org.grails.datastore.mapping.engine.AssociationIndexer;
import org.grails.datastore.mapping.engine.PropertyValueIndexer;
import org.grails.datastore.mapping.keyvalue.engine.KeyValueEntry;
import org.grails.datastore.mapping.model.ClassMapping;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;

import javax.persistence.Column;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
public class CassandraEntityPersister extends AbstractKeyValueEntityPersister<KeyValueEntry, Object> {

	private Session session;
	private static final byte[] ZERO_LENGTH_BYTE_ARRAY = new byte[0];

	public CassandraEntityPersister(MappingContext context, PersistentEntity entity, CassandraSession conn, Session session, ApplicationEventPublisher applicationEventPublisher) {
		super(context, entity, conn, applicationEventPublisher);
		this.session = session;
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
		if (value != null) {
			nativeEntry.put(key, value);
		}
	}

	@Override
	protected KeyValueEntry retrieveEntry(PersistentEntity persistentEntity, String family, Serializable nativeKey) {
		System.out.println("retrieveEntry");
		final ClassMapping cm = getPersistentEntity().getMapping();
		final String keyspaceName = getKeyspace(cm, CassandraDatastore.DEFAULT_KEYSPACE);

		//TODO review Native Key string conversion
		Statement stmt = QueryBuilder.select().all().from(keyspaceName, family).where(QueryBuilder.eq("id", UUID.fromString(nativeKey.toString())));
		ResultSet rs = session.execute(stmt);

		KeyValueEntry entry = new KeyValueEntry(family);
		if (rs.getAvailableWithoutFetching() == 1) {
			Row row = rs.one();
			ColumnDefinitions columns = row.getColumnDefinitions();
			for (ColumnDefinitions.Definition definition : columns) {
				String columnName = definition.getName();
				DataType dt = definition.getType();
				Object o = dt.deserialize(row.getBytesUnsafe(columnName));
				System.out.println(columnName+">"+dt.getName()+": "+dt.getName().equals(DataType.Name.BLOB));
				if (dt.getName().equals(DataType.Name.BLOB)) {

					try {
						ByteBuffer bb = row.getBytes(columnName);
						byte[] result = new byte[bb.remaining()];
						bb.get(result);
						ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(result);
						ObjectInputStream ois = new ObjectInputStream(byteArrayInputStream);
						o = ois.readObject();
						System.out.println(o.getClass()+":"+o);
					} catch (IOException e) {
						e.printStackTrace();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}
				entry.put(columnName, o);
			}
		}

		if (entry.isEmpty()) {
			return null;
		}

		return entry;
	}

	@Override
	protected Object storeEntry(PersistentEntity persistentEntity, EntityAccess entityAccess, Object storeId, KeyValueEntry entry) {
		System.out.println("StoreEntry");
		UUID uuid = (UUID)storeId;
		final ClassMapping cm = getPersistentEntity().getMapping();
		final String keyspaceName = getKeyspace(cm, CassandraDatastore.DEFAULT_KEYSPACE);
		String family = getFamily(persistentEntity, getPersistentEntity().getMapping());
		System.out.println(family);
		Insert insert = QueryBuilder.insertInto(keyspaceName, family);

		//Include id
		insert = insert.value("id", uuid);

		System.out.println(byte[].class);

		for (String prop : entry.keySet()) {
			System.out.println(prop + "->" + entry.get(prop) + ":" + entry.get(prop).getClass());

			insert = insert.value(prop, convertToCassandraType(entry.get(prop)));
		}

		System.out.println("After session execute insert " + insert.toString());

		ResultSet rs = session.execute(insert);

		System.out.println(rs.getExecutionInfo());
		return uuid;
	}

	@Override
	protected void updateEntry(PersistentEntity persistentEntity, EntityAccess entityAccess, Object id, KeyValueEntry entry) {
		final ClassMapping cm = getPersistentEntity().getMapping();
		final String keyspaceName = getKeyspace(cm, CassandraDatastore.DEFAULT_KEYSPACE);
		final String family = getFamily(persistentEntity, persistentEntity.getMapping());

		UUID uuid = (UUID)id;

		Update.Assignments updateAssignments = QueryBuilder.update(keyspaceName, family).with();
		for (String prop : entry.keySet()) {
			updateAssignments = updateAssignments.and(QueryBuilder.set(prop, convertToCassandraType(entry.get(prop))));
		}

		Statement update = updateAssignments.where(QueryBuilder.eq("id", UUID.fromString(uuid.toString())));

		session.execute(update);
	}

	@Override
	protected void deleteEntries(String family, List<Object> keys) {
		//TODO make this a batch or single call but I'm sleepy so not now.
		for (Object key : keys) {
			deleteEntry(family, key, null);
		}
	}

	@Override
	protected void deleteEntry(String family, Object key, Object entry) {
		final ClassMapping cm = getPersistentEntity().getMapping();
		final String keyspaceName = getKeyspace(cm, CassandraDatastore.DEFAULT_KEYSPACE);

		Statement stmt = QueryBuilder.delete().all().from(keyspaceName, family).where(QueryBuilder.eq("id", key));
		session.execute(stmt);
	}

	@Override
	protected Object generateIdentifier(PersistentEntity persistentEntity, KeyValueEntry id) {
		return UUID.randomUUID(); //TODO review if this is the correct UUID type we want.
	}

	private String getKeyspaceName() {
		return getKeyspace(getPersistentEntity().getMapping(), CassandraDatastore.DEFAULT_KEYSPACE);
	}

	@Override
	public Query createQuery() {
		return null; //TODO implement createQuery
	}

	protected String getFamily(PersistentEntity persistentEntity, ClassMapping<Family> cm) {
		//TODO make this something good
		String table = null;
		if (cm.getMappedForm() != null) {
			table = cm.getMappedForm().getFamily();
		}
		System.out.println(table);
		//if (table == null)
		table = persistentEntity.getJavaClass().getSimpleName();
		System.out.println("in getFamily: " + table);
		return table;
	}

	private Object convertToCassandraType(Object object) {
		Class clazz = object.getClass();

		Object o = object;
		if (clazz == byte[].class) {
			o = ByteBuffer.wrap((byte[])object);
		}

		if (!cassandraNativeSupport(object.getClass())) {
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
		} else if (c.equals(boolean.class)) {
			cassandraType = "boolean";
		} else if (c.equals(BigDecimal.class)) {
			cassandraType = "decimal";
		} else if (c.equals(double.class)) {
			cassandraType = "double";
		} else if (c.equals(float.class)) {
			cassandraType = "float";
		} else if (c.equals(int.class) || c.equals(Integer.class)) {
			cassandraType = "int";
		} else if (c.equals(List.class)) {
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

}
