/* Copyright (C) 2014 SpringSource
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
package org.grails.datastore.gorm.mongodb.boot.autoconfigure

import com.mongodb.Mongo
import com.mongodb.MongoOptions
import grails.mongodb.bootstrap.MongoDbDataStoreSpringInitializer
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.mongo.MongoGormEnhancer
import org.grails.datastore.mapping.mongo.MongoDatastore
import org.springframework.beans.BeansException
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.BeanFactoryAware
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.boot.autoconfigure.AutoConfigurationPackages
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoProperties
import org.springframework.boot.bind.RelaxedPropertyResolver
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.EnvironmentAware
import org.springframework.context.MessageSource
import org.springframework.context.ResourceLoaderAware
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.core.env.Environment
import org.springframework.core.io.ResourceLoader
import org.springframework.core.type.AnnotationMetadata

/**
 *
 * Auto configurer that configures GORM for MongoDB for use in Spring Boot
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
@Configuration
@ConditionalOnMissingBean(MongoDatastore)
@AutoConfigureAfter(MongoAutoConfiguration)
class MongoDbGormAutoConfiguration implements BeanFactoryAware, ResourceLoaderAware, ImportBeanDefinitionRegistrar, EnvironmentAware{

    @Autowired
    private MongoProperties properties;

    @Autowired(required = false)
    Mongo mongo

    @Autowired(required = false)
    MongoOptions mongoOptions

    BeanFactory beanFactory

    ResourceLoader resourceLoader

    RelaxedPropertyResolver environment

    @Override
    void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        MongoDbDataStoreSpringInitializer initializer
        def packages = AutoConfigurationPackages.get(beanFactory)
        def classLoader = ((ConfigurableBeanFactory)beanFactory).getBeanClassLoader()

        initializer = new MongoDbDataStoreSpringInitializer(classLoader, packages as String[])
        initializer.resourceLoader = resourceLoader
        initializer.setConfiguration(getDatastoreConfiguration())
        initializer.setMongo(mongo)
        initializer.setMongoOptions(mongoOptions)
        if(properties != null) {
            initializer.setDatabaseName(properties.database)
        }
        initializer.configureForBeanDefinitionRegistry(registry)

        registry.registerBeanDefinition("org.grails.internal.gorm.mongodb.EAGER_INIT_PROCESSOR", new RootBeanDefinition(EagerInitProcessor))
    }

    protected Properties getDatastoreConfiguration() {
        if(environment != null) {
            def config = environment.getSubProperties("mongodb.")
            def properties = new Properties()
            for(entry in config.entrySet()) {
                properties.put("grails.mongodb.${entry.key}".toString(), entry.value)
            }
            return properties
        }
    }

    @Override
    void setEnvironment(Environment environment) {
        this.environment = new RelaxedPropertyResolver(environment, "spring.");
    }

    static class EagerInitProcessor implements BeanPostProcessor, ApplicationContextAware {

        ApplicationContext applicationContext
        private MessageSource messageSource
        private Map enhancers

        @Override
        Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
            if(messageSource != null && enhancers == null) {
                // force MongoDB enhancer initialisation
                enhancers = applicationContext.getBeansOfType(GormEnhancer)
            }
            return bean
        }

        @Override
        Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
            if(bean instanceof MessageSource) {
                messageSource = (MessageSource)bean
            }
            return bean
        }
    }
}
