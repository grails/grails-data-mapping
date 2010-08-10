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

import org.jredis.JRedis;
import org.jredis.RedisException;
import org.jredis.Sort;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.dao.DataAccessException;
import org.springframework.datastore.engine.PropertyValueIndexer;
import org.springframework.datastore.mapping.PersistentProperty;
import org.springframework.datastore.redis.query.RedisQueryUtils;
import org.springframework.datastore.redis.util.RedisCallback;
import org.springframework.datastore.redis.util.RedisTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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

    public RedisPropertyValueIndexer(JRedis jredis, SimpleTypeConverter typeConverter, PersistentProperty property) {
        this.template = new RedisTemplate(jredis);
        this.property = property;
        this.typeConverter = typeConverter;
    }

    public void index(final Object value, final Long primaryKey) {
        if(value != null) {
            template.execute(new RedisCallback(){
                public Object doInRedis(JRedis jredis) throws RedisException {
                    jredis.sadd(createRedisKey(value), primaryKey);
                    return null;
                }
            });
        }
    }

    private String createRedisKey(Object value) {
        try {
            return property.getOwner().getName()+ ":" + property.getName() + ":" + URLEncoder.encode(value.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new DataAccessException("Cannot encoding Redis key: " + e.getMessage(), e){};
        }
    }

    public List<Long> query(final Object value) {
        return query(value, 0, -1);
    }

    public List<Long> query(final Object value, final int offset, final int max) {
        return (List<Long>) template.execute(new RedisCallback() {

            public Object doInRedis(JRedis jredis) throws RedisException {
                String redisKey = createRedisKey(value);
                final List<byte[]> results;
                if(offset > 0 || max > 0) {
                    Sort sort = jredis.sort(redisKey).LIMIT(offset, max);
                    results = sort.exec();
                }
                else {
                    results = jredis.smembers(redisKey);
                }

                return RedisQueryUtils.transformRedisResults(typeConverter, results);
            }
        });
    }

}
