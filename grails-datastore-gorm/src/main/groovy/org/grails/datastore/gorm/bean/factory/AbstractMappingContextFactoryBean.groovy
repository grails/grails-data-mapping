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

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware
import org.grails.datastore.gorm.proxy.GroovyProxyFactory
import org.springframework.beans.factory.FactoryBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.datastore.mapping.model.MappingContext
import org.springframework.datastore.mapping.model.PersistentEntity

/**
 * An abstract factory bean for constructing MappingContext instances
 *
 * @author Graeme Rocher
 * @since 1.0
 */
abstract class AbstractMappingContextFactoryBean implements FactoryBean<MappingContext>, GrailsApplicationAware, ApplicationContextAware {

    GrailsApplication grailsApplication
    GrailsPluginManager pluginManager
    ApplicationContext applicationContext
    String mappingStrategy
    boolean defaultExternal

    MappingContext getObject() {
        def mappingContext = createMappingContext();
        mappingContext.proxyFactory = new GroovyProxyFactory()

        if (mappingStrategy == null) {
            mappingStrategy = (getClass().simpleName - 'MappingContextFactoryBean').toLowerCase()
        }

        if (grailsApplication) {
            for (GrailsDomainClass domainClass in grailsApplication.domainClasses) {
                def domainMappingStrategy = domainClass.getPropertyValue(GrailsDomainClassProperty.MAPPING_STRATEGY)
                PersistentEntity entity

                if (mappingStrategy == domainMappingStrategy || (domainMappingStrategy == 'GORM' && !defaultExternal)) {
                    entity = mappingContext.addPersistentEntity(domainClass.clazz)
                }
                else {
                    entity = mappingContext.addExternalPersistentEntity(domainClass.clazz)
                }
                if (entity) {
                    final validatorBeanName = "${domainClass.fullName}Validator"
                    def validator = applicationContext.containsBean(validatorBeanName) ? applicationContext.getBean(validatorBeanName) : null

                    if (validator) {
                        mappingContext.addEntityValidator(entity, validator)
                    }
                }
            }
        }
        return mappingContext
    }

    protected abstract MappingContext createMappingContext()

    Class<?> getObjectType() { MappingContext }

    boolean isSingleton() { true }
}
