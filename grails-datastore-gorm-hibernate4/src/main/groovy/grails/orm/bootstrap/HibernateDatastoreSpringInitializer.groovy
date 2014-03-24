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
package grails.orm.bootstrap

import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.orm.hibernate.*
import org.codehaus.groovy.grails.orm.hibernate.support.ClosureEventTriggeringInterceptor
import org.codehaus.groovy.grails.orm.hibernate.support.HibernateDialectDetectorFactoryBean
import org.codehaus.groovy.grails.orm.hibernate.validation.HibernateDomainClassValidator
import org.grails.datastore.gorm.bootstrap.AbstractDatastoreInitializer
import org.grails.datastore.gorm.config.GrailsDomainClassMappingContext
import org.hibernate.EmptyInterceptor
import org.hibernate.SessionFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.MethodInvokingFactoryBean
import org.springframework.beans.factory.config.PropertiesFactoryBean
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.support.GenericApplicationContext
import org.springframework.jdbc.support.nativejdbc.CommonsDbcpNativeJdbcExtractor

import javax.sql.DataSource

/**
 * Class that handles the details of initializing GORM for Hibernate
 *
 * @author Graeme Rocher
 * @since 3.0
 */
class HibernateDatastoreSpringInitializer extends AbstractDatastoreInitializer {
    public static final String SESSION_FACTORY_BEAN_NAME = "sessionFactory"

    private String dataSourceBeanName = "dataSource"
    private String sessionFactoryBeanName = "sessionFactory"

    HibernateDatastoreSpringInitializer() {
    }

    HibernateDatastoreSpringInitializer(ClassLoader classLoader, String... packages) {
        super(classLoader, packages)
    }
    HibernateDatastoreSpringInitializer(String... packages) {
        super(packages)
    }

    HibernateDatastoreSpringInitializer(Collection<Class> persistentClasses) {
        super(persistentClasses)
    }

    HibernateDatastoreSpringInitializer(Class... persistentClasses) {
        super(persistentClasses.toList())
    }

    HibernateDatastoreSpringInitializer(Properties hibernateProperties, Collection<Class> persistentClasses) {
        super(hibernateProperties, persistentClasses)
    }

    HibernateDatastoreSpringInitializer(Properties hibernateProperties, Class... persistentClasses) {
        super(hibernateProperties, persistentClasses.toList())
    }

    public setDataSourceBeanName(String dataSourceBeanName) {
        this.dataSourceBeanName = dataSourceBeanName
    }

    @CompileStatic
    ApplicationContext configureForDataSource(DataSource dataSource) {
        ExpandoMetaClass.enableGlobally()
        GenericApplicationContext applicationContext = new GenericApplicationContext()
        applicationContext.beanFactory.registerSingleton(dataSourceBeanName, dataSource)
        configureForBeanDefinitionRegistry(applicationContext)
        applicationContext.refresh()
        return applicationContext
    }


