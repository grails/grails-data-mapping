package org.grails.datastore.mapping.cassandra.engine;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import org.grails.datastore.gorm.finders.MethodExpression;
import org.grails.datastore.mapping.cassandra.CassandraDatastore;
import org.grails.datastore.mapping.cassandra.CassandraSession;
import org.grails.datastore.mapping.cassandra.engine.CassandraEntityPersister;
import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.keyvalue.engine.KeyValueEntry;
import org.grails.datastore.mapping.keyvalue.mapping.config.Family;
import org.grails.datastore.mapping.model.ClassMapping;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.query.Query;
import org.grails.datastore.mapping.query.api.QueryArgumentsAware;

import java.util.*;

public class CassandraQuery extends Query implements QueryArgumentsAware {

	private Map queryArguments = Collections.emptyMap();
	private static Map<Class, QueryHandler> queryHandlers = new HashMap<Class, QueryHandler>();
	private CassandraSession cassandraSession;

	public CassandraQuery(CassandraSession session, PersistentEntity entity) {
		super(session, entity);
		cassandraSession = session;
	}

	@Override
	protected List executeQuery(PersistentEntity entity, Junction criteria) {
		System.out.println(criteria.getCriteria().size());

		List<Object> out = new LinkedList<Object>();
		if (criteria instanceof Disjunction) {
			System.out.println("Disjunction");
			//TODO Can Not actually support Disjunction

		} else if (criteria instanceof Conjunction) {
			System.out.println("Conjunction");

			final ClassMapping cm = entity.getMapping();
			final String keyspaceName = getKeyspace(cm, CassandraDatastore.DEFAULT_KEYSPACE);
			String family = entity.getDecapitalizedName(); //FIXME This is not correct just a hack
			System.out.println(cm.getMappedForm().toString());

			Select select = QueryBuilder.select().all().from(keyspaceName, family);

			Select.Where where = select.where();

			for (Criterion c : criteria.getCriteria()) {
				System.out.println(c);
				if (c instanceof Equals) {
					Equals eq = (Equals)c;
					Clause clause = QueryBuilder.eq(eq.getProperty(), eq.getValue());
					where.and(clause);
				} else {
					//TODO
				}
			}


			ResultSet rs = cassandraSession.getNativeInterface().execute(select);
			CassandraEntityPersister cassandraEntityPersister = (CassandraEntityPersister)session.getPersister(entity);

			for (Row row : rs) {
				KeyValueEntry nativeEntry = cassandraEntityPersister.rowToKeyValueEntry(row, family);
				out.add(cassandraEntityPersister.createObjectFromNativeEntry(entity, nativeEntry.get("id").toString(), nativeEntry));
			}
		}

		return out;
	}

	@Override
	public void setArguments(@SuppressWarnings("rawtypes") Map arguments) {
		this.queryArguments = arguments;
	}

	private static interface QueryHandler<T> {
		public void handle(PersistentEntity entity, T criterion, QueryBuilder qb);
	}

	protected String getKeyspace(ClassMapping<Family> cm, String defaultValue) {
		String keyspace = null;
		if (cm.getMappedForm() != null) {
			keyspace = cm.getMappedForm().getKeyspace();
		}
		if (keyspace == null) { keyspace = defaultValue; }
		return keyspace;
	}
}
