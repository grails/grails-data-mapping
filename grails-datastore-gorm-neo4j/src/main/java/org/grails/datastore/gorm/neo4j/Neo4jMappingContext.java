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
package org.grails.datastore.gorm.neo4j;

//import org.grails.datastore.gorm.neo4j.converters.*;
import org.grails.datastore.mapping.engine.NonPersistentTypeException;
import org.grails.datastore.mapping.model.AbstractMappingContext;
import org.grails.datastore.mapping.model.MappingConfigurationStrategy;
import org.grails.datastore.mapping.model.MappingFactory;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.config.GormMappingConfigurationStrategy;

import java.util.Collection;

/**
 * @author Stefan Armbruster <stefan@armbruster-it.de>
 */
public class Neo4jMappingContext extends AbstractMappingContext  {

    MappingFactory mappingFactory = new GraphGormMappingFactory();
    MappingConfigurationStrategy mappingSyntaxStrategy = new GormMappingConfigurationStrategy(mappingFactory);

    public Neo4jMappingContext() {
        super();
//        addTypeConverter(new LazyEntitySetToSetConverter());
    }

    @Override
    public MappingConfigurationStrategy getMappingSyntaxStrategy() {
        return mappingSyntaxStrategy;
    }

    @Override
    public MappingFactory getMappingFactory() {
        return mappingFactory;
    }

    @Override
    protected PersistentEntity createPersistentEntity(Class javaClass) {
        PersistentEntity persistentEntity = new GraphPersistentEntity(javaClass, this);
//        mappingFactory.createMappedForm(persistentEntity) // populates mappingFactory.entityToPropertyMap as a side effect
        return persistentEntity;
    }


    public GraphPersistentEntity findPersistentEntityForLabels(Collection<String> labels) {
        for (PersistentEntity pe : getPersistentEntities()) {
            GraphPersistentEntity gpe = (GraphPersistentEntity) pe;

            if (gpe.getLabels().equals(labels)) {
                return gpe;
            }
        }
        throw new NonPersistentTypeException(labels.toString());
    }

//    MappingFactory getMappingFactory() {
//        mappingFactory
//    }
}