    public Closure getBeanDefinitions(BeanDefinitionRegistry beanDefinitionRegistry) {
        Closure beanDefinitions = {
            def common = getCommonConfiguration(beanDefinitionRegistry)
            common.delegate = delegate
            common.call()

            Object vendorToDialect = getVenderToDialectMappings()
            "dialectDetector"(HibernateDialectDetectorFactoryBean) {
                dataSource = ref(dataSourceBeanName)
                vendorNameDialectMappings = vendorToDialect
            }

            if (!configuration['hibernate.hbm2ddl.auto']) {
                configuration['hibernate.hbm2ddl.auto'] = 'update'
            }
            if (!configuration['hibernate.dialect']) {
                configuration['hibernate.dialect'] = ref("dialectDetector")
            }

            "hibernateProperties"(PropertiesFactoryBean) { bean ->
                bean.scope = "prototype"
                properties = this.configuration
            }

            eventTriggeringInterceptor(ClosureEventTriggeringInterceptor)
            nativeJdbcExtractor(CommonsDbcpNativeJdbcExtractor)
            hibernateEventListeners(HibernateEventListeners)
            entityInterceptor(EmptyInterceptor)
            sessionFactory(ConfigurableLocalSessionFactoryBean) { bean ->
                bean.autowire = "byType"
                dataSource = ref(dataSourceBeanName)
                delegate.hibernateProperties = ref('hibernateProperties')
                grailsApplication = ref(GrailsApplication.APPLICATION_ID)
                eventListeners = [
                        'save': eventTriggeringInterceptor,
                        'save-update': eventTriggeringInterceptor,
                        'pre-load': eventTriggeringInterceptor,
                        'post-load': eventTriggeringInterceptor,
                        'pre-insert': eventTriggeringInterceptor,
                        'post-insert': eventTriggeringInterceptor,
                        'pre-update': eventTriggeringInterceptor,
                        'post-update': eventTriggeringInterceptor,
                        'pre-delete': eventTriggeringInterceptor,
                        'post-delete': eventTriggeringInterceptor]
                hibernateEventListeners = ref('hibernateEventListeners')

            }

            grailsHibernateConfig(MethodInvokingFactoryBean) { bean ->
                targetObject = ref(GrailsApplication.APPLICATION_ID)
                targetMethod = "getConfig"
            }
            grailsDomainClassMappingContext(GrailsDomainClassMappingContext, ref(GrailsApplication.APPLICATION_ID))
            hibernateDatastore(HibernateDatastore, ref('grailsDomainClassMappingContext'), ref(sessionFactoryBeanName), ref('grailsHibernateConfig'))

            if (!beanDefinitionRegistry.containsBeanDefinition("transactionManager")) {
                transactionManager(GrailsHibernateTransactionManager) { bean ->
                    bean.autowire = "byName"
                    sessionFactory = ref(sessionFactoryBeanName)
                    dataSource = ref(dataSourceBeanName)
                }
            }

            "org.grails.gorm.hibernate.internal.GORM_ENHANCER_BEAN-${dataSourceBeanName}"(HibernateGormEnhancer, ref("hibernateDatastore"), ref("transactionManager"), ref("grailsApplication")) { bean ->
                bean.initMethod = 'enhance'
                bean.lazyInit = false
            }

            "org.grails.gorm.hibernate.internal.POST_INIT_BEAN-${dataSourceBeanName}"(PostInitializationHandling) { bean ->
                grailsApplication = ref(GrailsApplication.APPLICATION_ID)
                bean.lazyInit = false
            }
        }
        beanDefinitions
    }

    protected Properties getVenderToDialectMappings() {
        def vendorToDialect = new Properties()
        def hibernateDialects = Thread.currentThread().contextClassLoader.getResource("hibernate-dialects.properties")
        if (hibernateDialects) {
            def p = new Properties()
            hibernateDialects.withInputStream { InputStream input ->
                p.load(input)
            }

            for (entry in p) {
                vendorToDialect[entry.value] = "org.hibernate.dialect.${entry.key}".toString()
            }
        }
        vendorToDialect
    }

    @CompileStatic
    static class PostInitializationHandling implements InitializingBean, ApplicationContextAware {

        @Autowired
        GrailsApplication grailsApplication

        ApplicationContext applicationContext

        @Override
        void afterPropertiesSet() throws Exception {
            grailsApplication.setMainContext(applicationContext)

            def hibernateDatastores = applicationContext.getBeansOfType(HibernateDatastore)
            def datastoreMap = new HashMap<SessionFactory, HibernateDatastore>()
            for (HibernateDatastore hibernateDatastore : hibernateDatastores.values()) {
                datastoreMap[hibernateDatastore.sessionFactory] = hibernateDatastore
            }
            applicationContext.getBean(ClosureEventTriggeringInterceptor).datastores = datastoreMap
        }
    }

}
