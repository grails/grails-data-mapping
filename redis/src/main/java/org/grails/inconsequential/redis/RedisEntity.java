/* Copyright 2004-2005 the original author or authors.
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
package org.grails.inconsequential.redis;

import org.grails.inconsequential.kv.mapping.KeyValuePersistentEntity;
import org.grails.inconsequential.mapping.*;
import org.grails.inconsequential.mapping.types.Identity;

import java.beans.Introspector;

/**
 * Represents an entity that has been mapped to the Redis Datastore
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class RedisEntity extends KeyValuePersistentEntity implements PersistentEntity {
    public RedisEntity(Class javaClass, MappingContext context) {
        super(javaClass, context);
    }
}
