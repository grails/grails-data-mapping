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
import org.grails.datastore.mapping.simpledb.util.SimpleDBConst;
import org.grails.datastore.mapping.simpledb.util.SimpleDBUtil;
import org.springframework.beans.propertyeditors.UUIDEditor;

import java.util.Map;

/**
 * Encapsulates logic of building appropriately configured SimpleDBIdGenerator instance.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
public class SimpleDBIdGeneratorFactory {

    public SimpleDBIdGenerator buildIdGenerator(PersistentEntity entity, SimpleDBDatastore simpleDBDatastore) {
        String entityFamily = SimpleDBUtil.getMappedDomainName(entity);

        @SuppressWarnings("unchecked")
        ClassMapping<SimpleDBDomainClassMappedForm> classMapping = entity.getMapping();
        SimpleDBDomainClassMappedForm mappedForm = classMapping.getMappedForm();

        Map<String, Object> generatorInfo = mappedForm.getId_generator();

        //by default use uuid generator
        if (generatorInfo == null || generatorInfo.isEmpty()) {
            return new SimpleDBUUIDIdGenerator();
        }

        String generatorType = (String) generatorInfo.get(SimpleDBConst.PROP_ID_GENERATOR_TYPE);
        if (SimpleDBConst.PROP_ID_GENERATOR_TYPE_UUID.equals(generatorType)) {
            return new SimpleDBUUIDIdGenerator();
        } else if ((SimpleDBConst.PROP_ID_GENERATOR_TYPE_HILO.equals(generatorType))) {
            Integer lowSize = (Integer) generatorInfo.get(SimpleDBConst.PROP_ID_GENERATOR_MAX_LO);
            if (lowSize == null) {
                lowSize = SimpleDBConst.PROP_ID_GENERATOR_MAX_LO_DEFAULT_VALUE; // default value
            }
            String hiloDomainName = SimpleDBUtil.getPrefixedDomainName(simpleDBDatastore.getDomainNamePrefix(), SimpleDBConst.ID_GENERATOR_HI_LO_DOMAIN_NAME);
            return new SimpleDBHiLoIdGenerator(hiloDomainName, entityFamily, lowSize, simpleDBDatastore.getSimpleDBTemplate());
        } else {
            throw new IllegalArgumentException("unknown id generator type for simpledb: " + generatorType + ". Current implementation supports only " +
                    SimpleDBConst.PROP_ID_GENERATOR_TYPE_UUID + " and " + SimpleDBConst.PROP_ID_GENERATOR_TYPE_HILO);
        }
    }
}
