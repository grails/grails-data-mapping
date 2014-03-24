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
package grails.test.mixin.mongodb

import com.mongodb.Mongo
import grails.mongodb.bootstrap.MongoDbDataStoreSpringInitializer
import grails.test.mixin.support.GrailsUnitTestMixin
import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.grails.datastore.gorm.GormEnhancer

/**
 * A test mixin that sets up MongoDB
 *
 * @author Graeme Rocher
 *
 */
@CompileStatic
class MongoDbTestMixin extends GrailsUnitTestMixin{

    /**
     * Sets up a GORM for MongoDB domain for the given domain classes
     *
     * @param persistentClasses
     */
    void mongoDomain(Collection<Class> persistentClasses) {
        def initializer = new MongoDbDataStoreSpringInitializer(persistentClasses)
        completeConfiguration(persistentClasses,initializer)
    }


    /**
     * Sets up a GORM for MongoDB domain for the given Mongo instance and domain classes
     *
     * @param persistentClasses
     */
    void mongoDomain(Mongo mongo, Collection<Class> persistentClasses) {
        def initializer = new MongoDbDataStoreSpringInitializer(persistentClasses)
        initializer.setMongo(mongo)
        completeConfiguration(persistentClasses,initializer)
    }

    /**
     * Sets up a GORM for MongoDB domain for the given configuration and domain classes
     *
     * @param persistentClasses
     */
    void mongoDomain(Map config, Collection<Class> persistentClasses) {
        def initializer = new MongoDbDataStoreSpringInitializer(persistentClasses)
        def props = new Properties()
        props.putAll(config)
        initializer.setConfiguration(props)
        completeConfiguration(persistentClasses,initializer)
    }

    protected void completeConfiguration(Collection<Class> persistentClasses, MongoDbDataStoreSpringInitializer initializer) {
        for(cls in persistentClasses) {
            grailsApplication.addArtefact(DomainClassArtefactHandler.TYPE, cls)
        }
        initializer.configureForBeanDefinitionRegistry(applicationContext)
        applicationContext.getBeansOfType(GormEnhancer)
    }

}
