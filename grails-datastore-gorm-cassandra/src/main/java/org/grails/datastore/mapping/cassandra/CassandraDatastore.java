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
package org.grails.datastore.mapping.cassandra;

import java.util.Map;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.SocketOptions;
import com.datastax.driver.core.policies.ConstantReconnectionPolicy;
import com.datastax.driver.core.policies.DowngradingConsistencyRetryPolicy;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.DataAccessResourceFailureException;
import org.grails.datastore.mapping.core.AbstractDatastore;
import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext;
import org.grails.datastore.mapping.model.MappingContext;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
public class CassandraDatastore extends AbstractDatastore {
	public static final String DEFAULT_KEYSPACE = "CassandraKeySpace"; //TODO make onse keyspace for each session somehow, maybe just do a different datastore instance?
	private Cluster cluster;

	public CassandraDatastore(MappingContext mappingContext, ConfigurableApplicationContext ctx) {
		super(mappingContext);
		this.setApplicationContext(ctx);

		this.cluster = Cluster.builder()
			.addContactPoints("10.125.12.32")
			.withRetryPolicy(DowngradingConsistencyRetryPolicy.INSTANCE)
			.withReconnectionPolicy(new ConstantReconnectionPolicy(100L))
			.withSocketOptions(new SocketOptions().setKeepAlive(true))
			.build();
	}

	public CassandraDatastore() {
		this(new KeyValueMappingContext(DEFAULT_KEYSPACE),null);
	}

	@Override
	protected Session createSession(Map<String, String> connectionDetails) {

		try {
			return new CassandraSession(this, getMappingContext(), cluster, getApplicationEventPublisher(), false);
		} catch (Exception e) {
			throw new DataAccessResourceFailureException("Failed to obtain Cassandra client session: " + e.getMessage(), e);
		}
	}
}
