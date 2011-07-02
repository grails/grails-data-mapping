package org.springframework.datastore.mapping.simpledb;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.datastore.mapping.core.AbstractSession;
import org.springframework.datastore.mapping.core.impl.PendingInsert;
import org.springframework.datastore.mapping.engine.Persister;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.simpledb.engine.SimpleDBEntityPersister;
import org.springframework.datastore.mapping.simpledb.query.SimpleDBQuery;
import org.springframework.datastore.mapping.simpledb.util.SimpleDBTemplate;
import org.springframework.datastore.mapping.transactions.SessionOnlyTransaction;
import org.springframework.datastore.mapping.transactions.Transaction;

import java.util.Collection;
import java.util.Map;

/**
 * A {@link org.springframework.datastore.mapping.core.Session} implementation for the AWS SimpleDB store.
 *
 * @author Roman Stepanenko based on Graeme Rocher code for MongoDb and Redis 
 * @since 0.1
 */
public class SimpleDBSession extends AbstractSession {

    SimpleDBDatastore simpleDBDatastore;

    public SimpleDBSession(SimpleDBDatastore datastore, MappingContext mappingContext, ApplicationEventPublisher publisher) {
        super(datastore, mappingContext, publisher);
        this.simpleDBDatastore = datastore;
    }

    @Override
    public SimpleDBQuery createQuery(@SuppressWarnings("rawtypes") Class type) {
        return (SimpleDBQuery) super.createQuery(type);
    }

/*    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void flushPendingInserts(Map<PersistentEntity, Collection<PendingInsert>> inserts) {
        //todo - optimize multiple inserts using batch put (make the number of threshold objects configurable)
        for (final PersistentEntity entity : inserts.keySet()) {
            final SimpleDBTemplate template = getSimpleDBTemplate(entity.isRoot() ? entity : entity.getRootEntity());

            throw new RuntimeException("not implemented yet");
//            template.persist(null); //todo - :)
        }
    }
    */

    public Object getNativeInterface() {
        return null; //todo
    }

    @Override
    protected Persister createPersister(@SuppressWarnings("rawtypes") Class cls, MappingContext mappingContext) {
        final PersistentEntity entity = mappingContext.getPersistentEntity(cls.getName());
        return entity == null ? null : new SimpleDBEntityPersister(mappingContext, entity, this, publisher);
    }

    @Override

    protected Transaction beginTransactionInternal() {
        return new SessionOnlyTransaction(null, this);
    }

    public SimpleDBTemplate getSimpleDBTemplate(PersistentEntity entity) {
        return simpleDBDatastore.getSimpleDBTemplate(entity);
    }
}