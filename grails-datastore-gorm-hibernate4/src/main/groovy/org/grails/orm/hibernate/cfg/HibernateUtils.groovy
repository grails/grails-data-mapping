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
package org.grails.orm.hibernate.cfg

import grails.artefact.Enhanced
import grails.core.GrailsApplication
import grails.core.GrailsDomainClass
import grails.util.GrailsMetaClassUtils
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.codehaus.groovy.runtime.InvokerHelper
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher
import org.grails.datastore.mapping.reflect.NameUtils
import org.grails.orm.hibernate.*
import org.grails.orm.hibernate.support.ClosureEventTriggeringInterceptor
import org.grails.orm.hibernate.support.HibernateRuntimeUtils
import org.hibernate.SessionFactory
import org.hibernate.proxy.HibernateProxy
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.PropertyAccessorFactory
import org.springframework.context.ApplicationContext
import org.springframework.core.convert.ConversionService
import org.springframework.transaction.PlatformTransactionManager

@CompileStatic
class HibernateUtils {

    static final Logger LOG = LoggerFactory.getLogger(HibernateUtils)

    /**
     * Overrides a getter on a property that is a Hibernate proxy in order to make sure the initialized object is returned hence avoiding Hibernate proxy hell.
     */
    static void handleLazyProxy(PersistentEntity entity, PersistentProperty property) {
        String propertyName = property.name
        String getterName = NameUtils.getGetterName(propertyName)
        String setterName = NameUtils.getSetterName(propertyName)

        GroovyObject mc = (GroovyObject)entity.javaClass.metaClass
        def propertyFetcher = ClassPropertyFetcher.forClass(entity.getJavaClass())

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

        def children = entity.getMappingContext().getDirectChildEntities(entity)
        for (PersistentEntity sub in children) {
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
        String datasourceName = suffix ? suffix[1..-1] : Mapping.DEFAULT_DATA_SOURCE

        HibernateGormEnhancer enhancer = new HibernateGormEnhancer(datastore, transactionManager)

        def enhanceEntity = { PersistentEntity entity ->
            if (!GrailsHibernateUtil.isMappedWithHibernate(entity) || !GrailsHibernateUtil.usesDatasource(entity, datasourceName)) {
                return
            }

            if (!datasourceName.equals(Mapping.DEFAULT_DATA_SOURCE)) {
                LOG.debug "Registering namespace methods for $entity.javaClass.name in DataSource '$datasourceName'"
                registerNamespaceMethods entity, datastore, datasourceName, transactionManager, application
            }

            if (datasourceName.equals(Mapping.DEFAULT_DATA_SOURCE) || datasourceName.equals(GrailsHibernateUtil.getDefaultDataSource(entity))) {
                LOG.debug "Enhancing GORM entity ${entity.name}"
                if (entity.javaClass.getAnnotation(Enhanced) == null) {
                    enhancer.enhance entity
                }
                else {
                    enhancer.enhance entity, false
                }
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

    private static void registerNamespaceMethods(PersistentEntity dc, HibernateDatastore datastore,
                                                 String datasourceName, PlatformTransactionManager  transactionManager,
                                                 GrailsApplication application) {

        String getter = NameUtils.getGetterName(datasourceName)
        if (dc.metaClass.methods.any { MetaMethod it -> it.name == getter && it.parameterTypes.size() == 0 }) {
            LOG.warn "The $dc.javaClass.name domain class has a method '$getter' - unable to add namespaced methods for datasource '$datasourceName'"
            return
        }

        def classLoader = application.classLoader

        def finders = new HibernateGormEnhancer(datastore, transactionManager).getFinders()
        def staticApi = new HibernateGormStaticApi(dc.javaClass, datastore, finders, classLoader, transactionManager)
        ((GroovyObject)((GroovyObject)dc.metaClass).getProperty('static')).setProperty(getter, { -> staticApi })

        def validateApi = new HibernateGormValidationApi(dc.javaClass, datastore, classLoader)
        def instanceApi = new HibernateGormInstanceApi(dc.javaClass, datastore, classLoader)
        ((GroovyObject)dc.metaClass).setProperty(getter, { -> new InstanceProxy(getDelegate(), instanceApi, validateApi) })
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
