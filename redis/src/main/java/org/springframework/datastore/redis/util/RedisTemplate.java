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
package org.springframework.datastore.redis.util;

import org.jredis.JRedis;
import org.jredis.RedisException;
import org.springframework.dao.DataAccessException;

/**
 * A Spring-style template for querying Redis and translating
 * JRedis exceptions into Spring exceptions
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class RedisTemplate {

    JRedis jredis;

    public RedisTemplate(JRedis jredis) {
        this.jredis = jredis;
    }

    public Object execute(RedisCallback callback) {
        try {
            return callback.doInRedis(jredis);
        } catch (RedisException e) {
            throw new DataAccessException("Exception occured executing Redis command: " + e.getMessage(), e) {};
        }
    }
}
