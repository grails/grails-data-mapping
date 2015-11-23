package org.grails.datastore.gorm.cassandra.boot.autoconfigure

import grails.cassandra.bootstrap.CassandraDatastoreSpringInitializer
import groovy.transform.CompileStatic
import org.grails.datastore.mapping.cassandra.CassandraDatastore
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.BeanFactoryAware
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.boot.autoconfigure.AutoConfigurationPackages
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.cassandra.CassandraProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.EnvironmentAware
import org.springframework.context.ResourceLoaderAware
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.core.env.Environment
import org.springframework.core.io.ResourceLoader
import org.springframework.core.type.AnnotationMetadata

@CompileStatic
@Configuration
@ConditionalOnMissingBean(CassandraDatastore)
@AutoConfigureAfter(org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration)
class CassandraAutoConfiguration implements BeanFactoryAware, ResourceLoaderAware, ImportBeanDefinitionRegistrar, EnvironmentAware{

    BeanFactory beanFactory

    ResourceLoader resourceLoader

    Environment environment

    @Autowired
    CassandraProperties cassandraProperties

    @Override
    void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        def packages = AutoConfigurationPackages.get(beanFactory)
        def classLoader = ((ConfigurableBeanFactory)beanFactory).getBeanClassLoader()

        def initializer = new CassandraDatastoreSpringInitializer(classLoader, packages as String[])
        initializer.resourceLoader = resourceLoader
        initializer.setConfiguration(environment)
        if(cassandraProperties != null) {
            initializer.setDefaultKeyspaceName(cassandraProperties.keyspaceName)
        }
        initializer.configureForBeanDefinitionRegistry(registry)
    }


    @Override
    void setEnvironment(Environment environment) {
        this.environment = environment;
    }

}