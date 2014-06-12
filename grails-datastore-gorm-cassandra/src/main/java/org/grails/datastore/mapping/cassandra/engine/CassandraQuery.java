package org.grails.datastore.mapping.cassandra.engine;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import org.grails.datastore.mapping.cassandra.CassandraDatastore;
import org.grails.datastore.mapping.cassandra.CassandraSession;
import org.grails.datastore.mapping.keyvalue.engine.KeyValueEntry;
import org.grails.datastore.mapping.keyvalue.mapping.config.Family;
import org.grails.datastore.mapping.model.ClassMapping;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.query.Query;
import org.grails.datastore.mapping.query.api.QueryArgumentsAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CassandraQuery extends Query implements QueryArgumentsAware {

	private static Logger log = LoggerFactory.getLogger(CassandraQuery.class);

	private Map queryArguments = Collections.emptyMap();
	private static Map<Class, QueryHandler> queryHandlers = new HashMap<Class, QueryHandler>();
	private CassandraSession cassandraSession;

	public CassandraQuery(CassandraSession session, PersistentEntity entity) {
		super(session, entity);
		cassandraSession = session;
	}

	@Override
	protected List executeQuery(PersistentEntity entity, Junction criteria) {


		List<Object> out = new LinkedList<Object>();
		if (criteria instanceof Disjunction) {
			log.error("Cassandra GORM doesn't support Disjuntion, nothing will be returned");
		} else if (criteria instanceof Conjunction) {
			log.debug("Executing Conjunction Query");

			final ClassMapping<Family> cm = entity.getMapping();
			final String keyspaceName = getKeyspace(cm, CassandraDatastore.DEFAULT_KEYSPACE);
			String family = cm.getMappedForm().getFamily();

			Select select = QueryBuilder.select().all().from(keyspaceName, family);

			Select.Where where = select.where();

			List<Criterion> criterions = criteria.getCriteria();

			for (Criterion c : criterions) {
				log.debug("Working on Criterion: " + c);
				if (c instanceof Equals) {
					Equals eq = (Equals)c;
					Clause clause = QueryBuilder.eq(eq.getProperty(), eq.getValue());
					where.and(clause);
				} else {
					//TODO
					log.warn("Got Non Equals Criterion not supported yet");
				}
			}

			if (criterions.size() > 1) {
				//TODO make this a config option?
				log.debug("Criterions size:"+criterions.size());
				log.warn("Allowing filtering in CQL Query this can cause performance issues");
				select = select.allowFiltering();
			}

			ResultSet rs = cassandraSession.getNativeInterface().execute(select);
			CassandraEntityPersister cassandraEntityPersister = (CassandraEntityPersister)session.getPersister(entity);

			for (Row row : rs) {
				KeyValueEntry nativeEntry = cassandraEntityPersister.rowToKeyValueEntry(row, family);
				out.add(cassandraEntityPersister.createObjectFromNativeEntry(entity, nativeEntry.get(cm.getIdentifier().getIdentifierName()[0]).toString(), nativeEntry));    //TODO change for updates
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
