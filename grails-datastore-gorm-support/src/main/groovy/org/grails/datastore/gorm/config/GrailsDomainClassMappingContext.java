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

package org.grails.datastore.gorm.config;

import grails.core.GrailsApplication;
import grails.core.GrailsClass;
import grails.core.GrailsDomainClass;
import org.grails.core.artefact.DomainClassArtefactHandler;
import org.grails.datastore.mapping.core.Datastore;
import org.grails.datastore.mapping.model.AbstractMappingContext;
import org.grails.datastore.mapping.model.MappingConfigurationStrategy;
import org.grails.datastore.mapping.model.MappingFactory;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;

/**
 * A MappingContext that adapts the Grails domain model to the Mapping API.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings("rawtypes")
public class GrailsDomainClassMappingContext extends AbstractMappingContext implements ApplicationContextAware {

    private GrailsApplication grailsApplication;
    private MappingConfigurationStrategy configurationStrategy;
    private MappingFactory mappingFactory;
    private ApplicationContext applicationContext;

    public GrailsDomainClassMappingContext(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;

        final GrailsClass[] artefacts = grailsApplication.getArtefacts(DomainClassArtefactHandler.TYPE);
        Class[] persistentClasses = new Class[artefacts.length];
        int i=0;
        for (GrailsClass grailsClass : artefacts) {
            persistentClasses[i++] = grailsClass.getClazz();
        }
        addPersistentEntities(persistentClasses);
    }

    public GrailsApplication getGrailsApplication() {
        return grailsApplication;
    }

    public MappingConfigurationStrategy getMappingSyntaxStrategy() {
        if(configurationStrategy == null) {
            if(applicationContext != null) {
                Map<String, Datastore> datastoreBeans = applicationContext.getBeansOfType(Datastore.class);
                if(datastoreBeans.containsKey("hibernateDatastore")) {
                    Datastore datastore = datastoreBeans.get("hibernateDatastore");
                    this.configurationStrategy = datastore.getMappingContext().getMappingSyntaxStrategy();
                    return configurationStrategy;
                }
                else if(!datastoreBeans.isEmpty()) {
                    Datastore first = datastoreBeans.values().iterator().next();
                    this.configurationStrategy = first.getMappingContext().getMappingSyntaxStrategy();
                    return configurationStrategy;
                }
            }
            throw new IllegalStateException("No GORM implementation configured");
        }
        return configurationStrategy;
    }

    @Override
    public MappingFactory getMappingFactory() {
        if(mappingFactory == null) {
            if(applicationContext != null) {
                Map<String, Datastore> datastoreBeans = applicationContext.getBeansOfType(Datastore.class);
                if(datastoreBeans.containsKey("hibernateDatastore")) {
                    Datastore datastore = datastoreBeans.get("hibernateDatastore");
                    this.mappingFactory = datastore.getMappingContext().getMappingFactory();
                    return mappingFactory;
                }
                else if(!datastoreBeans.isEmpty()) {
                    Datastore first = datastoreBeans.values().iterator().next();
                    this.mappingFactory = first.getMappingContext().getMappingFactory();
                    return mappingFactory;
                }
            }
            throw new IllegalStateException("No GORM implementation configured");
        }
        return mappingFactory;

    }

    @Override
    protected PersistentEntity createPersistentEntity(Class javaClass) {
        GrailsDomainClass domainClass = (GrailsDomainClass) grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, javaClass.getName());
        if (domainClass == null)
            return null;
        return new GrailsDomainClassPersistentEntity(domainClass, this);
    }

    @Override
    protected PersistentEntity createPersistentEntity(Class javaClass, boolean external) {
        return createPersistentEntity(javaClass);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
