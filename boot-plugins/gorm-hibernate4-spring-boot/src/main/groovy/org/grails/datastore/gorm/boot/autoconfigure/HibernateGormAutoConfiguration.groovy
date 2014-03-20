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
package org.grails.datastore.gorm.boot.autoconfigure

import grails.orm.bootstrap.HibernateDatastoreSpringInitializer
import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsAnnotationConfiguration
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.BeanFactoryAware
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.boot.autoconfigure.AutoConfigurationPackages
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.context.ResourceLoaderAware
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.core.io.ResourceLoader
import org.springframework.core.type.AnnotationMetadata

import javax.sql.DataSource

/**
 * Auto configuration for GORM for Hibernate
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
@Configuration
@ConditionalOnClass(GrailsAnnotationConfiguration)
@ConditionalOnBean(DataSource)
@ConditionalOnMissingBean(HibernateDatastoreSpringInitializer)
@AutoConfigureAfter(DataSourceAutoConfiguration)
@AutoConfigureBefore(HibernateJpaAutoConfiguration)
class HibernateGormAutoConfiguration implements BeanFactoryAware, ResourceLoaderAware, ImportBeanDefinitionRegistrar{

    @Autowired(required = false)
    Properties hibernateProperties = new Properties()

    BeanFactory beanFactory

    ResourceLoader resourceLoader

    @Override
    void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        HibernateDatastoreSpringInitializer initializer
        def packages = AutoConfigurationPackages.get(beanFactory)
        def classLoader = ((ConfigurableBeanFactory)beanFactory).getBeanClassLoader()

        initializer = new HibernateDatastoreSpringInitializer(classLoader, packages as String[])
        initializer.resourceLoader = resourceLoader
        initializer.setHibernateProperties(hibernateProperties)
        initializer.configureForBeanDefinitionRegistry(registry)
    }
}
