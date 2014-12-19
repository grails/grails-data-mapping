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
import grails.core.GrailsApplication
import grails.core.GrailsDomainClass
import grails.core.GrailsDomainClassProperty
import grails.util.GrailsClassUtils
import grails.util.GrailsMetaClassUtils
import grails.util.GrailsNameUtils
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

import org.codehaus.groovy.grails.orm.hibernate.GrailsHibernateTemplate
import org.codehaus.groovy.grails.orm.hibernate.HibernateDatastore
import org.codehaus.groovy.grails.orm.hibernate.HibernateGormEnhancer
import org.codehaus.groovy.grails.orm.hibernate.HibernateGormInstanceApi
import org.codehaus.groovy.grails.orm.hibernate.HibernateGormStaticApi
import org.codehaus.groovy.grails.orm.hibernate.HibernateGormValidationApi
import org.codehaus.groovy.grails.orm.hibernate.support.ClosureEventTriggeringInterceptor
import org.codehaus.groovy.grails.orm.hibernate.support.HibernateRuntimeUtils
import org.codehaus.groovy.runtime.InvokerHelper
import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher
import org.hibernate.FlushMode
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.proxy.HibernateProxy
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.PropertyAccessorFactory
import org.springframework.context.ApplicationContext
import org.springframework.core.convert.ConversionService
import org.springframework.dao.DataAccessException
import org.springframework.transaction.PlatformTransactionManager

@CompileStatic
class HibernateUtils {

    static final Logger LOG = LoggerFactory.getLogger(HibernateUtils)

    /**
     * Overrides a getter on a property that is a Hibernate proxy in order to make sure the initialized object is returned hence avoiding Hibernate proxy hell.
     */
    static void handleLazyProxy(GrailsDomainClass domainClass, GrailsDomainClassProperty property) {
        String propertyName = property.name
        String getterName = GrailsClassUtils.getGetterName(propertyName)
        String setterName = GrailsClassUtils.getSetterName(propertyName)

        GroovyObject mc = (GroovyObject)domainClass.metaClass

        def propertyFetcher = ClassPropertyFetcher.forClass(domainClass.getClazz())

        mc.setProperty(getterName, {->
            def propertyValue = propertyFetcher.getPropertyValue(getDelegate(), propertyName)
            if (propertyValue instanceof HibernateProxy) {
                propertyValue = GrailsHibernateUtil.unwrapProxy(propertyValue)
            }
            return propertyValue
        })
        mc.setProperty(setterName, {
            PropertyAccessorFactory.forBeanPropertyAccess(getDelegate()).setPropertyValue(propertyName, it)
        })


        for (GrailsDomainClass sub in domainClass.subClasses) {
            handleLazyProxy(sub, sub.getPropertyByName(property.name))
        }
    }

    static void enhanceSessionFactories(ApplicationContext ctx, GrailsApplication grailsApplication, Object source = null) {

        Map<SessionFactory, HibernateDatastore> datastores = [:]

        for (entry in ctx.getBeansOfType(SessionFactory).entrySet()) {
            SessionFactory sessionFactory = entry.value
            String beanName = entry.key
            String suffix = beanName - 'sessionFactory'
            enhanceSessionFactory sessionFactory, grailsApplication, ctx, suffix, datastores, source
        }

        ctx.getBean("eventTriggeringInterceptor", ClosureEventTriggeringInterceptor).datastores = datastores
    }

    static enhanceSessionFactory(SessionFactory sessionFactory, GrailsApplication application, ApplicationContext ctx) {
        enhanceSessionFactory(sessionFactory, application, ctx, '', [:])
    }

    static enhanceSessionFactory(SessionFactory sessionFactory, GrailsApplication application,
            ApplicationContext ctx, String suffix, Map<SessionFactory, HibernateDatastore> datastores, Object source = null) {

        MappingContext mappingContext = ctx.getBean("grailsDomainClassMappingContext", MappingContext)
        PlatformTransactionManager transactionManager = ctx.getBean("transactionManager$suffix", PlatformTransactionManager)
        HibernateDatastore datastore = (HibernateDatastore)ctx.getBean("hibernateDatastore$suffix", Datastore)
        datastores[sessionFactory] = datastore
        String datasourceName = suffix ? suffix[1..-1] : GrailsDomainClassProperty.DEFAULT_DATA_SOURCE

        HibernateGormEnhancer enhancer = new HibernateGormEnhancer(datastore, transactionManager, application)

        def enhanceEntity = { PersistentEntity entity ->
            GrailsDomainClass dc = (GrailsDomainClass)application.getArtefact(DomainClassArtefactHandler.TYPE, entity.javaClass.name)
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

                HibernateGormEnhancer.addRelationshipManagementMethods(dc, ctx)
            }
        }

