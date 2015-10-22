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

import org.grails.datastore.mapping.engine.NonPersistentTypeException;
import org.grails.datastore.mapping.model.AbstractMappingContext;
import org.grails.datastore.mapping.model.MappingConfigurationStrategy;
import org.grails.datastore.mapping.model.MappingFactory;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.config.GormMappingConfigurationStrategy;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A {@link org.grails.datastore.mapping.model.MappingContext} implementation for Neo4j
 *
 * @author Stefan Armbruster (stefan@armbruster-it.de)
 * @author Graeme Rocher
 *
 * @since 1.0
 */
public class Neo4jMappingContext extends AbstractMappingContext  {

    MappingFactory mappingFactory = new GraphGormMappingFactory();
    MappingConfigurationStrategy mappingSyntaxStrategy = new GormMappingConfigurationStrategy(mappingFactory);

    protected Map<Collection<String>, GraphPersistentEntity> entitiesByLabel = new LinkedHashMap<Collection<String>, GraphPersistentEntity>();

    public Neo4jMappingContext() {
        super();
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
        final GraphPersistentEntity entity = new GraphPersistentEntity(javaClass, this);
        final Collection<String> labels = entity.getLabels();
        entitiesByLabel.put(labels, entity);
        return entity;
    }


    public GraphPersistentEntity findPersistentEntityForLabels(Collection<String> labels) {
        final GraphPersistentEntity entity = entitiesByLabel.get(labels);
        if(entity != null) {
            return entity;
        }
        throw new NonPersistentTypeException(labels.toString());
    }

}
