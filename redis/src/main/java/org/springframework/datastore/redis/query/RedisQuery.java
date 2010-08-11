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
package org.springframework.datastore.redis.query;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.datastore.core.Session;
import org.springframework.datastore.engine.PropertyValueIndexer;
import org.springframework.datastore.keyvalue.mapping.KeyValue;
import org.springframework.datastore.mapping.PersistentEntity;
import org.springframework.datastore.mapping.PersistentProperty;
import org.springframework.datastore.query.Query;
import org.springframework.datastore.redis.collection.RedisCollection;
import org.springframework.datastore.redis.engine.RedisEntityPersister;

import java.util.Collections;
import java.util.List;

/**
 * A Query implementation for Redis
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class RedisQuery extends Query {
    private RedisEntityPersister entityPersister;

    public RedisQuery(Session session, PersistentEntity persistentEntity, RedisEntityPersister entityPersister) {
        super(session, persistentEntity);
        this.entityPersister = entityPersister;
    }

    @Override
    protected List executeQuery(PersistentEntity entity, List<Criterion> criteria) {
        if(criteria == null || criteria.isEmpty()) {
            List<byte[]> results;
            RedisCollection col = entityPersister.getAllEntityIndex();
            if(offset > 0 || max > -1) {
                results = col.members(offset, max);
            }
            else {
                results = col.members();
            }

            final List<Long> identifiers = RedisQueryUtils.transformRedisResults(entityPersister.getTypeConverter(), results);

            return new RedisEntityResultList(getSession(), getEntity(), identifiers);
        }
        else {
            if(criteria.size() == 1) {
                Criterion c = criteria.get(0);
                if(c instanceof Equals) {
                    Equals eq = (Equals) c;
                    PersistentProperty property = getEntity().getPropertyByName(eq.getName());
                    KeyValue kv = (KeyValue) property.getMapping().getMappedForm();
                    if(kv.isIndex()) {
                        PropertyValueIndexer indexer = entityPersister.getPropertyIndexer(property);
                        List identifiers = indexer.query(eq.getValue(), offset, max);

                        return new RedisEntityResultList(getSession(), getEntity(), identifiers);
                    }
                    else {
                        throw new DataIntegrityViolationException("Cannot query on class ["+getEntity()+"] on property ["+property+"]. The property is not indexed!");
                    }
                }
            }
        }
        return Collections.emptyList();
    }
}
