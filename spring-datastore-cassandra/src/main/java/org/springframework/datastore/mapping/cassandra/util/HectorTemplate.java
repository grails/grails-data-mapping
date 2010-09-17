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
package org.springframework.datastore.mapping.cassandra.util;

import me.prettyprint.cassandra.model.HectorException;
import me.prettyprint.cassandra.service.CassandraClient;
import me.prettyprint.cassandra.service.Keyspace;
import org.apache.cassandra.thrift.InvalidRequestException;
import org.apache.cassandra.thrift.NotFoundException;
import org.apache.cassandra.thrift.TimedOutException;
import org.apache.cassandra.thrift.UnavailableException;
import org.apache.thrift.TException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;

/**
 * A Spring style template that wraps Cassandra data access exceptions and rethrows
 * to Spring's standard exception hierarchy
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class HectorTemplate {

    CassandraClient cassandraClient;

    public HectorTemplate(CassandraClient cassandraClient) {
        this.cassandraClient = cassandraClient;
    }


    public Object execute(String keyspace, HectorCallback callable) throws DataAccessException {
        final Keyspace ks;
        try {
            ks = cassandraClient.getKeyspace(keyspace);

        }
        catch (HectorException e) {
              throw new DataAccessResourceFailureException("Exception occurred invoking Cassandra: " + e.getMessage(),e);
        }
        try {
            return callable.doInHector(ks);
        }
        catch (HectorException e) {
              throw new DataAccessResourceFailureException("Exception occurred invoking Cassandra: " + e.getMessage(),e);
        }



    }
}
