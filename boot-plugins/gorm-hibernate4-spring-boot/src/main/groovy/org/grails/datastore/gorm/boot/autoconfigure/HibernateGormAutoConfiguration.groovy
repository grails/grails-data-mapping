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
import org.grails.config.PropertySourcesConfig
import org.grails.datastore.gorm.GormEnhancer
import org.grails.orm.hibernate.cfg.GrailsAnnotationConfiguration
import org.grails.orm.hibernate.cfg.HibernateMappingContextConfiguration
import org.hibernate.SessionFactory
import org.springframework.beans.BeansException
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.BeanFactoryAware
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.boot.autoconfigure.AutoConfigurationPackages
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.boot.bind.RelaxedPropertyResolver
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.EnvironmentAware
import org.springframework.context.ResourceLoaderAware
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.core.env.Environment
import org.springframework.core.env.MutablePropertySources
import org.springframework.core.env.PropertiesPropertySource
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
@ConditionalOnClass(HibernateMappingContextConfiguration)
@ConditionalOnBean(DataSource)
@ConditionalOnMissingBean(HibernateDatastoreSpringInitializer)
@AutoConfigureAfter(DataSourceAutoConfiguration)
@AutoConfigureBefore(HibernateJpaAutoConfiguration)
class HibernateGormAutoConfiguration implements BeanFactoryAware, ResourceLoaderAware, ImportBeanDefinitionRegistrar, EnvironmentAware{

    BeanFactory beanFactory

    ResourceLoader resourceLoader

    RelaxedPropertyResolver environment

    @Override
    void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        HibernateDatastoreSpringInitializer initializer
        def packages = AutoConfigurationPackages.get(beanFactory)
        def classLoader = ((ConfigurableBeanFactory)beanFactory).getBeanClassLoader()

        initializer = new HibernateDatastoreSpringInitializer(classLoader, packages as String[])
        initializer.resourceLoader = resourceLoader
        initializer.setConfiguration(environment)
        initializer.configureForBeanDefinitionRegistry(registry)

        registry.registerBeanDefinition("org.grails.internal.gorm.hibernate4.EAGER_INIT_PROCESSOR", new RootBeanDefinition(EagerInitProcessor))
    }


    @Override
    void setEnvironment(Environment environment) {
        this.environment = new RelaxedPropertyResolver(environment, "spring.");
    }

    static class EagerInitProcessor implements BeanPostProcessor, ApplicationContextAware {

        ApplicationContext applicationContext
        private SessionFactory sessionFactory
        private Map enhancers

        @Override
        Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
            if(sessionFactory != null && enhancers == null) {
                // force GORM enhancer initialisation
                applicationContext.getBean(HibernateDatastoreSpringInitializer.PostInitializationHandling)
                enhancers = applicationContext.getBeansOfType(GormEnhancer)
            }
            return bean
        }

        @Override
        Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
            if(bean instanceof SessionFactory) {
                sessionFactory = (SessionFactory)bean
            }
            return bean
        }
    }
}
