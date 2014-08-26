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
package org.grails.datastore.gorm.cassandra.bean.factory


import groovy.transform.Canonical

import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.grails.datastore.gorm.bean.factory.AbstractMappingContextFactoryBean
import org.grails.datastore.gorm.proxy.GroovyProxyFactory
import org.grails.datastore.mapping.cassandra.config.CassandraMappingContext
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.springframework.util.Assert
import org.springframework.util.ClassUtils

/**
 * Factory bean for construction the Cassandra MappingContext.
 *
 */
class CassandraMappingContextFactoryBean extends AbstractMappingContextFactoryBean {
	String keyspace
	DefaultMappingHolder defaultMapping
	
    /**
     * What must be specified as a value of 'mapWith' to map the
     * domain class with the Cassandra Gorm plugin if the Hibernate plugin is
     * also installed
     *
     * <pre>
     * class Person {
     *      String id
     *      String firstName
     *      static mapWith = "cassandra"
     * }
     * </pre>
     */
    static final String CASSANDRA_MAP_WITH_VALUE = "cassandra"
    
    @Override
    public MappingContext getObject() {
        def mappingContext = createMappingContext()
        mappingContext.proxyFactory = new GroovyProxyFactory()

        registerCustomTypeMarshallers(mappingContext)

        if (mappingStrategy == null) {
            mappingStrategy = CASSANDRA_MAP_WITH_VALUE
        }        
        def isHibernateInstalled = ClassUtils.isPresent("org.codehaus.groovy.grails.orm.hibernate.AbstractHibernateDatastore", getClass().getClassLoader())       
        //For now, only add domain classes to mappingContext if mapped by 'cassandra' or hibernate not installed       
        if (grailsApplication) {            
            for (GrailsDomainClass domainClass in grailsApplication.domainClasses) {                
                def domainMappingStrategy = domainClass.mappingStrategy
                PersistentEntity entity 
                               
                if (domainMappingStrategy == mappingStrategy || !isHibernateInstalled) {                    
                    entity = mappingContext.addPersistentEntity(domainClass.clazz)
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

    @Override
    protected MappingContext createMappingContext() {
		Assert.hasText(keyspace, "Property [keyspace] must be set!")
        new CassandraMappingContext(keyspace, defaultMapping?.defaultMapping)        
    }
}

@Canonical
class DefaultMappingHolder {
	Closure defaultMapping
}
