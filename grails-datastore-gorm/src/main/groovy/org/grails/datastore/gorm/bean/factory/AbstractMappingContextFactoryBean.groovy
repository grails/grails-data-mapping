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
package org.grails.datastore.gorm.bean.factory

import grails.core.GrailsApplication
import grails.core.GrailsDomainClass
import grails.core.support.GrailsApplicationAware
import groovy.transform.CompileStatic
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.datastore.gorm.proxy.GroovyProxyFactory
import org.grails.datastore.mapping.engine.types.CustomTypeMarshaller
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.springframework.beans.factory.FactoryBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.validation.Validator

/**
 * An abstract factory bean for constructing MappingContext instances
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
abstract class AbstractMappingContextFactoryBean implements FactoryBean<MappingContext>, GrailsApplicationAware, ApplicationContextAware {

    private static final Log LOG = LogFactory.getLog(AbstractMappingContextFactoryBean)

    GrailsApplication grailsApplication
    ApplicationContext applicationContext
    String mappingStrategy
    boolean defaultExternal

    MappingContext getObject() {
        def mappingContext = createMappingContext()
        mappingContext.proxyFactory = new GroovyProxyFactory()

        registerCustomTypeMarshallers(mappingContext)

        if (mappingStrategy == null) {
            mappingStrategy = (getClass().simpleName - 'MappingContextFactoryBean').toLowerCase()
        }

        if (grailsApplication) {
            for (GrailsDomainClass domainClass in (GrailsDomainClass[])grailsApplication.getArtefacts(DomainClassArtefactHandler.TYPE)) {
                def domainMappingStrategy = domainClass.mappingStrategy
                PersistentEntity entity

                if (mappingStrategy == domainMappingStrategy || (domainMappingStrategy == 'GORM' && !defaultExternal)) {
                    entity = mappingContext.addPersistentEntity(domainClass.clazz)
                }
                else {
                    entity = mappingContext.addExternalPersistentEntity(domainClass.clazz)
                }
                if (entity) {
                    final validatorBeanName = "${domainClass.fullName}Validator"
                    Validator validator = applicationContext.containsBean(validatorBeanName) ? applicationContext.getBean(validatorBeanName, Validator) : null

                    if (validator) {
                        mappingContext.addEntityValidator(entity, validator)
                    }
                }
            }
        }
        return mappingContext
    }

    protected void registerCustomTypeMarshallers(MappingContext mappingContext) {
        try {
            final typeMarshallers = applicationContext.getBeansOfType(CustomTypeMarshaller)
            final mappingFactory = mappingContext.mappingFactory
            for (marshaller in typeMarshallers.values()) {
                mappingFactory.registerCustomType(marshaller)
            }
        } catch (e) {
            LOG.error("Error configuring custom type marshallers: " + e.getMessage(), e)
        }
    }

    protected abstract MappingContext createMappingContext()

    Class<?> getObjectType() { MappingContext }

    boolean isSingleton() { true }
}
