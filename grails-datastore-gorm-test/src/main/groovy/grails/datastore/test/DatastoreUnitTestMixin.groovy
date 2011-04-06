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
package grails.datastore.test

import grails.test.MockUtils

import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass
import org.codehaus.groovy.grails.plugins.DefaultGrailsPluginManager
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.plugins.PluginManagerHolder
import org.codehaus.groovy.grails.validation.GrailsDomainClassValidator
import org.grails.datastore.gorm.GormEnhancer
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.datastore.mapping.core.Session
import org.springframework.datastore.mapping.simple.SimpleMapDatastore
import org.springframework.datastore.mapping.transactions.DatastoreTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.util.ClassUtils

/**
 * <p>A Groovy mixin used for testing datastore interactions. Test cases should include the mixin using
 * the Groovy @Mixin transformation:</p>
 *
 * <pre><code>
 *  @Mixin(DatastoreUnitTestMixin)
 *  class MyTests {}
 * </code></pre>
 *
 * <p>
 * The {@link DatastoreUnitTestMixin#connect()} method should be called in the test cases setUp() method
 * and the {@link DatastoreUnitTestMixin#disconnect()} method on the tearDown() method.
 * </p>
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class DatastoreUnitTestMixin {

    static ConfigurableApplicationContext ctx
    static SimpleMapDatastore datastore
    static {
        ctx = new GenericApplicationContext()
        ctx.refresh()
        datastore = new SimpleMapDatastore(ctx)
    }
 
    Session session
    PlatformTransactionManager transactionManager = new DatastoreTransactionManager(datastore:datastore)

    private mockPluginManager = [hasGrailsPlugin: { String name ->
        if (name == "hibernate") {
            return ClassUtils.isPresent("org.hibernate.mapping.Value", getClass().getClassLoader())
        }
        return true
    }] as GrailsPluginManager

    Session connect() {
        session = datastore.connect()
        return session
    }

    def mockDomain(Class domainClass, List instances = []) {
        if (session == null) {
            datastore.clearData()
            session = datastore.connect()
        }

        if (!(PluginManagerHolder.pluginManager instanceof DefaultGrailsPluginManager)) {
            PluginManagerHolder.pluginManager = mockPluginManager
        }

        def entity = datastore.mappingContext.addPersistentEntity(domainClass)
        def enhancer = new GormEnhancer(datastore, transactionManager)
        enhancer.enhance entity
        MockUtils.prepareForConstraintsTests(entity.javaClass)
        def dc = new DefaultGrailsDomainClass(entity.javaClass)
        datastore.mappingContext.addEntityValidator(entity, new GrailsDomainClassValidator(domainClass:dc))
        instances.each {
            it.metaClass = GroovySystem.metaClassRegistry.getMetaClass(domainClass)
            session.persist(it)
        }
        session.flush()
    }

    def disconnect() {
        session?.disconnect()
        datastore.clearData()
    }
}
