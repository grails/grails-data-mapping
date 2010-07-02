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
package org.springframework.datastore.redis;

import org.springframework.datastore.core.AbstractDatastore;
import org.springframework.datastore.core.Session;
import org.springframework.datastore.keyvalue.mapping.KeyValueMappingContext;
import org.springframework.datastore.mapping.MappingContext;

import java.util.Map;

/**
 * A Datastore implementation for the Redis key/value datastore
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class RedisDatastore extends AbstractDatastore {

    public RedisDatastore(MappingContext mappingContext) {
        super(mappingContext);
    }

    public RedisDatastore() {
        super(new KeyValueMappingContext(""));
    }

    @Override
    protected Session createConnection(Map<String, String> connectionDetails) {
        return new RedisSession(connectionDetails, getMappingContext());
    }
}
