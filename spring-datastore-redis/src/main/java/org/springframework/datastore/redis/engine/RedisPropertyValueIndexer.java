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
package org.springframework.datastore.redis.engine;

import org.springframework.beans.SimpleTypeConverter;
import org.springframework.dao.DataAccessException;
import org.springframework.datastore.engine.PropertyValueIndexer;
import org.springframework.datastore.mapping.MappingContext;
import org.springframework.datastore.mapping.PersistentProperty;
import org.springframework.datastore.redis.collection.RedisSet;
import org.springframework.datastore.redis.query.RedisQueryUtils;
import org.springframework.datastore.redis.util.RedisTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Indexes property values for querying later
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class RedisPropertyValueIndexer implements PropertyValueIndexer<Long> {

    private RedisTemplate template;
    private PersistentProperty property;
    private SimpleTypeConverter typeConverter;
    private RedisEntityPersister entityPersister;
    private MappingContext mappingContext;


    public RedisPropertyValueIndexer(MappingContext context, RedisEntityPersister redisEntityPersister, PersistentProperty property) {
        this.template = redisEntityPersister.getRedisTemplate();
        this.typeConverter = redisEntityPersister.getTypeConverter();
        this.entityPersister = redisEntityPersister;
        this.property = property;
        this.mappingContext = context;
    }


    public void index(final Object value, final Long primaryKey) {
        if(value != null) {
            template.sadd(createRedisKey(value), primaryKey);
            // for numbers and dates we also create a list index in order to support range queries
            String propSortKey = entityPersister.getPropertySortKey(property);
            if(value instanceof Number) {
                Number n = (Number) value;
                template.zadd(propSortKey,n.doubleValue(),primaryKey);
                // delete any cached indices
                clearCachedIndices(propSortKey);
            }
            else if(value instanceof Date) {
                Date d = (Date) value;
                Long time = d.getTime();
                template.zadd(propSortKey,time.doubleValue(),primaryKey);
                clearCachedIndices(propSortKey);
            }
        }
    }

    private void clearCachedIndices(String propSortKey) {
        final List<String> toDelete = template.keys(propSortKey + "~*");
        if(toDelete != null && !toDelete.isEmpty())
            template.del(toDelete.toArray(new String[toDelete.size()]));
    }

    private String createRedisKey(Object value) {
        return getIndexRoot() + urlEncode(value);
    }

    private String urlEncode(Object value) {
        try {
            return URLEncoder.encode(value.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new DataAccessException("Cannot encoding Redis key: " + e.getMessage(), e){};
        }
    }

    private String getIndexRoot() {
        return property.getOwner().getName()+ ":" + property.getName() + ":";
    }

    public List<Long> query(final Object value) {
        return query(value, 0, -1);
    }

    public List<Long> query(final Object value, final int offset, final int max) {
        String redisKey = createRedisKey(value);

        RedisSet set = new RedisSet(template, redisKey);
        Collection<String> results;
        if(offset > 0 || max > 0) {
            results = set.members(offset, max);
        }
        else {
            results = set.members();
        }
        return RedisQueryUtils.transformRedisResults(mappingContext.getConversionService(), results);
    }

    public String getIndexName(Object value) {
        return createRedisKey(value);
    }

    public String getIndexPattern(String pattern) {
        String root = getIndexRoot();

        return root + urlEncode(pattern.replaceAll("%", "*"));
    }

}
