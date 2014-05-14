package org.grails.datastore.mapping.cassandra.query;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.grails.datastore.mapping.cassandra.CassandraSession;
import org.grails.datastore.mapping.keyvalue.mapping.config.Family;
import org.grails.datastore.mapping.model.ClassMapping;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.query.Query;
import org.grails.datastore.mapping.query.api.QueryArgumentsAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.cassandra.core.CassandraTemplate;

import com.datastax.driver.core.querybuilder.QueryBuilder;

public class CassandraQuery extends Query implements QueryArgumentsAware {

    private static Logger log = LoggerFactory.getLogger(CassandraQuery.class);

    private Map queryArguments = Collections.emptyMap();
    private static Map<Class, QueryHandler> queryHandlers = new HashMap<Class, QueryHandler>();
    private CassandraSession cassandraSession;
    private CassandraTemplate cassandraTemplate;
    
    public CassandraQuery(CassandraSession session, PersistentEntity entity) {
        super(session, entity);
        cassandraSession = session;
        cassandraTemplate = cassandraSession.getCassandraTemplate();
    }

    @Override
    protected List executeQuery(PersistentEntity entity, Junction criteria) {

        List<Object> out = new LinkedList<Object>();
        if (criteria instanceof Disjunction) {
            log.error("Cassandra GORM doesn't support Disjuntion, nothing will be returned");
        } else if (criteria instanceof Conjunction) {
            final List<Projection> projectionList = projections().getProjectionList();
            if (projectionList.isEmpty()) {
                return cassandraTemplate.selectAll(entity.getJavaClass());
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
        if (keyspace == null) {
            keyspace = defaultValue;
        }
        return keyspace;
    }
}
