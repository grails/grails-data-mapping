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
package org.grails.inconsequential.cassandra.engine;

import me.prettyprint.cassandra.service.CassandraClient;
import me.prettyprint.cassandra.service.Keyspace;
import org.apache.cassandra.thrift.*;
import org.apache.thrift.TException;
import org.grails.inconsequential.cassandra.CassandraConnection;
import org.grails.inconsequential.cassandra.CassandraDatastore;
import org.grails.inconsequential.cassandra.CassandraKey;
import org.grails.inconsequential.cassandra.uuid.UUIDUtil;
import org.grails.inconsequential.core.Key;
import org.grails.inconsequential.kv.engine.AbstractKeyValueEntityPesister;
import org.grails.inconsequential.kv.engine.KeyValueEntry;
import org.grails.inconsequential.mapping.ClassMapping;
import org.grails.inconsequential.mapping.PersistentEntity;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

import java.io.Serializable;
import java.util.*;

import static me.prettyprint.cassandra.utils.StringUtils.bytes;
import static me.prettyprint.cassandra.utils.StringUtils.string;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
public class CassandraEntityPersister extends AbstractKeyValueEntityPesister<KeyValueEntry, Object> {
    private CassandraClient cassandraClient;


    public CassandraEntityPersister(PersistentEntity entity, CassandraConnection conn, CassandraClient cassandraClient) {
        super(entity,conn);
        this.cassandraClient = cassandraClient;
    }


    @Override
    protected Key createDatastoreKey(Object key) {
        return new CassandraKey(key);
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
        if(value != null) {
            nativeEntry.put(key, bytes(value.toString()));
        }
    }

    @Override
    protected KeyValueEntry retrieveEntry(PersistentEntity persistentEntity, String family, Key key) {
        final ClassMapping cm = getPersistentEntity().getMapping();
        final String keyspaceName = getKeyspace(cm, CassandraDatastore.DEFAULT_KEYSPACE);

        final Keyspace keyspace;
        try {
            keyspace = cassandraClient.getKeyspace(keyspaceName);
        } catch (NotFoundException e) {
            throw new InvalidDataAccessResourceUsageException("Cassandra Keyspace ["+keyspaceName+"] not found: " + e.getMessage(),e);
        } catch (TException e) {
            throw new DataAccessResourceFailureException("Exception occurred invoking Cassandra: " + e.getMessage(),e);
        }

        SuperColumn sc = getSuperColumn(keyspace, family, (Serializable) key.getNativeKey());
        KeyValueEntry entry = new KeyValueEntry(family);
        if(sc != null) {
            for (Column column : sc.getColumns()) {
                entry.put(string(column.getName()), string(column.getValue()));
            }
        }
        if(entry.isEmpty()) return null;
        else
            return entry;
    }

    private SuperColumn getSuperColumn(Keyspace keyspace, String family, Serializable id) {
        ColumnParent parent = new ColumnParent();
        parent.setColumn_family(family);


        final List<SuperColumn> result;
        try {
            SlicePredicate predicate = new SlicePredicate();
            predicate.setSlice_range(new SliceRange(new byte[0], new byte[0], false, 1));
            result = keyspace.getSuperSlice(id.toString(), parent, predicate);
        } catch (InvalidRequestException e) {
            throw new DataIntegrityViolationException("Cannot retrieve SuperColumn for uuid ["+ id +"] for ColumnPath ["+family+"]: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new DataAccessResourceFailureException("Exception occurred invoking Cassandra: " + e.getMessage(),e);
        }
        return !result.isEmpty() ? result.get(0) : null;
    }

    @Override
    protected void updateEntry(PersistentEntity persistentEntity, Object id, KeyValueEntry entry) {
        Keyspace keyspace = getKeyspace();
        final String family = getFamily(persistentEntity, persistentEntity.getMapping());
        SuperColumn sc = getSuperColumn(keyspace, family, (Serializable)id);
        if(sc != null) {
            updateSuperColumn(sc, entry);

            Map<String, List<SuperColumn>> insertMap = createInsertMap(family, sc);

            performInsertion(keyspace,id.toString(),insertMap, entry);
        }
    }

    @Override
    protected Object storeEntry(PersistentEntity persistentEntity, KeyValueEntry nativeEntry) {

        UUID uuid = UUIDUtil.getTimeUUID();
        final Keyspace keyspace = getKeyspace();
        String family = getFamily(persistentEntity, getPersistentEntity().getMapping());
        SuperColumn sc = new SuperColumn();
        sc.setName(UUIDUtil.asByteArray(uuid));
        updateSuperColumn(sc, nativeEntry);
        Map<String, List<SuperColumn>> insertMap = createInsertMap(family, sc);
        performInsertion(keyspace, uuid.toString(), insertMap, nativeEntry);
        return uuid;
    }

    private void performInsertion(Keyspace keyspace, String key, Map<String, List<SuperColumn>> insertMap, KeyValueEntry nativeEntry) {
        try {
            keyspace.batchInsert(key, null,insertMap);

        } catch (TException e) {
            throw new DataAccessResourceFailureException("Exception occurred performing insertion of entry ["+nativeEntry+"]: " + e.getMessage(),e);
        } catch (UnavailableException e) {
            throw new DataAccessResourceFailureException("Cassandra Unavailable Exception: " + e.getMessage(),e);
        } catch (InvalidRequestException e) {
            throw new DataIntegrityViolationException("Cannot write values "+nativeEntry.keySet()+" for key ["+key+"]: " + e.getMessage(), e);
        } catch (TimedOutException e) {
            throw new DataAccessResourceFailureException("Cassandra Unavailable Exception: " + e.getMessage(),e);
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
            c.setName( bytes(prop) );
            c.setValue((byte[])nativeEntry.get(prop));
            c.setTimestamp(time);
            sc.addToColumns(c);
        }
    }

    @Override
    protected void deleteEntries(String family, List<Object> keys) {

        //To change body of implemented methods use File | Settings | File Templates.
    }

    private Keyspace getKeyspace() {
        Keyspace keyspace;
        final String keyspaceName = getKeyspace(getPersistentEntity().getMapping(), CassandraDatastore.DEFAULT_KEYSPACE);
        try {
            keyspace = cassandraClient.getKeyspace(keyspaceName);
        } catch (NotFoundException e) {
            throw new InvalidDataAccessResourceUsageException("Cassandra Keyspace ["+keyspaceName+"] not found: " + e.getMessage(),e);
        } catch (TException e) {
            throw new DataAccessResourceFailureException("Exception occurred looking up Keyspace ["+keyspaceName+"]: " + e.getMessage(),e);
        }
        return keyspace;
    }
}
