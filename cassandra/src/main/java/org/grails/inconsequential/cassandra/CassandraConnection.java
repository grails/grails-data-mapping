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
package org.grails.inconsequential.cassandra;

import org.grails.inconsequential.core.AbstractDatastore;
import org.grails.inconsequential.core.AbstractObjectDatastoreConnection;
import org.grails.inconsequential.engine.Persister;
import org.grails.inconsequential.mapping.MappingContext;
import org.grails.inconsequential.tx.Transaction;

import java.util.Map;
import java.util.UUID;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
public class CassandraConnection extends AbstractObjectDatastoreConnection<UUID> {

    public CassandraConnection(Map<String, String> connectionDetails, MappingContext mappingContext) {
        super(connectionDetails, mappingContext);
    }

    @Override
    protected Persister createPersister(Class cls, MappingContext mappingContext) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isConnected() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Transaction beginTransaction() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
