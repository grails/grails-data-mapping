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
package org.springframework.datastore.mapping.cassandra.engine;

import me.prettyprint.cassandra.service.CassandraClient;
import me.prettyprint.cassandra.service.Keyspace;
import org.apache.cassandra.thrift.*;
import org.apache.thrift.TException;
import org.springframework.datastore.mapping.cassandra.util.HectorCallback;
import org.springframework.datastore.mapping.cassandra.util.HectorTemplate;
import org.springframework.datastore.mapping.engine.AssociationIndexer;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.model.types.Association;
import org.springframework.datastore.mapping.reflect.NameUtils;

import java.io.Serializable;
import java.util.*;

import static me.prettyprint.cassandra.utils.StringUtils.bytes;
import static me.prettyprint.cassandra.utils.StringUtils.string;

/**
 * AssociationIndexer for Cassandra one-to-many associations
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class CassandraAssociationIndexer implements AssociationIndexer<Serializable, Serializable> {

    private static final byte[] ZERO_LENGTH_BYTE_ARRAY = new byte[0];

    private CassandraClient cassandraClient;
    private Association association;
    private String keyspace;
    private String columnFamily;


    public CassandraAssociationIndexer(CassandraClient cassandraClient, Association association, String keyspace) {
        this.cassandraClient = cassandraClient;
        this.association = association;
        this.keyspace = keyspace;
        this.columnFamily = getDefaultColumnFamilyName(association);
    }

    protected String getDefaultColumnFamilyName(Association association) {
        return association.getOwner().getName() + NameUtils.capitalize(association.getName());
    }

    public void index(final Serializable primaryKey, final List<Serializable> foreignKeys) {

        HectorTemplate ht = new HectorTemplate(cassandraClient);
        ht.execute(keyspace, new HectorCallback(){

            public Object doInHector(Keyspace keyspace)  {
                Map<String, List<Column>> cfmap = new HashMap<String, List<Column>>();
                final long time = System.currentTimeMillis() * 1000;

                List<Column> columns = new ArrayList<Column>();
                for (Serializable foreignKey : foreignKeys) {
                    byte[] keyInBytes = bytes(foreignKey.toString());
                    final Column column = new Column(keyInBytes,keyInBytes,time);
                    columns.add(column);
                }
                cfmap.put(columnFamily, columns);
                keyspace.batchInsert(primaryKey.toString(),cfmap, null);
                return null;
            }
        });
    }

    public void index(Serializable primaryKey, Serializable foreignKey) {
        List list = new ArrayList(); list.add(foreignKey);
        index(primaryKey, list);
    }

    public List<Serializable> query(final Serializable primaryKey) {
        HectorTemplate ht = new HectorTemplate(cassandraClient);
        return (List<Serializable>) ht.execute(keyspace, new HectorCallback() {
            public Object doInHector(Keyspace keyspace)  {
                SlicePredicate predicate = new SlicePredicate();
                predicate.setSlice_range(new SliceRange(ZERO_LENGTH_BYTE_ARRAY,ZERO_LENGTH_BYTE_ARRAY, false, Integer.MAX_VALUE));
                ColumnParent cp = new ColumnParent();
                cp.setColumn_family(columnFamily);


                final List<Column> columns = keyspace.getSlice(primaryKey.toString(), cp, predicate);
                if(columns== null || columns.isEmpty()) {
                    return Collections.emptyList();
                }


                List<Serializable> keys = new ArrayList<Serializable>();
                for (Column column : columns) {
                    keys.add(string(column.getName()));
                }
                return keys;

            }
        });

    }

    public PersistentEntity getIndexedEntity() {
        return association.getAssociatedEntity();
    }
}
