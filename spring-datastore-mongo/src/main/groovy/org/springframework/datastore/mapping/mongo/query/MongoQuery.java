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
package org.springframework.datastore.mapping.mongo.query;

import com.mongodb.*;
import org.springframework.datastore.document.DocumentStoreConnectionCallback;
import org.springframework.datastore.document.mongodb.MongoTemplate;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.mongo.MongoSession;
import org.springframework.datastore.mapping.mongo.engine.MongoEntityPersister;
import org.springframework.datastore.mapping.query.Query;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;

/**
 * A {@link org.springframework.datastore.mapping.query.Query} implementation for the Mongo document store
 *
 * @author Graeme Rocher
 * @since 1.0
 */

public class MongoQuery extends Query{

    private static Map<Class, QueryHandler> queryHandlers = new HashMap<Class, QueryHandler>();

    static {
        queryHandlers.put(Equals.class, new QueryHandler<Equals>() {
            public void handle(PersistentEntity entity, Equals criterion, DBObject query) {
                query.put(criterion.getProperty(), criterion.getValue());
            }
        });
        queryHandlers.put(Like.class, new QueryHandler<Like>() {
            public void handle(PersistentEntity entity, Like like, DBObject query) {
                Object value = like.getValue();
                if(value == null) value = "null";
                final String expr = value.toString().replace("%", ".*");
                Pattern regex = Pattern.compile(expr);
                query.put(like.getProperty(), regex);
            }
        });
        queryHandlers.put(In.class, new QueryHandler<In>() {
            public void handle(PersistentEntity entity, In in, DBObject query) {
                DBObject inQuery = new BasicDBObject();
                inQuery.put("$in", in.getValues());
                query.put(in.getProperty(), inQuery);
            }
        });
        queryHandlers.put(Conjunction.class, new QueryHandler<Conjunction>() {

            public void handle(PersistentEntity entity, Conjunction criterion, DBObject query) {
                populateMongoQuery(entity, query, criterion);
            }
        });

        queryHandlers.put(Disjunction.class, new QueryHandler<Disjunction>() {

            public void handle(PersistentEntity entity, Disjunction criterion, DBObject query) {
                List orList = new ArrayList();
                for (Criterion subCriterion : criterion.getCriteria()) {
                    final QueryHandler queryHandler = queryHandlers.get(subCriterion.getClass());
                    if(queryHandler != null) {
                        DBObject dbo = new BasicDBObject();
                        queryHandler.handle(entity, subCriterion, dbo);
                        orList.add(dbo);
                    }
                }
                query.put("$or", orList);
            }
        });
    }

    private MongoSession mongoSession;
    private MongoEntityPersister mongoEntityPersister;
    public MongoQuery(MongoSession session, PersistentEntity entity) {
        super(session, entity);
        this.mongoSession = session;
        this.mongoEntityPersister = (MongoEntityPersister) session.getPersister(entity);
    }

    @Override
    protected List executeQuery(final PersistentEntity entity, final Junction criteria) {
        final MongoTemplate template = mongoSession.getMongoTemplate(entity);

        return (List) template.execute(new DocumentStoreConnectionCallback<DB, Object>(){

            public Object doInConnection(DB db) throws Exception {

                final DBCollection collection = db.getCollection(template.getDefaultCollectionName());
                if(uniqueResult) {
                    final DBObject dbObject = collection.findOne();
                    final Object object = createObjectFromDBObject(dbObject);
                    return wrapObjectResultInList(object);
                }
                else {
                    final DBCursor cursor;
                    if(criteria.isEmpty()) {
                        cursor = collection.find();
                    }
                    else {
                        DBObject query = new BasicDBObject();

                        populateMongoQuery(entity, query, criteria);

                        cursor = collection.find(query);

                    }

                    if(offset > 0) {
                        cursor.skip(offset);
                    }
                    if(max > -1) {
                        cursor.limit(max);
                    }

                    for (Order order : orderBy) {
                        DBObject orderObject = new BasicDBObject();
                        orderObject.put(order.getProperty(), order.getDirection() == Order.Direction.DESC ? -1 : 1);
                        cursor.sort(orderObject);
                    }

                    final List<Projection> projectionList = projections().getProjectionList();
                    if(projectionList.isEmpty()) {
                        return new MongoResultList(cursor, mongoEntityPersister);
                    }
                    else {
                        List projectedResults = new ArrayList();
                        for (Projection projection : projectionList) {
                            if(projection instanceof CountProjection) {
                                projectedResults.add(cursor.size());
                            }
                        }

                        return projectedResults;
                    }

                }
            }

        });
    }

    public static void populateMongoQuery(PersistentEntity entity, DBObject query, Junction criteria) {

        List disjunction = null;
        if(criteria instanceof Disjunction) {
            disjunction = new ArrayList();
            query.put("$or",disjunction);
        }
        for (Criterion criterion : criteria.getCriteria()) {
            final QueryHandler queryHandler = queryHandlers.get(criterion.getClass());
            if(queryHandler != null) {
                DBObject dbo = query;
                if(disjunction != null) {
                    dbo = new BasicDBObject();
                    disjunction.add(dbo);
                }
                queryHandler.handle(entity, criterion, dbo);
            }
        }
    }

    private Object createObjectFromDBObject(DBObject dbObject) {
        final Object id = dbObject.get(MongoEntityPersister.MONGO_ID_FIELD);
        return mongoEntityPersister.createObjectFromNativeEntry(getEntity(), (Serializable) id, dbObject);
    }

    private Object wrapObjectResultInList(Object object) {
        List result = new ArrayList();
        result.add(object);
        return result;
    }

    private static interface QueryHandler<T> {
        public void handle(PersistentEntity entity, T criterion, DBObject query);
    }

    public static class MongoResultList extends AbstractList{
        private List<DBObject> results;
        private Object[] objectResults;
        private MongoEntityPersister mongoEntityPersister;

        public MongoResultList(DBCursor cursor, MongoEntityPersister mongoEntityPersister) {
            this.results = cursor.toArray();
            objectResults = new Object[results.size()];
            this.mongoEntityPersister = mongoEntityPersister;
        }

        @Override
        public Object get(int index) {
            Object object = objectResults[index];
            if(object == null) {
                final DBObject dbObject = results.get(index);
                Object id = dbObject.get(MongoEntityPersister.MONGO_ID_FIELD);
                object = mongoEntityPersister.createObjectFromNativeEntry(mongoEntityPersister.getPersistentEntity(), (Serializable) id, dbObject);
                objectResults[index] = object;
            }
            return  object;

        }

        @Override
        public int size() {
            return results.size();
        }
    }
}
