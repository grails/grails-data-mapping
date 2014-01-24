/* Copyright (C) 2011 SpringSource
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
package org.codehaus.groovy.grails.orm.hibernate

import grails.util.GrailsNameUtils
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainConfigurationUtil
import org.codehaus.groovy.grails.orm.hibernate.cfg.HibernateNamedQueriesBuilder
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.GormValidationApi
import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.model.PersistentEntity
import org.springframework.beans.BeanUtils
import org.springframework.context.ApplicationContext
import org.springframework.transaction.PlatformTransactionManager
import org.grails.datastore.mapping.proxy.ProxyFactory
import org.grails.datastore.mapping.model.types.ToOne

@CompileStatic
abstract class AbstractHibernateGormEnhancer extends GormEnhancer {

    protected ClassLoader classLoader
    protected GrailsApplication grailsApplication

    protected AbstractHibernateGormEnhancer(AbstractHibernateDatastore datastore,
            PlatformTransactionManager transactionManager,
            GrailsApplication grailsApplication) {
        super(datastore, transactionManager)
        this.grailsApplication = grailsApplication
        classLoader = grailsApplication.classLoader
        getFinders()
    }

    @Override
    protected void registerConstraints(Datastore datastore) {
        // no-op
    }

    @Override
    protected void registerNamedQueries(PersistentEntity entity, Closure namedQueries) {
        if (grailsApplication == null) {
            return
        }
        def domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, entity.name)
        if (domainClass) {
            new HibernateNamedQueriesBuilder(domainClass, entity, getFinders()).evaluate(namedQueries)
        }
    }

    protected void registerAssociationIdentifierGetter(ProxyFactory proxyFactory, MetaClass metaClass, ToOne association) {
        // no-op
    }

    /**
     * Legacy implementation used by Hibernate only for backwards compatibility
     *
     * @param dc The domain class
     * @param ctx The application context
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    static void addRelationshipManagementMethods(GrailsDomainClass dc, ApplicationContext ctx) {
        def metaClass = dc.metaClass
        for (p in dc.persistentProperties) {
            def prop = p
            if (prop.basicCollectionType) {
                def collectionName = GrailsNameUtils.getClassNameRepresentation(prop.name)
                metaClass."addTo$collectionName" = { obj ->
                    if (obj instanceof CharSequence && !(obj instanceof String)) {
                        obj = obj.toString()
                    }
                    if (prop.referencedPropertyType.isInstance(obj)) {
                        if (delegate[prop.name] == null) {
                            delegate[prop.name] = GrailsClassUtils.createConcreteCollection(prop.type)
                        }
                        delegate[prop.name] << obj
                        return delegate
                    }
                    else {
                        throw new MissingMethodException("addTo${collectionName}", dc.clazz, [obj] as Object[])
                    }
                }
                metaClass."removeFrom$collectionName" = { obj ->
                    if (delegate[prop.name]) {
                        if (obj instanceof CharSequence && !(obj instanceof String)) {
                            obj = obj.toString()
                        }
                        delegate[prop.name].remove(obj)
                    }
                    return delegate
                }
            }
            else if (prop.oneToOne || prop.manyToOne) {
                def identifierPropertyName = "${prop.name}Id"
                if (!dc.hasMetaProperty(identifierPropertyName)) {
                    def getterName = GrailsClassUtils.getGetterName(identifierPropertyName)
                    metaClass."$getterName" = {-> GrailsDomainConfigurationUtil.getAssociationIdentifier(
                            delegate, prop.name, prop.referencedDomainClass) }
                }
            }
            else if (prop.oneToMany || prop.manyToMany) {
                if (metaClass instanceof ExpandoMetaClass) {
                    def propertyName = prop.name
                    def collectionName = GrailsNameUtils.getClassNameRepresentation(propertyName)
                    def otherDomainClass = prop.referencedDomainClass

                    metaClass."addTo${collectionName}" = { Object arg ->
                        Object obj
                        if (delegate[prop.name] == null) {
                            delegate[prop.name] = GrailsClassUtils.createConcreteCollection(prop.type)
                        }
                        if (arg instanceof Map) {
                            obj = getDomainInstance(otherDomainClass, ctx)
                            obj.properties = arg
                            delegate[prop.name].add(obj)
                        }
                        else if (otherDomainClass.clazz.isInstance(arg)) {
                            obj = arg
                            delegate[prop.name].add(obj)
                        }
                        else {
                            throw new MissingMethodException("addTo${collectionName}", dc.clazz, [arg] as Object[])
                        }
                        if (prop.bidirectional && prop.otherSide) {
                            def otherSide = prop.otherSide
                            if (otherSide.oneToMany || otherSide.manyToMany) {
                                String name = prop.otherSide.name
                                if (!obj[name]) {
                                    obj[name] = GrailsClassUtils.createConcreteCollection(prop.otherSide.type)
                                }
                                obj[prop.otherSide.name].add(delegate)
                            }
                            else {
                                obj[prop.otherSide.name] = delegate
                            }
                        }
                        delegate
                    }
                    metaClass."removeFrom${collectionName}" = {Object arg ->
                        if (otherDomainClass.clazz.isInstance(arg)) {
                            delegate[prop.name]?.remove(arg)
                            if (prop.bidirectional) {
                                if (prop.manyToMany) {
                                    String name = prop.otherSide.name
                                    arg[name]?.remove(delegate)
                                }
                                else {
                                    arg[prop.otherSide.name] = null
                                }
                            }
                        }
                        else {
                            throw new MissingMethodException("removeFrom${collectionName}", dc.clazz, [arg] as Object[])
                        }
                        delegate
                    }
                }
            }
        }
    }

    private static Object getDomainInstance(GrailsDomainClass domainClass, ApplicationContext ctx) {
        def obj
        if (ctx.containsBean(domainClass.fullName)) {
            obj = ctx.getBean(domainClass.fullName)
        }
        else {
            obj = BeanUtils.instantiateClass(domainClass.clazz)
        }
        obj
    }
}
