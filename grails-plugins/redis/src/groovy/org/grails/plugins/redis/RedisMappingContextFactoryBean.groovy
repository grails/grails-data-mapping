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
package org.grails.plugins.redis

import org.springframework.beans.factory.FactoryBean
import org.springframework.datastore.mapping.MappingContext
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.springframework.datastore.keyvalue.mapping.KeyValueMappingContext
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware
import org.springframework.datastore.mapping.PersistentEntity
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ApplicationContext

/**
 *
 */
class RedisMappingContextFactoryBean implements FactoryBean<MappingContext>, GrailsApplicationAware, ApplicationContextAware{

  GrailsApplication grailsApplication
  GrailsPluginManager pluginManager
  ApplicationContext applicationContext

  MappingContext getObject() {
    def mappingContext = new KeyValueMappingContext("");
    def isHibernateInstalled = pluginManager.hasGrailsPlugin("hibernate")

    if(grailsApplication) {
      for(GrailsDomainClass domainClass in grailsApplication.domainClasses){
         PersistentEntity entity = null
         if(!isHibernateInstalled) {
           entity = mappingContext.addPersistentEntity(domainClass.clazz)

         }
         else {
           def mappedWith = domainClass.getPropertyValue(GrailsDomainClassProperty.MAPPING_STRATEGY, String)
           if(mappedWith == 'redis') {
              entity = mappingContext.addPersistentEntity(domainClass.clazz)
           }
         }

        if(entity) {
          final validatorBeanName = "${domainClass.fullName}Validator"
          def validator = applicationContext.containsBean(validatorBeanName) ? applicationContext.getBean(validatorBeanName) : null

          if(validator)
            mappingContext.addEntityValidator(entity, validator)
        }
      }
    }
    return mappingContext
  }

  Class<?> getObjectType() { MappingContext }

  boolean isSingleton() { true }
}
