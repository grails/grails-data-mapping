package org.springframework.datastore.mapping.simpledb.query;

import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.query.Query;

import java.util.List;

/**
 * A {@link org.springframework.datastore.mapping.query.Query} implementation for the SimpleDB store
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
@SuppressWarnings("rawtypes")
public class SimpleDBQuery extends Query{
    public SimpleDBQuery(Session session, PersistentEntity entity) {
        super(session, entity);
    }

    @Override
    protected List executeQuery(PersistentEntity entity, Junction criteria) {
        throw new RuntimeException("not implemented yet: executeQuery");
    }
}
