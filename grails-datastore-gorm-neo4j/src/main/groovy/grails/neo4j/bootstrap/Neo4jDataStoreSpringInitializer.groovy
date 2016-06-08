
/*
 * Copyright 2015 original authors
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
package grails.neo4j.bootstrap

import grails.neo4j.Neo4jEntity
import groovy.transform.InheritConstructors
import org.grails.datastore.gorm.bootstrap.AbstractDatastoreInitializer
import org.grails.datastore.gorm.events.ConfigurableApplicationContextEventPublisher
import org.grails.datastore.gorm.events.DefaultApplicationEventPublisher
import org.grails.datastore.gorm.neo4j.Neo4jDatastore
import org.grails.datastore.gorm.plugin.support.PersistenceContextInterceptorAggregator
import org.grails.datastore.gorm.support.AbstractDatastorePersistenceContextInterceptor
import org.grails.datastore.gorm.support.DatastorePersistenceContextInterceptor
import org.grails.datastore.mapping.validation.BeanFactoryValidatorRegistry
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.util.ClassUtils

/**
 * An {@link AbstractDatastoreInitializer} used for initializing GORM for Neo4j when using Spring.
 *
 * If you are not using the Spring container then using the constructors of {@link Neo4jDatastore} is preferable
 *
 * @author Graeme Rocher
 * @since 4.0
 */
@InheritConstructors
class Neo4jDataStoreSpringInitializer extends AbstractDatastoreInitializer {
    public static final String DATASTORE_TYPE = "neo4j"

    protected Closure defaultMapping

    @Override
    protected Class<AbstractDatastorePersistenceContextInterceptor> getPersistenceInterceptorClass() {
        DatastorePersistenceContextInterceptor
    }

    @Override
    protected boolean isMappedClass(String datastoreType, Class cls) {
        return Neo4jEntity.isAssignableFrom(cls) || super.isMappedClass(datastoreType, cls)
    }

    @Override
    Closure getBeanDefinitions(BeanDefinitionRegistry beanDefinitionRegistry) {
        {->
            def callable = getCommonConfiguration(beanDefinitionRegistry, DATASTORE_TYPE)
            callable.delegate = delegate
            callable.call()

            ApplicationEventPublisher eventPublisher
            if(beanDefinitionRegistry instanceof ConfigurableApplicationContext){
                eventPublisher = new ConfigurableApplicationContextEventPublisher((ConfigurableApplicationContext)beanDefinitionRegistry)
            }
            else {
                eventPublisher = new DefaultApplicationEventPublisher()
            }
            neo4jDatastore(Neo4jDatastore, configuration, eventPublisher, collectMappedClasses(DATASTORE_TYPE))
            neo4jMappingContext(neo4jDatastore:"getMappingContext") {
                validatorRegistry = new BeanFactoryValidatorRegistry((BeanFactory)beanDefinitionRegistry)
            }
            neo4jTransactionManager(neo4jDatastore:"getTransactionManager")
            neo4jDriver(neo4jDatastore:"getBoltDriver")
            neo4jPersistenceInterceptor(getPersistenceInterceptorClass(), ref("neo4jDatastore"))
            neo4jPersistenceContextInterceptorAggregator(PersistenceContextInterceptorAggregator)


            def transactionManagerBeanName = TRANSACTION_MANAGER_BEAN
            if (!containsRegisteredBean(delegate, beanDefinitionRegistry, transactionManagerBeanName)) {
                beanDefinitionRegistry.registerAlias("neo4jTransactionManager", transactionManagerBeanName)
            }

            def classLoader = getClass().getClassLoader()
            if (beanDefinitionRegistry.containsBeanDefinition('dispatcherServlet') && ClassUtils.isPresent(OSIV_CLASS_NAME, classLoader)) {
                String interceptorName = "neo4jOpenSessionInViewInterceptor"
                "${interceptorName}"(ClassUtils.forName(OSIV_CLASS_NAME, classLoader)) {
                    datastore = ref("neo4jDatastore")
                }
            }
        }
    }

    /**
     * Sets the default Neo4j GORM mapping configuration
     */
    @Deprecated
    void setDefaultMapping(Closure defaultMapping) {
        this.defaultMapping = defaultMapping
    }
}
