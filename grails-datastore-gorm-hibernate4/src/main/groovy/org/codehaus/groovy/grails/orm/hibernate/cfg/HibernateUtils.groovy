/* 
 * Copyright 2013 the original author or authors.
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
package org.codehaus.groovy.grails.orm.hibernate.cfg

import grails.artefact.Enhanced
import grails.util.GrailsNameUtils

import org.codehaus.groovy.grails.orm.hibernate.GrailsHibernateTemplate
import org.apache.commons.beanutils.PropertyUtils
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.orm.hibernate.GrailsHibernateTemplate
import org.codehaus.groovy.grails.orm.hibernate.HibernateDatastore
import org.codehaus.groovy.grails.orm.hibernate.HibernateGormEnhancer
import org.codehaus.groovy.grails.orm.hibernate.HibernateGormInstanceApi
import org.codehaus.groovy.grails.orm.hibernate.HibernateGormStaticApi
import org.codehaus.groovy.grails.orm.hibernate.HibernateGormValidationApi
import org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.hibernate.FlushMode;
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.TypeMismatchException
import org.hibernate.proxy.HibernateProxy
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.SimpleTypeConverter
import org.springframework.context.ApplicationContext
import org.springframework.dao.DataAccessException
import org.springframework.transaction.PlatformTransactionManager

class HibernateUtils {

    static final Logger LOG = LoggerFactory.getLogger(this)

    static final LAZY_PROPERTY_HANDLER = { String propertyName ->
        def propertyValue = PropertyUtils.getProperty(delegate, propertyName)
        if (propertyValue instanceof HibernateProxy) {
            propertyValue = GrailsHibernateUtil.unwrapProxy(propertyValue)
        }
        return propertyValue
    }

    /**
     * Overrides a getter on a property that is a Hibernate proxy in order to make sure the initialized object is returned hence avoiding Hibernate proxy hell.
     */
    static void handleLazyProxy(GrailsDomainClass domainClass, GrailsDomainClassProperty property) {
        String propertyName = property.name
        String getterName = GrailsClassUtils.getGetterName(propertyName)
        String setterName = GrailsClassUtils.getSetterName(propertyName)
        domainClass.metaClass."${getterName}" = LAZY_PROPERTY_HANDLER.clone().curry(propertyName)
        domainClass.metaClass."${setterName}" = { PropertyUtils.setProperty(delegate, propertyName, it) }

        for (GrailsDomainClass sub in domainClass.subClasses) {
            handleLazyProxy(sub, sub.getPropertyByName(property.name))
        }
    }

    static void enhanceSessionFactories(ApplicationContext ctx, GrailsApplication grailsApplication, source = null) {

        Map<SessionFactory, HibernateDatastore> datastores = [:]

        for (entry in ctx.getBeansOfType(SessionFactory)) {
            SessionFactory sessionFactory = entry.value
            String beanName = entry.key
            String suffix = beanName - 'sessionFactory'
            enhanceSessionFactory sessionFactory, grailsApplication, ctx, suffix, datastores, source
        }

        ctx.eventTriggeringInterceptor.datastores = datastores
    }

    static enhanceSessionFactory(SessionFactory sessionFactory, GrailsApplication application, ApplicationContext ctx) {
        enhanceSessionFactory(sessionFactory, application, ctx, '', [:])
    }

    static enhanceSessionFactory(SessionFactory sessionFactory, GrailsApplication application,
            ApplicationContext ctx, String suffix, Map<SessionFactory, HibernateDatastore> datastores, source = null) {

        MappingContext mappingContext = ctx.getBean("grailsDomainClassMappingContext", MappingContext)
        PlatformTransactionManager transactionManager = ctx.getBean("transactionManager$suffix", PlatformTransactionManager)
        final datastore = ctx.getBean("hibernateDatastore$suffix", Datastore)
        datastores[sessionFactory] = datastore
        String datasourceName = suffix ? suffix[1..-1] : GrailsDomainClassProperty.DEFAULT_DATA_SOURCE

        HibernateGormEnhancer enhancer = new HibernateGormEnhancer(datastore, transactionManager, application)

        def enhanceEntity = { PersistentEntity entity ->
            GrailsDomainClass dc = application.getDomainClass(entity.javaClass.name)
            if (!GrailsHibernateUtil.isMappedWithHibernate(dc) || !GrailsHibernateUtil.usesDatasource(dc, datasourceName)) {
                return
            }

            if (!datasourceName.equals(GrailsDomainClassProperty.DEFAULT_DATA_SOURCE)) {
                LOG.debug "Registering namespace methods for $dc.clazz.name in DataSource '$datasourceName'"
                registerNamespaceMethods dc, datastore, datasourceName, transactionManager, application
            }

            if (datasourceName.equals(GrailsDomainClassProperty.DEFAULT_DATA_SOURCE) || datasourceName.equals(GrailsHibernateUtil.getDefaultDataSource(dc))) {
                LOG.debug "Enhancing GORM entity ${entity.name}"
                if (entity.javaClass.getAnnotation(Enhanced) == null) {
                    enhancer.enhance entity
                }
                else {
                    enhancer.enhance entity, true
                }

                DomainClassGrailsPlugin.addRelationshipManagementMethods(application.getDomainClass(entity.javaClass.name), ctx)
            }
        }

        // If we are reloading via an onChange event, the source indicates the specific
        // entity that needs to be reloaded. Otherwise, just reload all of them.
        if (source) {
            PersistentEntity entity = mappingContext.getPersistentEntity(source.name)
            if (entity) {
                enhanceEntity(entity)
            }
        }
        else {
            for (PersistentEntity entity in mappingContext.getPersistentEntities()) {
                enhanceEntity(entity)
            }
        }
    }

    static Map filterQueryArgumentMap(Map query) {
        def queryArgs = [:]
        for (entry in query) {
            if (entry.value instanceof CharSequence) {
                queryArgs[entry.key] = entry.value.toString()
            }
            else {
                queryArgs[entry.key] = entry.value
            }
        }
        return queryArgs
    }

    private static List<String> removeNullNames(Map query) {
        List<String> nullNames = []
        Set<String> allNames = new HashSet(query.keySet())
        for (String name in allNames) {
            if (query[name] == null) {
                query.remove name
                nullNames << name
            }
        }
        nullNames
    }

    static void enhanceProxyClass(Class proxyClass) {
        def mc = proxyClass.metaClass
        if (mc.pickMethod('grailsEnhanced', GrailsHibernateUtil.EMPTY_CLASS_ARRAY)) {
            return
        }

        // hasProperty
        def originalHasProperty = mc.getMetaMethod("hasProperty", String)
        mc.hasProperty = { String name ->
            if (delegate instanceof HibernateProxy) {
                return GrailsHibernateUtil.unwrapProxy(delegate).hasProperty(name)
            }
            return originalHasProperty.invoke(delegate, name)
        }
        // respondsTo
        def originalRespondsTo = mc.getMetaMethod("respondsTo", String)
        mc.respondsTo = { String name ->
            if (delegate instanceof HibernateProxy) {
                return GrailsHibernateUtil.unwrapProxy(delegate).respondsTo(name)
            }
            return originalRespondsTo.invoke(delegate, name)
        }
        def originalRespondsToTwoArgs = mc.getMetaMethod("respondsTo", String, Object[])
        mc.respondsTo = { String name, Object[] args ->
            if (delegate instanceof HibernateProxy) {
                return GrailsHibernateUtil.unwrapProxy(delegate).respondsTo(name, args)
            }
            return originalRespondsToTwoArgs.invoke(delegate, name, args)
        }
        // getter
        mc.propertyMissing = { String name ->
            if (delegate instanceof HibernateProxy) {
                return GrailsHibernateUtil.unwrapProxy(delegate)."$name"
            }
            throw new MissingPropertyException(name, delegate.getClass())
        }

        // setter
        mc.propertyMissing = { String name, val ->
            if (delegate instanceof HibernateProxy) {
                GrailsHibernateUtil.unwrapProxy(delegate)."$name" = val
            }
            else {
                throw new MissingPropertyException(name, delegate.getClass())
            }
        }

        mc.methodMissing = { String name, args ->
            if (delegate instanceof HibernateProxy) {
                def obj = GrailsHibernateUtil.unwrapProxy(delegate)
                return obj."$name"(*args)
            }
            throw new MissingPropertyException(name, delegate.getClass())
        }

        mc.grailsEnhanced = { true }
    }

    static void enhanceProxy(HibernateProxy proxy) {
        proxy.metaClass = GroovySystem.metaClassRegistry.getMetaClass(proxy.getClass())
    }

    private static void registerNamespaceMethods(GrailsDomainClass dc, HibernateDatastore datastore,
            String datasourceName, PlatformTransactionManager  transactionManager,
            GrailsApplication application) {

        String getter = GrailsNameUtils.getGetterName(datasourceName)
        if (dc.metaClass.methods.any { it.name == getter && it.parameterTypes.size() == 0 }) {
            LOG.warn "The $dc.clazz.name domain class has a method '$getter' - unable to add namespaced methods for datasource '$datasourceName'"
            return
        }

        def classLoader = application.classLoader

        def finders = HibernateGormEnhancer.createPersistentMethods(application, classLoader, datastore)
        def staticApi = new HibernateGormStaticApi(dc.clazz, datastore, finders, classLoader, transactionManager)
        dc.metaClass.static."$getter" = { -> staticApi }

        def validateApi = new HibernateGormValidationApi(dc.clazz, datastore, classLoader)
        def instanceApi = new HibernateGormInstanceApi(dc.clazz, datastore, classLoader)
        dc.metaClass."$getter" = { -> new InstanceProxy(delegate, instanceApi, validateApi) }
    }

    /**
     * Session should no longer be flushed after a data access exception occurs (such a constriant violation)
     */
    static void handleDataAccessException(GrailsHibernateTemplate template, DataAccessException e) {
        try {
            template.execute new GrailsHibernateTemplate.HibernateCallback() {
                def doInHibernate(Session session) {
                    session.setFlushMode(FlushMode.MANUAL)
                }
            }
        }
        finally {
            throw e
        }
    }

    static shouldFlush(GrailsApplication application, Map map = [:]) {
        def shouldFlush

        if (map?.containsKey('flush')) {
            shouldFlush = Boolean.TRUE == map.flush
        } else {
            def config = application.flatConfig
            shouldFlush = Boolean.TRUE == config.get('grails.gorm.autoFlush')
        }
        return shouldFlush
    }

    /**
     * Converts an id value to the appropriate type for a domain class.
     *
     * @param grailsDomainClass a GrailsDomainClass
     * @param idValue an value to be converted
     * @return the idValue parameter converted to the type that grailsDomainClass expects
     * its identifiers to be
     */
    static convertValueToIdentifierType(grailsDomainClass, idValue) {
        convertToType(idValue, grailsDomainClass.identifier.type)
    }

    private static convertToType(value, targetType) {
        SimpleTypeConverter typeConverter = new SimpleTypeConverter()

        if (value != null && !targetType.isAssignableFrom(value.getClass())) {
            if (value instanceof Number && Long.equals(targetType)) {
                value = value.toLong()
            }
            else {
                try {
                    value = typeConverter.convertIfNecessary(value, targetType)
                } catch (TypeMismatchException e) {
                    // ignore
                }
            }
        }
        return value
    }
}
