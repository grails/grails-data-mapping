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
package org.grails.datastore.mapping.dynamodb.engine;

import org.grails.datastore.mapping.model.ClassMapping;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.dynamodb.DynamoDBDatastore;
import org.grails.datastore.mapping.dynamodb.config.DynamoDBDomainClassMappedForm;
import org.grails.datastore.mapping.dynamodb.util.DynamoDBConst;
import org.grails.datastore.mapping.dynamodb.util.DynamoDBUtil;

import java.util.Map;

/**
 * Encapsulates logic of building appropriately configured DynamoDBIdGenerator instance.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
public class DynamoDBIdGeneratorFactory {

    public DynamoDBIdGenerator buildIdGenerator(PersistentEntity entity, DynamoDBDatastore dynamoDBDatastore) {
        String entityFamily = DynamoDBUtil.getMappedTableName(entity);

        @SuppressWarnings("unchecked")
        ClassMapping<DynamoDBDomainClassMappedForm> classMapping = entity.getMapping();
        DynamoDBDomainClassMappedForm mappedForm = classMapping.getMappedForm();

        Map<String, Object> generatorInfo = mappedForm.getId_generator();

        //by default use uuid generator
        if (generatorInfo == null || generatorInfo.isEmpty()) {
            return new DynamoDBUUIDIdGenerator();
        }

        String generatorType = (String) generatorInfo.get(DynamoDBConst.PROP_ID_GENERATOR_TYPE);
        if (DynamoDBConst.PROP_ID_GENERATOR_TYPE_UUID.equals(generatorType)) {
            return new DynamoDBUUIDIdGenerator();
        } else if ((DynamoDBConst.PROP_ID_GENERATOR_TYPE_HILO.equals(generatorType))) {
            Integer lowSize = (Integer) generatorInfo.get(DynamoDBConst.PROP_ID_GENERATOR_MAX_LO);
            if (lowSize == null) {
                lowSize = DynamoDBConst.PROP_ID_GENERATOR_MAX_LO_DEFAULT_VALUE; // default value
            }
            String hiloDomainName = DynamoDBUtil.getPrefixedTableName(dynamoDBDatastore.getTableNamePrefix(), DynamoDBConst.ID_GENERATOR_HI_LO_TABLE_NAME);
            return new DynamoDBHiLoIdGenerator(hiloDomainName, entityFamily, lowSize, dynamoDBDatastore);
        } else {
            throw new IllegalArgumentException("unknown id generator type for dynamodb: " + generatorType + ". Current implementation supports only " +
                    DynamoDBConst.PROP_ID_GENERATOR_TYPE_UUID + " and " + DynamoDBConst.PROP_ID_GENERATOR_TYPE_HILO);
        }
    }
}
