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
import org.springframework.datastore.keyvalue.mapping.Family;
import org.springframework.datastore.mapping.ClassMapping;
import org.springframework.datastore.mapping.PersistentEntity;
import org.springframework.datastore.mapping.PersistentProperty;
import org.springframework.datastore.redis.collection.RedisSet;
import org.springframework.datastore.redis.query.RedisQueryUtils;
import org.springframework.datastore.redis.util.RedisCallback;
import org.springframework.datastore.redis.util.RedisTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;
import java.util.Set;

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


    public RedisPropertyValueIndexer(RedisTemplate template, SimpleTypeConverter typeConverter, PersistentProperty property) {
        this.template = template;
        this.property = property;
        this.typeConverter = typeConverter;

    }

    protected String getFamily(PersistentEntity persistentEntity) {
        ClassMapping<Family> cm = persistentEntity.getMapping();
        String table = null;
        if(cm.getMappedForm() != null) {
            table = cm.getMappedForm().getFamily();
        }
        if(table == null) table = persistentEntity.getJavaClass().getName();
        return table;
    }


    public void index(final Object value, final Long primaryKey) {
        if(value != null) {
            final RedisSet set = new RedisSet(template, createRedisKey(value));
            set.add(primaryKey);

            // for numbers and dates we also create a list index in order to support range queries
            if(value instanceof Number) {
//               template.execute(new RedisCallback(){
//                   public Object doInRedis(JRedis jredis) throws RedisException {
//                       String key = set.getRedisKey();
//
//                       String entityKey = getFamily(property.getOwner());
//                       try {
//                           jredis.sort(key)
//                                 .BY(entityKey + ":*->" + property.getName())
//                                 .STORE(key + ":sorted")
//                                 .exec();
//                       } catch (ProviderException e) {
//                          // TODO: Workaround. JRedis doesn't support the store command properly
//                       }
//                       return null;
//                   }
//               });

            }
            else if(value instanceof Date) {
                // TODO: Support range queries for dates
            }
        }
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
        String[] results;
        if(offset > 0 || max > 0) {
            results = set.members(offset, max);
        }
        else {
            results = set.members();
        }
        return RedisQueryUtils.transformRedisResults(typeConverter, results);
    }

    public String getIndexName(Object value) {
        return createRedisKey(value);
    }

    public String getIndexPattern(String pattern) {
        String root = getIndexRoot();

        return root + urlEncode(pattern.replaceAll("%", "*"));
    }

}
