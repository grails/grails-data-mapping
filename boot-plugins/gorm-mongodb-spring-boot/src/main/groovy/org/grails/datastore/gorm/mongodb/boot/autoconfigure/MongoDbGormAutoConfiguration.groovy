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
import org.grails.datastore.mapping.mongo.MongoDatastore
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.BeanFactoryAware
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.boot.autoconfigure.AutoConfigurationPackages
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoProperties
import org.springframework.context.ResourceLoaderAware
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
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
class MongoDbGormAutoConfiguration implements BeanFactoryAware, ResourceLoaderAware, ImportBeanDefinitionRegistrar{

    @Autowired
    private MongoProperties properties;

    @Autowired(required = false)
    Properties mongoProperties = new Properties()

    @Autowired(required = false)
    Mongo mongo

    @Autowired(required = false)
    MongoOptions mongoOptions

    BeanFactory beanFactory

    ResourceLoader resourceLoader

    @Override
    void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        MongoDbDataStoreSpringInitializer initializer
        def packages = AutoConfigurationPackages.get(beanFactory)
        def classLoader = ((ConfigurableBeanFactory)beanFactory).getBeanClassLoader()

        initializer = new MongoDbDataStoreSpringInitializer(classLoader, packages as String[])
        initializer.resourceLoader = resourceLoader
        initializer.setConfiguration(mongoProperties)
        initializer.setMongo(mongo)
        initializer.setMongoOptions(mongoOptions)
        if(properties != null) {
            initializer.setDatabaseName(properties.database)
        }
        initializer.configureForBeanDefinitionRegistry(registry)
    }
}
