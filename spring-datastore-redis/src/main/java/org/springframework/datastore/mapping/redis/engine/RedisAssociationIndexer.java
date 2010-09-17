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
package org.springframework.datastore.mapping.redis.engine;

import org.springframework.core.convert.ConversionService;
import org.springframework.datastore.mapping.engine.AssociationIndexer;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.model.types.Association;
import org.springframework.datastore.mapping.redis.collection.RedisCollection;
import org.springframework.datastore.mapping.redis.collection.RedisList;
import org.springframework.datastore.mapping.redis.collection.RedisSet;
import org.springframework.datastore.mapping.redis.query.RedisQueryUtils;
import org.springframework.datastore.mapping.redis.util.RedisTemplate;

import java.util.Collection;
import java.util.List;

/**
 * An indexer for Redis
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class RedisAssociationIndexer implements AssociationIndexer<Long, Long> {
    private RedisTemplate template;
    private ConversionService typeConverter;
    private Association association;

    public RedisAssociationIndexer(RedisTemplate template, ConversionService typeConverter, Association association) {
        this.template = template;
        this.typeConverter = typeConverter;
        this.association = association;
    }

    public void index(Long primaryKey, final List<Long> foreignKeys) {
        final String redisKey = createRedisKey(primaryKey);
        RedisCollection col = createRedisCollection(redisKey);
        col.addAll(foreignKeys);
    }

    public void index(Long primaryKey, Long foreignKey) {
        final String redisKey = createRedisKey(primaryKey);
        RedisCollection col = createRedisCollection(redisKey);
        col.add(foreignKey);
    }

    private String createRedisKey(Long primaryKey) {
        return association.getOwner().getName()+ ":" + primaryKey + ":" + association.getName();
    }

    public List<Long> query(Long primaryKey) {
        String redisKey = createRedisKey(primaryKey);
        return queryInternal(redisKey);
    }

    public PersistentEntity getIndexedEntity() {
        return association.getAssociatedEntity();
    }

    private List<Long> queryInternal(final String redisKey) {
        RedisCollection col = createRedisCollection(redisKey);
        return queryRedisCollection(col);
    }

    private List<Long> queryRedisCollection(RedisCollection col) {
        Collection<String> results = col.members();
        return RedisQueryUtils.transformRedisResults(typeConverter, results);
    }

    private RedisCollection createRedisCollection(String redisKey) {
        RedisCollection col;
        if(association.isList()) {
            col = new RedisList(template, redisKey);
        }
        else {
            col = new RedisSet(template, redisKey);
        }
        return col;
    }

}
