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

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessResourceFailureException;
import org.grails.datastore.mapping.cassandra.engine.CassandraEntityPersister;
import org.grails.datastore.mapping.core.AbstractSession;
import org.grails.datastore.mapping.core.Datastore;
import org.grails.datastore.mapping.engine.Persister;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.transactions.Transaction;
import org.springframework.transaction.TransactionSystemException;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
public class CassandraSession extends AbstractSession<Session> {

	Logger log = LoggerFactory.getLogger(CassandraSession.class);
	private Session session;
	private ApplicationEventPublisher applicationEventPublisher;

	public CassandraSession(Datastore ds, MappingContext context, Session session, ApplicationEventPublisher applicationEventPublisher, boolean stateless) {
		super(ds, context, applicationEventPublisher, stateless);
		this.applicationEventPublisher = applicationEventPublisher;
		this.session = session;
	}

	@Override
	protected Persister createPersister(Class cls, MappingContext mappingContext) {
		PersistentEntity entity = mappingContext.getPersistentEntity(cls.getName());
		if (entity != null) {
			return new CassandraEntityPersister(mappingContext, entity, this,applicationEventPublisher);
		}
		return null;
	}

	@Override
	public void disconnect() {
		super.disconnect();
	}

	@Override
	protected Transaction beginTransactionInternal() {
		throw new TransactionSystemException("Transactions are not supported by Cassandra");
	}

	public Session getNativeInterface() {
		return session;
	}
}
