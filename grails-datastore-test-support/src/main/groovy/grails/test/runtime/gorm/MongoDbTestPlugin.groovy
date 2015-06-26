/*
 * Copyright 2014 the original author or authors.
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

package grails.test.runtime.gorm

import grails.core.GrailsApplication
import grails.mongodb.bootstrap.MongoDbDataStoreSpringInitializer
import grails.test.runtime.TestEvent
import grails.test.runtime.TestPlugin
import grails.test.runtime.TestRuntime
import groovy.transform.CompileStatic
import org.grails.config.PropertySourcesConfig
import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.datastore.gorm.GormEnhancer
import org.springframework.beans.factory.support.BeanDefinitionRegistry

import com.mongodb.Mongo
import org.springframework.core.env.MutablePropertySources
import org.springframework.core.env.PropertiesPropertySource

/**
 * a TestPlugin for TestRuntime for adding Grails DomainClass (GORM) support
 * 
 * @author Lari Hotari
 * @since 2.4.1
 *
 */
@CompileStatic
class MongoDbTestPlugin implements TestPlugin {
    String[] requiredFeatures = ['grailsApplication', 'coreBeans']
    String[] providedFeatures = ['domainClass', 'gorm', 'mongoDbGorm']
    int ordinal = 1
    
     /**
     * Sets up a GORM for Hibernate domain
     *
     * @param persistentClasses
     */
    void mongoDomain(TestRuntime runtime, Map parameters) {
        Collection<Class> persistentClasses = [] as Set
        persistentClasses.addAll((Collection<Class>)parameters.domains)
        boolean immediateDelivery = true
        if(runtime.containsValueFor("mongoDbPersistentClassesToRegister")) {
            Collection<Class<?>> allPersistentClasses = runtime.getValue("mongoDbPersistentClassesToRegister", Collection)
            allPersistentClasses.addAll(persistentClasses)
            immediateDelivery = false
        }

        Mongo mongo = (Mongo)parameters.mongo

        Properties initializerConfig
        if(parameters.config instanceof Map) {
            initializerConfig = new Properties()
            parameters.config.each { k, v ->
                initializerConfig.setProperty(k.toString(), v?.toString())
            }
        }
        
        if(immediateDelivery) {
            Collection<Class<?>> previousPersistentClasses = runtime.getValue("initializedMongoPersistentClasses", Collection)
            if(!previousPersistentClasses?.containsAll(persistentClasses) || initializerConfig || mongo) {
                if(previousPersistentClasses) {
                    persistentClasses.addAll(previousPersistentClasses)
                }
                registerMongoDomains(runtime, runtime.getValueIfExists("grailsApplication", GrailsApplication), persistentClasses, initializerConfig, mongo, true)
            }
        } else {
            if(initializerConfig) {
                runtime.putValue("mongoInitializerConfig", initializerConfig)
            }
            if(mongo != null) {
                runtime.putValue("mongoInstance", mongo)
            }
        }
    }
    
    void defineBeans(TestRuntime runtime, boolean immediateDelivery = true, Closure<?> closure) {
        runtime.publishEvent("defineBeans", [closure: closure], [immediateDelivery: immediateDelivery])
    }
    
    GrailsApplication getGrailsApplication(TestRuntime runtime) {
        runtime.getValue("grailsApplication", GrailsApplication)
    }
    
    void registerBeans(TestRuntime runtime, GrailsApplication grailsApplication) {
        if(runtime.containsValueFor("mongoDbPersistentClassesToRegister")) {
            Collection<Class<?>> persistentClasses = runtime.removeValue("mongoDbPersistentClassesToRegister", Collection)
            registerMongoDomains(runtime, grailsApplication, persistentClasses, runtime.getValueIfExists("mongoInitializerConfig", Properties), runtime.getValueIfExists("mongoInstance", Mongo), false)
        }
    }
   
    void registerMongoDomains(TestRuntime runtime, GrailsApplication grailsApplication, Collection<Class<?>> persistentClasses, Properties initializerConfig, Mongo mongo, boolean immediateDelivery) {
        for(cls in persistentClasses) {
            grailsApplication.addArtefact(DomainClassArtefactHandler.TYPE, cls)
        }
        def initializer = new MongoDbDataStoreSpringInitializer(persistentClasses)
        if(initializerConfig) {
            def propertySources = new MutablePropertySources()
            propertySources.addFirst(new PropertiesPropertySource("mongo-test-config", initializerConfig))
            initializer.configuration = new PropertySourcesConfig(propertySources)
        }
        if(mongo) {
            initializer.mongo = mongo
        }
        def context = grailsApplication.getMainContext()
        def beansClosure = initializer.getBeanDefinitions((BeanDefinitionRegistry)context)
        runtime.putValue('initializedMongoPersistentClasses', Collections.unmodifiableList(new ArrayList(persistentClasses)))
        defineBeans(runtime, immediateDelivery, beansClosure)
        if(immediateDelivery) {
            enhanceDomains(runtime, grailsApplication)
        }
    }

    void onTestEvent(TestEvent event) {
        switch(event.name) {
            case 'registerBeans':
                registerBeans(event.runtime, (GrailsApplication)event.arguments.grailsApplication)
                break
            case 'afterClass':
                event.runtime.removeValue("mongoDbPersistentClassesToRegister")
                break
            case 'beforeClass':
                Collection<Class<?>> persistentClasses = [] as Set
                persistentClasses.addAll(GormTestPluginUtil.collectDomainClassesFromAnnotations((Class<?>)event.arguments.testClass, event.runtime.getSharedRuntimeConfigurer()?.getClass()))
                event.runtime.putValue('mongoDbPersistentClassesToRegister', persistentClasses)
                break
            case 'mongoDomain':
                mongoDomain(event.runtime, event.arguments)
                break
            case 'applicationInitialized':
                enhanceDomains(event.runtime, (GrailsApplication)event.arguments.grailsApplication)
                break
        }
    }
    
    void close(TestRuntime runtime) {
        runtime.removeValue('initializedMongoPersistentClasses')
    }
    
    void enhanceDomains(TestRuntime runtime, GrailsApplication grailsApplication) {
        grailsApplication.mainContext.getBeansOfType(GormEnhancer)
    }
}


