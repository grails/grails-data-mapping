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
package org.grails.inconsequential.kv.mapping;

import org.grails.inconsequential.mapping.MappingFactory;
import org.grails.inconsequential.mapping.PersistentEntity;
import org.grails.inconsequential.mapping.PersistentProperty;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
public class KeyValueMappingFactory extends MappingFactory<KeyMapping,KeyValueMapping> {

    protected String keyspace;

    public KeyValueMappingFactory(String keyspace) {
        this.keyspace = keyspace;
    }

    @Override
    public KeyValueMapping createMappedForm(PersistentEntity entity) {
        return new KeyValueMapping(keyspace, entity.getName());
    }

    @Override
    public KeyMapping createMappedForm(PersistentProperty mpp) {
        return new KeyMapping(mpp.getName());
    }
}