        // If we are reloading via an onChange event, the source indicates the specific
        // entity that needs to be reloaded. Otherwise, just reload all of them.
        if (source) {
            PersistentEntity entity = getPersistentEntity(mappingContext, InvokerHelper.getPropertySafe(source, 'name')?.toString())
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

    // workaround CS bug
    @CompileStatic(TypeCheckingMode.SKIP)
    private static PersistentEntity getPersistentEntity(mappingContext, String name) {
        mappingContext.getPersistentEntity(name)
    }

    static Map filterQueryArgumentMap(Map query) {
        def queryArgs = [:]
        for (entry in query.entrySet()) {
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

    // http://jira.codehaus.org/browse/GROOVY-6138 prevents using CompileStatic for this method
    @CompileStatic(TypeCheckingMode.SKIP)
    static void enhanceProxyClass(Class proxyClass) {

        MetaClass mc = GrailsMetaClassUtils.getExpandoMetaClass(proxyClass)
        MetaMethod grailsEnhancedMetaMethod = mc.getStaticMetaMethod("grailsEnhanced", (Class[])null)
        if (grailsEnhancedMetaMethod != null && grailsEnhancedMetaMethod.invoke(proxyClass, null) == proxyClass) {
            return
        }

        MetaClass superMc = GrailsMetaClassUtils.getExpandoMetaClass(proxyClass.getSuperclass())

        // hasProperty
        registerMetaMethod(mc, 'hasProperty', { String name ->
            Object obj = getDelegate()
            boolean result = superMc.hasProperty(obj, name)
            if (!result) {
                Object unwrapped = GrailsHibernateUtil.unwrapProxy((HibernateProxy)obj)
                result = unwrapped.getMetaClass().hasProperty(obj, name)
            }
            return result
        })
        // respondsTo
        registerMetaMethod(mc, 'respondsTo', { String name ->
            Object obj = getDelegate()
            def result = superMc.respondsTo(obj, name)
            if (!result) {
                Object unwrapped = GrailsHibernateUtil.unwrapProxy((HibernateProxy)obj)
                result = unwrapped.getMetaClass().respondsTo(obj, name)
            }
            result
        })
        registerMetaMethod(mc, 'respondsTo', { String name, Object[] args ->
            Object obj = getDelegate()
            def result = superMc.respondsTo(obj, name, args)
            if (!result) {
                Object unwrapped = GrailsHibernateUtil.unwrapProxy((HibernateProxy)obj)
                result = unwrapped.getMetaClass().respondsTo(obj, name, args)
            }
            result
        })

        // setter
        registerMetaMethod(mc, 'propertyMissing', { String name, Object val ->
            Object obj = getDelegate()
            try {
                superMc.setProperty(proxyClass, obj, name, val, true, true)
            } catch (MissingPropertyException e) {
                Object unwrapped = GrailsHibernateUtil.unwrapProxy((HibernateProxy)obj)
                unwrapped.getMetaClass().setProperty(unwrapped, name, val)
            }
        })

        // getter
        registerMetaMethod(mc, 'propertyMissing', { String name ->
            Object obj = getDelegate()
            try {
                return superMc.getProperty(proxyClass, obj, name, true, true)
            } catch (MissingPropertyException e) {
                Object unwrapped = GrailsHibernateUtil.unwrapProxy((HibernateProxy)obj)
                unwrapped.getMetaClass().getProperty(unwrapped, name)
            }
        })

        registerMetaMethod(mc, 'methodMissing', { String name, Object args ->
            Object obj = getDelegate()
            Object[] argsArray = (Object[])args
            try {
                superMc.invokeMethod(proxyClass, obj, name, argsArray, true, true)
            } catch (MissingMethodException e) {
                Object unwrapped = GrailsHibernateUtil.unwrapProxy((HibernateProxy)obj)
                unwrapped.getMetaClass().invokeMethod(unwrapped, name, argsArray)
            }
        })

        mc.static.grailsEnhanced = {->proxyClass}
    }


    @CompileStatic(TypeCheckingMode.SKIP)
    private static final registerMetaMethod(MetaClass mc, String name, Closure c) {
        mc."$name" = c
    }

    static void enhanceProxy(HibernateProxy proxy) {
        // no need to do anything here
    }

    private static void registerNamespaceMethods(GrailsDomainClass dc, HibernateDatastore datastore,
            String datasourceName, PlatformTransactionManager  transactionManager,
            GrailsApplication application) {

        String getter = GrailsNameUtils.getGetterName(datasourceName)
        if (dc.metaClass.methods.any { MetaMethod it -> it.name == getter && it.parameterTypes.size() == 0 }) {
            LOG.warn "The $dc.clazz.name domain class has a method '$getter' - unable to add namespaced methods for datasource '$datasourceName'"
            return
        }

        def classLoader = application.classLoader

        def finders = new HibernateGormEnhancer(datastore, transactionManager, application).getFinders()
        def staticApi = new HibernateGormStaticApi(dc.clazz, datastore, finders, classLoader, transactionManager)
        ((GroovyObject)((GroovyObject)dc.metaClass).getProperty('static')).setProperty(getter, { -> staticApi })

        def validateApi = new HibernateGormValidationApi(dc.clazz, datastore, classLoader)
        def instanceApi = new HibernateGormInstanceApi(dc.clazz, datastore, classLoader)
        ((GroovyObject)dc.metaClass).setProperty(getter, { -> new InstanceProxy(getDelegate(), instanceApi, validateApi) })
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
    static Object convertValueToIdentifierType(GrailsDomainClass grailsDomainClass, Object idValue, ConversionService conversionService) {
        convertValueToType(idValue, grailsDomainClass.identifier.type, conversionService)
    }

    static Object convertValueToType(Object passedValue, Class targetType, ConversionService conversionService) {
        HibernateRuntimeUtils.convertValueToType(passedValue, targetType, conversionService)
    }
}
