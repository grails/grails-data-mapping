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
import org.springframework.datastore.engine.PropertyValueIndexer;
import org.springframework.datastore.keyvalue.mapping.KeyValue;
import org.springframework.datastore.mapping.PersistentEntity;
import org.springframework.datastore.mapping.PersistentProperty;
import org.springframework.datastore.query.Query;
import org.springframework.datastore.redis.RedisSession;
import org.springframework.datastore.redis.collection.RedisCollection;
import org.springframework.datastore.redis.engine.RedisEntityPersister;
import org.springframework.datastore.redis.util.RedisTemplate;

import java.util.ArrayList;
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
    private RedisTemplate template;

    public RedisQuery(RedisSession session, PersistentEntity persistentEntity, RedisEntityPersister entityPersister) {
        super(session, persistentEntity);
        this.entityPersister = entityPersister;
        template = new RedisTemplate(session.getNativeInterface());
    }

    @Override
    protected List executeQuery(PersistentEntity entity, Junction criteria) {
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
            List<Criterion> criteriaList = criteria.getCriteria();
            if(criteriaList.size() == 1) {
                Criterion c = criteriaList.get(0);
                if(c instanceof Equals) {
                    Equals eq = (Equals) c;
                    PersistentProperty property = getEntity().getPropertyByName(eq.getName());
                    boolean indexed = isIndexed(property);
                    if(indexed) {
                        PropertyValueIndexer indexer = entityPersister.getPropertyIndexer(property);
                        List identifiers = indexer.query(eq.getValue(), offset, max);

                        return new RedisEntityResultList(getSession(), getEntity(), identifiers);
                    }
                    else {
                        throw new DataIntegrityViolationException("Cannot query on class ["+getEntity()+"] on property ["+property+"]. The property is not indexed!");
                    }
                }
            }
            else {
                List<String> indices = getIndexNames(criteria, entityPersister);
                List<byte[]> results;
                if(criteria instanceof Conjunction) {
                   List<String> remainingKeys = indices.subList(1, indices.size());
                   results = template.sinter(indices.get(0), remainingKeys.toArray(new String[remainingKeys.size()]));
                }
                else {
                    List<String> remainingKeys = indices.subList(1, indices.size());
                    results = template.sunion(indices.get(0), remainingKeys.toArray(new String[remainingKeys.size()]));
                }
                final List<Long> identifiers = RedisQueryUtils.transformRedisResults(entityPersister.getTypeConverter(), results);
                return new RedisEntityResultList(getSession(), getEntity(), identifiers);
            }
        }
        return Collections.emptyList();
    }

    private boolean isIndexed(PersistentProperty property) {
        KeyValue kv = (KeyValue) property.getMapping().getMappedForm();
        return kv.isIndex();
    }

    private List<String> getIndexNames(Junction criteria, RedisEntityPersister entityPersister) {

        List<Criterion> criteriaList = criteria.getCriteria();
        List<String> indices = new ArrayList<String>();
        for (Criterion criterion : criteriaList) {
            if(criterion instanceof Equals) {
                Equals eq = (Equals) criterion;
                PersistentProperty prop = getEntity().getPropertyByName(eq.getName());
                if(prop == null) {
                    throw new DataIntegrityViolationException("Cannot execute query. Entity ["+getEntity()+"] does not declare a property named ["+eq.getName()+"]");
                }
                else if(!isIndexed(prop)) {
                    throw new DataIntegrityViolationException("Cannot query class ["+getEntity()+"] on property ["+prop+"]. The property is not indexed!");
                }

                PropertyValueIndexer indexer = entityPersister.getPropertyIndexer(prop);
                indices.add( indexer.getIndexName(eq.getValue()) );
            }
        }
        
        return indices;
    }
}
