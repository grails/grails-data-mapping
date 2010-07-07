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
import org.springframework.datastore.engine.PropertyValueIndexer;
import org.springframework.datastore.mapping.PersistentProperty;
import org.springframework.datastore.redis.util.RedisCallback;
import org.springframework.datastore.redis.util.RedisTemplate;

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

    public RedisPropertyValueIndexer(JRedis jredis, PersistentProperty property) {
        this.template = new RedisTemplate(jredis);
        this.property = property;
    }

    public void index(final Object value, final Long primaryKey) {
        template.execute(new RedisCallback(){
            public Object doInRedis(JRedis jredis) throws RedisException {
                jredis.rpush(createRedisKey(value), primaryKey);
                return null;
            }
        });
    }

    private String createRedisKey(Object value) {
        return property.getOwner().getName()+ ":" + property.getName() + ":" + value;
    }

    public List<Long> query(Object value) {
        return (List<Long>) template.execute(new RedisCallback(){
            public Object doInRedis(JRedis jredis) throws RedisException {
                return null;  // TODO: Querying of the index
            }
        });
    }
}
