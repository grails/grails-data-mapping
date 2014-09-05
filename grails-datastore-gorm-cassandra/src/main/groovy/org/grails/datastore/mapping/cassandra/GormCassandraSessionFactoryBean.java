package org.grails.datastore.mapping.cassandra;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cassandra.core.SessionCallback;
import org.springframework.cassandra.core.cql.generator.CreateIndexCqlGenerator;
import org.springframework.cassandra.core.keyspace.CreateIndexSpecification;
import org.springframework.dao.DataAccessException;
import org.springframework.data.cassandra.config.CassandraSessionFactoryBean;
import org.springframework.data.cassandra.core.CassandraAdminTemplate;
import org.springframework.data.cassandra.mapping.CassandraPersistentEntity;
import org.springframework.data.cassandra.mapping.CassandraPersistentProperty;
import org.springframework.data.mapping.PropertyHandler;

import com.datastax.driver.core.ResultSet;

// TODO: Remove this class when spring-data-cassandra has implemented index creation
public class GormCassandraSessionFactoryBean extends CassandraSessionFactoryBean {
	private static Logger log = LoggerFactory.getLogger(CassandraSessionFactoryBean.class);

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		performIndexAction();
	}

	protected void performIndexAction() {
		switch (schemaAction) {

		case NONE:
			return;

		case RECREATE_DROP_UNUSED:
			// don't break!
		case RECREATE:
			// don't break!
		case CREATE:
			createIndex();
		}

	}

	protected void createIndex() {
		Collection<? extends CassandraPersistentEntity<?>> entities = mappingContext.getNonPrimaryKeyEntities();
		for (final CassandraPersistentEntity<?> entity : entities) {
			createIndex(entity, admin);
		}
	}

	static void createIndex(final CassandraPersistentEntity<?> entity, final CassandraAdminTemplate cassandraAdminTemplate) {
		entity.doWithProperties(new PropertyHandler<CassandraPersistentProperty>() {
			@Override
			public void doWithPersistentProperty(CassandraPersistentProperty persistentProperty) {
				if (persistentProperty.isIndexed()) {
					final CreateIndexSpecification createIndexSpecification = new CreateIndexSpecification();
					createIndexSpecification.tableName(entity.getTableName()).columnName(persistentProperty.getColumnName()).ifNotExists();
					cassandraAdminTemplate.execute(new SessionCallback<ResultSet>() {
						@Override
						public ResultSet doInSession(com.datastax.driver.core.Session s) throws DataAccessException {
							String cql = CreateIndexCqlGenerator.toCql(createIndexSpecification);
							log.debug(cql);
							return s.execute(cql);
						}
					});
				}
			}
		});
	}
}