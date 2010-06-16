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

import me.prettyprint.cassandra.service.BatchMutation;
import me.prettyprint.cassandra.service.CassandraClient;
import me.prettyprint.cassandra.service.Keyspace;
import org.apache.cassandra.thrift.*;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.grails.inconsequential.cassandra.CassandraDatastore;
import org.grails.inconsequential.cassandra.CassandraKey;
import org.grails.inconsequential.cassandra.uuid.UUIDUtil;
import org.grails.inconsequential.core.Key;
import org.grails.inconsequential.kv.engine.AbstractKeyValueEntityPesister;
import org.grails.inconsequential.kv.engine.KeyValueEntry;
import org.grails.inconsequential.kv.mapping.Family;
import org.grails.inconsequential.kv.mapping.KeyValuePersistentEntity;
import org.grails.inconsequential.mapping.ClassMapping;
import org.grails.inconsequential.mapping.PersistentEntity;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import static me.prettyprint.cassandra.utils.StringUtils.*;

import java.util.*;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
public class CassandraEntityPersister extends AbstractKeyValueEntityPesister<KeyValueEntry, Object> {
    private CassandraClient cassandraClient;


    public CassandraEntityPersister(PersistentEntity entity, CassandraClient cassandraClient) {
        super(entity);
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
        nativeEntry.put(key, bytes(value.toString()));
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

        final UUID uuid = (UUID) key.getNativeKey();
        ColumnParent parent = new ColumnParent();
        parent.setColumn_family(family);


        final List<SuperColumn> result;
        try {
            SlicePredicate predicate = new SlicePredicate();
            predicate.setSlice_range(new SliceRange(new byte[0], new byte[0], false, 1));
            result = keyspace.getSuperSlice(uuid.toString(), parent, predicate);
        } catch (InvalidRequestException e) {
            throw new DataIntegrityViolationException("Cannot retrieve SuperColumn for uuid ["+uuid+"] for ColumnPath ["+family+"]: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new DataAccessResourceFailureException("Exception occurred invoking Cassandra: " + e.getMessage(),e);
        }
        KeyValueEntry entry = new KeyValueEntry(family);

        for (SuperColumn sc : result) {

            if(sc != null) {
                for (Column column : sc.getColumns()) {
                    entry.put(string(column.getName()), string(column.getValue()));
                }
            }
        }
        if(entry.isEmpty()) return null;
        else
            return entry;
    }

    private ColumnPath createColumnPath(String family, UUID uuid) {
        ColumnPath cp = new ColumnPath(family);

        cp.setSuper_column(UUIDUtil.asByteArray(uuid));
        return cp;
    }

    @Override
    protected Object storeEntry(PersistentEntity persistentEntity, KeyValueEntry nativeEntry) {
        final String keyspaceName = getKeyspace(getPersistentEntity().getMapping(), CassandraDatastore.DEFAULT_KEYSPACE);

        UUID uuid = UUIDUtil.getTimeUUID();
        try {
            final Keyspace keyspace = cassandraClient.getKeyspace(keyspaceName);

            String family = getFamily(persistentEntity, getPersistentEntity().getMapping());

            final long time = System.currentTimeMillis() * 1000;
            SuperColumn sc = new SuperColumn();

            sc.setName(UUIDUtil.asByteArray(uuid));
            for (String prop : nativeEntry.keySet()) {
                Column c = new Column();
                c.setName( bytes(prop) );
                c.setValue((byte[])nativeEntry.get(prop));
                c.setTimestamp(time);
                sc.addToColumns(c);
            }

            Map<String, List<SuperColumn>> insertMap = new HashMap<String, List<SuperColumn>>();
            List<SuperColumn> superColumns = new ArrayList<SuperColumn>();
            superColumns.add(sc);
            insertMap.put(family, superColumns);

            keyspace.batchInsert(uuid.toString(), null,insertMap);
            return uuid;

        } catch (NotFoundException e) {
            throw new InvalidDataAccessResourceUsageException("Cassandra Keyspace ["+keyspaceName+"] not found: " + e.getMessage(),e);
        } catch (TException e) {
            throw new DataAccessResourceFailureException("Exception occurred ");
        } catch (UnavailableException e) {
            throw new DataAccessResourceFailureException("Cassandra Unavailable Exception: " + e.getMessage(),e);
        } catch (InvalidRequestException e) {
            throw new DataIntegrityViolationException("Cannot write values "+nativeEntry.keySet()+" for id ["+uuid+"]: " + e.getMessage(), e);
        } catch (TimedOutException e) {
            throw new DataAccessResourceFailureException("Cassandra Unavailable Exception: " + e.getMessage(),e);
        }
    }

    @Override
    protected void deleteEntries(String family, List<Object> keys) {

        //To change body of implemented methods use File | Settings | File Templates.
    }
}
