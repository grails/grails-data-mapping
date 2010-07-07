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

/**
 * @author Graeme Rocher
 * @since 1.1
 */
public interface RedisCallback {

    /**
     * Executes redis logic whilst handling exce
     * @param jredis The jredis instance
     * @return The result of the calling jredis
     *
     * @throws org.jredis.RedisException When an error occurs invoking a Redis command
     */
    public Object doInRedis(JRedis jredis) throws RedisException;
}
