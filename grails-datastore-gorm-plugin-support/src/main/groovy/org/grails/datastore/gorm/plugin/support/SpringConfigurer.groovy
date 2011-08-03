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

package org.grails.datastore.gorm.plugin.support

import org.grails.datastore.mapping.transactions.DatastoreTransactionManager
import org.grails.datastore.gorm.support.DatastorePersistenceContextInterceptor
import org.grails.datastore.mapping.web.support.OpenSessionInViewInterceptor
import org.codehaus.groovy.grails.commons.GrailsServiceClass
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.validation.GrailsDomainClassValidator
import org.springframework.transaction.annotation.Transactional
import org.springframework.core.annotation.AnnotationUtils
import java.lang.reflect.Method
import org.springframework.beans.factory.support.AbstractBeanDefinition

/**
 * Helper class for use by plugins in configuring Spring
 *
 * @author Graeme Rocher
 * @since 1.0
 */
abstract class SpringConfigurer {

    /**
     * The name of the datastore type (example "Mongo" or "Neo4j")
     * @return
     */
    abstract String getDatastoreType()

    /**
     * Additional Spring configuration that is specific to the underlying Datastore. The returned closure should use BeanBuilder syntax and must as a minimum
     * define two beans named "${datastoreType.toLowerCase()}Datastore" and "${datastoreType.toLowerCase()}MappingContext" (Example "neo4jDatastore" and "neo4jMappingContext"
     *
     * @return BeanBuilder syntax closure.
     */
    abstract Closure getSpringCustomizer()

    public Closure getConfiguration() {
        return configureSpring( getSpringCustomizer() )
    }

    protected Closure configureSpring(Closure customizer) {

        String type = getDatastoreType()
        String typeLower = type.toLowerCase()


        return {

            "${typeLower}TransactionManager"(DatastoreTransactionManager) {
                datastore = ref("${typeLower}Datastore")
            }


            "${typeLower}PersistenceInterceptor"(DatastorePersistenceContextInterceptor, ref("${typeLower}Datastore"))

            "${typeLower}PersistenceContextInterceptorAggregator"(PersistenceContextInterceptorAggregator)

            if (manager?.hasGrailsPlugin("controllers")) {
                String interceptorName = "${typeLower}OpenSessionInViewInterceptor"
                "${interceptorName}"(OpenSessionInViewInterceptor) {
                    datastore = ref("${typeLower}Datastore")
                }
                if (getSpringConfig().containsBean("controllerHandlerMappings")) {
                    controllerHandlerMappings.interceptors << ref(interceptorName)
                }
                if (getSpringConfig().containsBean("annotationHandlerMapping")) {
                    if (annotationHandlerMapping.interceptors) {
                        annotationHandlerMapping.interceptors << ref(interceptorName)
                    }
                    else {
                        annotationHandlerMapping.interceptors = [ref(interceptorName)]
                    }
                }
            }

            // need to fix the service proxies to use TransactionManager
            for (serviceGrailsClass in application.serviceClasses) {
                GrailsServiceClass serviceClass = serviceGrailsClass

                if (!shouldCreateTransactionalProxy(typeLower, serviceClass)) {
                    continue
                }

                def beanName = serviceClass.propertyName
                if (springConfig.containsBean(beanName)) {
                    delegate."$beanName".transactionManager = ref("${typeLower}TransactionManager")
                }
            }

            // make sure validators for domain classes are regular GrailsDomainClassValidator
            def isHibernateInstalled = manager.hasGrailsPlugin("hibernate")
            for (dc in application.domainClasses) {
                def cls = dc.clazz
                def cpf = ClassPropertyFetcher.forClass(cls)
                def mappedWith = cpf.getStaticPropertyValue(GrailsDomainClassProperty.MAPPING_STRATEGY, String)
                if (mappedWith == typeLower || (!isHibernateInstalled && mappedWith == null)) {
                    String validatorBeanName = "${dc.fullName}Validator"
                    AbstractBeanDefinition beandef = springConfig.getBeanConfig(validatorBeanName)?.beanDefinition ?:
                                                        springConfig.getBeanDefinition(validatorBeanName)
                    // remove the session factory attribute if present
                    beandef.getPropertyValues().removePropertyValue("sessionFactory")
                    beandef.beanClassName = GrailsDomainClassValidator.name
                }
            }
            customizer.delegate = delegate
            customizer.call()
        }
    }

    boolean shouldCreateTransactionalProxy(String type, GrailsServiceClass serviceClass) {

        if (serviceClass.getStaticPropertyValue('transactional', Boolean)) {
            // leave it as a regular proxy
            return false
        }

        if (!type.equals(serviceClass.getStaticPropertyValue('transactional', String))) {
            return false
        }

        try {
            Class javaClass = serviceClass.clazz
            serviceClass.transactional &&
                !AnnotationUtils.findAnnotation(javaClass, Transactional) &&
                !javaClass.methods.any { Method m -> AnnotationUtils.findAnnotation(m, Transactional) != null }
        }
        catch (e) {
            return false
        }
    }
}
