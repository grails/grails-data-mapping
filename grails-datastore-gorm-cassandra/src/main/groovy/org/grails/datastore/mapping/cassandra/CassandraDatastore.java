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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SocketOptions;
import com.datastax.driver.core.policies.ConstantReconnectionPolicy;
import com.datastax.driver.core.policies.DowngradingConsistencyRetryPolicy;

import groovy.util.ConfigObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.dao.DataAccessResourceFailureException;
import org.grails.datastore.mapping.core.AbstractDatastore;
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext;
import org.grails.datastore.mapping.model.MappingContext;

public class CassandraDatastore extends AbstractDatastore implements DisposableBean {

	private static Logger log = LoggerFactory.getLogger(CassandraDatastore.class);

	public static final String DEFAULT_KEYSPACE = "CassandraKeySpace";
	private Cluster cluster;
	private Session session;

	public CassandraDatastore(MappingContext mappingContext, ConfigurableApplicationContext ctx, ConfigObject config) {
		super(mappingContext);

		List<String> contactPoints = Collections.<String>emptyList();
		Object configContactPoints = config.get("contactPoints");
		if (configContactPoints instanceof List) {
			List configContactPointsList = (List)configContactPoints;
			contactPoints = new ArrayList<String>(configContactPointsList.size());
			for (Object point : (List)configContactPoints) {
				if (point instanceof String) {
					contactPoints.add((String)point);
				} else {
					log.warn("Rejecting non-string contact point: " + point.toString());
				}
			}
		} else if (configContactPoints instanceof String) {
			contactPoints = new ArrayList<String>(1);
			contactPoints.add((String)configContactPoints);
		} else {
			log.error("Could not convert contactPoints to needed format. Should be a List of Strings or a String.");
		}

		this.setApplicationContext(ctx);

		Cluster.Builder builder = Cluster.builder();

		for (String contactPoint : contactPoints) {
			builder.addContactPoint(contactPoint);
		}
		builder.withRetryPolicy(DowngradingConsistencyRetryPolicy.INSTANCE)
			.withReconnectionPolicy(new ConstantReconnectionPolicy(100L))
			.withSocketOptions(new SocketOptions().setKeepAlive(true));

		this.cluster = builder.build();
		this.session = cluster.connect();

        initializeConverters(mappingContext);

        mappingContext.getConverterRegistry().addConverter(new Converter<Date, Calendar>() {
            public Calendar convert(Date source) {
                Calendar dest = Calendar.getInstance();
                dest.setTime(source);
                return dest;
            }
        });

        mappingContext.getConverterRegistry().addConverter(new Converter<Calendar, Date>() {
            public Date convert(Calendar source) {
                return source.getTime();
            }
        });

	}

	@Override
	public void destroy() throws Exception {
		super.destroy();
		session.close();
		cluster.close();
	}

	@Override
	protected org.grails.datastore.mapping.core.Session createSession(Map<String, String> connectionDetails) {

		try {
			return new CassandraSession(this, getMappingContext(), this.session, getApplicationEventPublisher(), false);
		} catch (Exception e) {
			throw new DataAccessResourceFailureException("Failed to obtain Cassandra client session: " + e.getMessage(), e);
		}
	}
}
