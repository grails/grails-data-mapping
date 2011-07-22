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

import static me.prettyprint.cassandra.utils.StringUtils.bytes;
import static me.prettyprint.cassandra.utils.StringUtils.string;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import me.prettyprint.cassandra.model.HectorException;
import me.prettyprint.cassandra.service.CassandraClient;
import me.prettyprint.cassandra.service.Keyspace;

import org.apache.cassandra.thrift.Column;
import org.apache.cassandra.thrift.ColumnParent;
import org.apache.cassandra.thrift.SlicePredicate;
import org.apache.cassandra.thrift.SliceRange;
import org.apache.cassandra.thrift.SuperColumn;
import org.springframework.dao.DataAccessResourceFailureException;
import org.grails.datastore.mapping.cassandra.CassandraDatastore;
import org.grails.datastore.mapping.cassandra.CassandraSession;
import org.grails.datastore.mapping.cassandra.uuid.UUIDUtil;
import org.grails.datastore.mapping.engine.AssociationIndexer;
import org.grails.datastore.mapping.engine.PropertyValueIndexer;
import org.grails.datastore.mapping.keyvalue.engine.AbstractKeyValueEntityPesister;
import org.grails.datastore.mapping.keyvalue.engine.KeyValueEntry;
import org.grails.datastore.mapping.model.ClassMapping;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.types.Association;
import org.grails.datastore.mapping.query.Query;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
public class CassandraEntityPersister extends AbstractKeyValueEntityPesister<KeyValueEntry, Object> {

    private CassandraClient cassandraClient;
    private static final byte[] ZERO_LENGTH_BYTE_ARRAY = new byte[0];

    public CassandraEntityPersister(MappingContext context, PersistentEntity entity,
            CassandraSession conn, CassandraClient cassandraClient) {
        super(context, entity, conn);
        this.cassandraClient = cassandraClient;
    }

    @Override
    protected void deleteEntry(String family, Object key) {
        // TODO: Implement deletion of entities
    }

    @Override
    public AssociationIndexer getAssociationIndexer(KeyValueEntry nativeEntry, Association association) {
        return new CassandraAssociationIndexer(cassandraClient, association, getKeyspaceName());
    }

    @Override
    public PropertyValueIndexer getPropertyIndexer(PersistentProperty property) {
        return null; // TODO: Support querying in cassandra
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
            nativeEntry.put(key, bytes(value.toString()));
        }
    }

    @Override
    protected KeyValueEntry retrieveEntry(PersistentEntity persistentEntity, String family, Serializable nativeKey) {
        final ClassMapping cm = getPersistentEntity().getMapping();
        final String keyspaceName = getKeyspace(cm, CassandraDatastore.DEFAULT_KEYSPACE);

        final Keyspace keyspace;
        try {
            keyspace = cassandraClient.getKeyspace(keyspaceName);
        }
        catch (HectorException e) {
            throw new DataAccessResourceFailureException(
                    "Exception occurred invoking Cassandra: " + e.getMessage(), e);
        }

        SuperColumn sc = getSuperColumn(keyspace, family, nativeKey);
        KeyValueEntry entry = new KeyValueEntry(family);
        if (sc != null) {
            for (Column column : sc.getColumns()) {
                entry.put(string(column.getName()), string(column.getValue()));
            }
        }

        if (entry.isEmpty()) {
            return null;
        }

        return entry;
    }

    private SuperColumn getSuperColumn(Keyspace keyspace, String family, Serializable id) {
        ColumnParent parent = new ColumnParent();
        parent.setColumn_family(family);

        final List<SuperColumn> result;
        try {
            SlicePredicate predicate = new SlicePredicate();
            predicate.setSlice_range(new SliceRange(ZERO_LENGTH_BYTE_ARRAY, ZERO_LENGTH_BYTE_ARRAY, false, 1));
            result = keyspace.getSuperSlice(id.toString(), parent, predicate);
        }
        catch (HectorException e) {
            throw new DataAccessResourceFailureException("Exception occurred invoking Cassandra: " + e.getMessage(), e);
        }

        return !result.isEmpty() ? result.get(0) : null;
    }

    @Override
    protected void updateEntry(PersistentEntity persistentEntity, Object id, KeyValueEntry entry) {
        Keyspace keyspace = getKeyspace();
        final String family = getFamily(persistentEntity, persistentEntity.getMapping());
        SuperColumn sc = getSuperColumn(keyspace, family, (Serializable)id);
        if (sc != null) {
            updateSuperColumn(sc, entry);

            Map<String, List<SuperColumn>> insertMap = createInsertMap(family, sc);

            performInsertion(keyspace, id.toString(), insertMap, entry);
        }
    }

    @Override
    protected void deleteEntries(String family, List<Object> keys) {
    }

    @Override
    protected Object storeEntry(PersistentEntity persistentEntity, Object storeId, KeyValueEntry nativeEntry) {
        UUID uuid = (UUID)storeId;
        final Keyspace keyspace = getKeyspace();
        String family = getFamily(persistentEntity, getPersistentEntity().getMapping());
        SuperColumn sc = new SuperColumn();
        sc.setName(UUIDUtil.asByteArray(uuid));
        updateSuperColumn(sc, nativeEntry);
        Map<String, List<SuperColumn>> insertMap = createInsertMap(family, sc);
        performInsertion(keyspace, uuid.toString(), insertMap, nativeEntry);
        return uuid;
    }

    @Override
    protected Object generateIdentifier(PersistentEntity persistentEntity, KeyValueEntry id) {
        return UUIDUtil.getTimeUUID();
    }

    private void performInsertion(Keyspace keyspace, String key, Map<String, List<SuperColumn>> insertMap,
            @SuppressWarnings("unused") KeyValueEntry nativeEntry) {
        try {
            keyspace.batchInsert(key, null, insertMap);
        }
        catch (HectorException e) {
            throw new DataAccessResourceFailureException(
                    "Exception occurred invoking Cassandra: " + e.getMessage(), e);
        }
    }

    private Map<String, List<SuperColumn>> createInsertMap(String family, SuperColumn sc) {
        Map<String, List<SuperColumn>> insertMap = new HashMap<String, List<SuperColumn>>();
        List<SuperColumn> superColumns = new ArrayList<SuperColumn>();
        superColumns.add(sc);
        insertMap.put(family, superColumns);
        return insertMap;
    }

    private void updateSuperColumn(SuperColumn sc, KeyValueEntry nativeEntry) {
        final long time = System.currentTimeMillis() * 1000;
        for (String prop : nativeEntry.keySet()) {
            Column c = new Column();
            c.setName(bytes(prop));
            c.setValue((byte[])nativeEntry.get(prop));
            c.setTimestamp(time);
            sc.addToColumns(c);
        }
    }

    private Keyspace getKeyspace() {
        final String keyspaceName = getKeyspaceName();
        try {
            return cassandraClient.getKeyspace(keyspaceName);
        }
        catch (HectorException e) {
            throw new DataAccessResourceFailureException(
                    "Exception occurred invoking Cassandra: " + e.getMessage(), e);
        }
    }

    private String getKeyspaceName() {
        return getKeyspace(getPersistentEntity().getMapping(), CassandraDatastore.DEFAULT_KEYSPACE);
    }

    public Query createQuery() {
        return null; // TODO: Implement querying for Cassandra
    }
}
