package org.grails.datastore.gorm.cassandra;

import com.datastax.driver.core.querybuilder.QueryBuilder;
import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.query.Query;
import org.grails.datastore.mapping.query.api.QueryArgumentsAware;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CassandraQuery extends Query implements QueryArgumentsAware {
	protected CassandraQuery(Session session, PersistentEntity entity) {
		super(session, entity);
	}

	private static Map<Class, QueryHandler> queryHandlers = new HashMap<Class, QueryHandler>();

	@Override
	protected List executeQuery(PersistentEntity entity, Junction criteria) {
		return null;
	}

	@Override
	public void setArguments(@SuppressWarnings("rawtypes") Map arguments) {

	}

	private static interface QueryHandler<T> {
		public void handle(PersistentEntity entity, T criterion, QueryBuilder qb);
	}

}
