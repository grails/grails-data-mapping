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

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.springframework.datastore.document.DocumentStoreConnectionCallback;
import org.springframework.datastore.document.mongodb.MongoTemplate;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.mongo.MongoSession;
import org.springframework.datastore.mapping.mongo.engine.MongoEntityPersister;
import org.springframework.datastore.mapping.query.Query;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A {@link org.springframework.datastore.mapping.query.Query} implementation for the Mongo document store
 *
 * @author Graeme Rocher
 * @since 1.0
 */

public class MongoQuery extends Query{
    private MongoSession mongoSession;
    private MongoEntityPersister mongoEntityPersister;
    public MongoQuery(MongoSession session, PersistentEntity entity) {
        super(session, entity);
        this.mongoSession = session;
        this.mongoEntityPersister = (MongoEntityPersister) session.getPersister(entity);
    }

    @Override
    protected List executeQuery(PersistentEntity entity, Junction criteria) {
        if(criteria.isEmpty()) {

            final MongoTemplate template = mongoSession.getMongoTemplate(entity);

            return (List) template.execute(new DocumentStoreConnectionCallback<DB, Object>(){

                public Object doInConnection(DB db) throws Exception {

                    final DBCollection collection = db.getCollection(template.getDefaultCollectionName());
                    if(uniqueResult) {
                        final DBObject dbObject = collection.findOne();
                        final Object id = dbObject.get(MongoEntityPersister.MONGO_ID_FIELD);
                        final Object object = mongoEntityPersister.createObjectFromNativeEntry(getEntity(), (Serializable) id, dbObject);
                        List result = new ArrayList();
                        result.add(object);
                        return result;
                    }
                    else {
                        final DBCursor cursor = collection.find();
                        if(offset > 0) {
                            cursor.skip(offset);
                        }
                        if(max > -1) {
                            cursor.limit(max);
                        }

                        return new MongoResultList(cursor, mongoEntityPersister);
                    }
                }

            });
        }
        else {

        }
        return Collections.emptyList();
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
