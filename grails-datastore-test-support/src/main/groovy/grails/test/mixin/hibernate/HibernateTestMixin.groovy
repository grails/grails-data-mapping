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
package grails.test.mixin.hibernate

import grails.test.mixin.support.GrailsUnitTestMixin
import grails.test.mixin.support.SkipMethod
import grails.test.runtime.TestPluginRegistrar
import grails.test.runtime.TestPluginUsage
import grails.test.runtime.gorm.HibernateTestPlugin
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import javax.sql.DataSource

import org.hibernate.Session
import org.hibernate.SessionFactory
import org.springframework.orm.hibernate4.SessionHolder
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * A Mixin that sets up a Hibernate domain model
 *
 * @author Graeme Rocher
 * @since 3.0.4
 */
@CompileStatic
class HibernateTestMixin extends GrailsUnitTestMixin implements TestPluginRegistrar {
    private static final Set<String> REQUIRED_FEATURES = (["hibernateGorm"] as Set<String>).asImmutable()
    
    public HibernateTestMixin(Set<String> features) {
        super((REQUIRED_FEATURES + features) as Set<String>)
    }
    
    public HibernateTestMixin() {
        super(REQUIRED_FEATURES)
    }
    
    /**
     * Sets up a GORM for Hibernate domain for the given domain classes
     *
     * @param persistentClasses
     */
    void hibernateDomain(Collection<Class> persistentClasses) {
        getRuntime().publishEvent("hibernateDomain", [domains: persistentClasses])
    }
    
    void hibernateDomain(DataSource dataSource, Collection<Class> persistentClasses) {
        getRuntime().publishEvent("hibernateDomain", [domains: persistentClasses, dataSource: dataSource])
    }

    /**
     * Sets up a GORM for Hibernate domain for the given configuration and domain classes
     *
     * @param persistentClasses
     */
    void hibernateDomain(Map config, Collection<Class> persistentClasses) {
        getRuntime().publishEvent("hibernateDomain", [domains: persistentClasses, config: config])
    }
    
    @SkipMethod
    public Iterable<TestPluginUsage> getTestPluginUsages() {
        return TestPluginUsage.createForActivating(HibernateTestPlugin)
    }

    @CompileDynamic
    public PlatformTransactionManager getTransactionManager() {
        getMainContext().getBean("transactionManager", PlatformTransactionManager)
    }
    
    public Session getHibernateSession() {
        Object value = TransactionSynchronizationManager.getResource(getSessionFactory());
        if (value instanceof Session) {
            return (Session) value;
        }
        if (value instanceof SessionHolder) {
            SessionHolder sessionHolder = (SessionHolder) value;
            return sessionHolder.getSession();
        }
        return null
    }

    @CompileDynamic
    public SessionFactory getSessionFactory() {
        getMainContext().getBean("sessionFactory", SessionFactory)
    }

}
