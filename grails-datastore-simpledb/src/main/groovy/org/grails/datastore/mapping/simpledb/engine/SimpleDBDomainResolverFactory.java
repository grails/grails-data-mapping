/* Copyright (C) 2011 SpringSource
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
package org.grails.datastore.mapping.simpledb.engine;

import org.grails.datastore.mapping.model.ClassMapping;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.simpledb.SimpleDBDatastore;
import org.grails.datastore.mapping.simpledb.config.SimpleDBDomainClassMappedForm;

/**
 * Encapsulates logic of building appropriately configured SimpleDBDomainResolver instance.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
public class SimpleDBDomainResolverFactory {

    public SimpleDBDomainResolver buildResolver(PersistentEntity entity, SimpleDBDatastore simpleDBDatastore) {
        @SuppressWarnings("unchecked")
        ClassMapping<SimpleDBDomainClassMappedForm> classMapping = entity.getMapping();
        SimpleDBDomainClassMappedForm mappedForm = classMapping.getMappedForm();
        String entityFamily = getFamily(entity, mappedForm);

        if (mappedForm.isShardingEnabled()) {
            throw new RuntimeException("sharding is not implemented yet");
        }

        return new ConstSimpleDBDomainResolver(entityFamily, simpleDBDatastore.getDomainNamePrefix());
    }

    protected String getFamily(PersistentEntity persistentEntity, SimpleDBDomainClassMappedForm mappedForm) {
        String table = null;
        if (mappedForm != null) {
            table = mappedForm.getFamily();
        }
        if (table == null) table = persistentEntity.getJavaClass().getName();
        return table;
    }
}
