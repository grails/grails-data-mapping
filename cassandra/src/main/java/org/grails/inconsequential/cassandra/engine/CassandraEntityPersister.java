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
import org.apache.thrift.TBase;
import org.grails.inconsequential.cassandra.CassandraKey;
import org.grails.inconsequential.core.Key;
import org.grails.inconsequential.kv.engine.AbstractKeyValueEntityPesister;
import org.grails.inconsequential.mapping.PersistentEntity;

import java.util.List;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
public class CassandraEntityPersister extends AbstractKeyValueEntityPesister<TBase, Object> {
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
    protected TBase createNewEntry(String family) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected Object getEntryValue(TBase nativeEntry, String get) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void setEntryValue(TBase nativeEntry, String key, Object value) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected TBase retrieveEntry(PersistentEntity persistentEntity, String family, Key key) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected Object storeEntry(PersistentEntity persistentEntity, TBase nativeEntry) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void deleteEntries(String family, List<Object> keys) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
